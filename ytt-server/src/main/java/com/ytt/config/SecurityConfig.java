package com.ytt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytt.common.ErrorCode;
import com.ytt.common.Result;
import com.ytt.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置：无状态 JWT 认证 + RBAC 授权
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /** BCrypt 密码加密器 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 无状态：关闭 Session 与 CSRF（Token 认证不需要 CSRF 防护）
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 放行 CORS 预检请求
            .cors()
            .and()
            .authorizeRequests()
            // 公开接口：登录、注册、刷新令牌、接口文档
            .antMatchers(
                    "/auth/login", "/auth/register", "/auth/refresh",
                    "/auth/email/code", "/auth/email/login",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                    "/doc.html", "/webjars/**", "/favicon.ico", "/error",
                    "/common/banks"
            ).permitAll()
            // 管理员接口：仅 ADMIN 角色
            .antMatchers("/admin/**").hasRole("ADMIN")
            // 其余接口需要登录
            .anyRequest().authenticated()
            .and()
            // 认证/授权异常统一返回 Result JSON
            .exceptionHandling()
            .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, ErrorCode.UNAUTHORIZED))
            .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeError(response, ErrorCode.FORBIDDEN))
            .and()
            // JWT 过滤器置于用户名密码过滤器之前
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode)));
    }
}
