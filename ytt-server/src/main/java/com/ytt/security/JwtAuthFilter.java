package com.ytt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytt.common.ErrorCode;
import com.ytt.common.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器：解析 Bearer Token -> 校验黑名单 -> 加载用户 -> 写入 SecurityContext
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklist tokenBlacklist;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    public JwtAuthFilter(JwtUtil jwtUtil,
                         UserDetailsServiceImpl userDetailsService,
                         TokenBlacklist tokenBlacklist) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader(header);

        // 无 Token 直接放行（由 Security 配置决定哪些路径需要认证）
        if (authHeader == null || !authHeader.startsWith(prefix)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(prefix.length()).trim();
        try {
            // 黑名单校验（注销/改密后的旧 Token）
            if (tokenBlacklist.contains(token)) {
                writeError(response, ErrorCode.TOKEN_BLACKLISTED);
                return;
            }
            Claims claims = jwtUtil.parse(token);
            // 只允许 access token 用于接口访问
            if (!JwtUtil.TYPE_ACCESS.equals(jwtUtil.getType(claims))) {
                writeError(response, ErrorCode.TOKEN_INVALID);
                return;
            }
            Long userId = jwtUtil.getUserId(claims);
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            if (!userDetails.isEnabled()) {
                writeError(response, ErrorCode.ACCOUNT_DISABLED);
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            writeError(response, ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.debug("Token 校验失败: {}", e.getMessage());
            writeError(response, ErrorCode.TOKEN_INVALID);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
    }
}
