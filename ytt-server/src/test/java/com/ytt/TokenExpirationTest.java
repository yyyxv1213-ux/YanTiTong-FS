package com.ytt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ytt.security.JwtUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Token 过期处理测试：
 * 过期令牌拒绝（40102）、黑名单令牌（40103）、令牌类型错误/格式错误（40101）、
 * 无令牌（40100），以及 JwtUtil 层面的过期判定。
 */
class TokenExpirationTest extends BaseIntegrationTest {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private com.ytt.modules.auth.AuthService authService;

    /**
     * 手工构造一枚未过期的 refresh token。
     * iat 相对当前时间偏移 -2 秒，避免 jjwt 0.9.1 秒级时间戳下，
     * 与同秒内其它登录签发的 refresh token 字符串完全相同而误中黑名单。
     */
    private String buildRefreshTokenOffset() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtUtil.CLAIM_USER_ID, 2L);
        claims.put(JwtUtil.CLAIM_TYPE, JwtUtil.TYPE_REFRESH);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("study")
                .setIssuedAt(new Date(now - 2_000L))
                .setExpiration(new Date(now + 604_800_000L))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 手工构造一枚已过期的 access token（exp 为过去时间） */
    private String buildExpiredAccessToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtUtil.CLAIM_USER_ID, 2L);
        claims.put(JwtUtil.CLAIM_TYPE, JwtUtil.TYPE_ACCESS);
        long past = System.currentTimeMillis() - 60_000L;
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("study")
                .setIssuedAt(new Date(past - 60_000L))
                .setExpiration(new Date(past))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 过期 access token：过滤器返回 40102 */
    @Test
    void expiredAccessTokenRejected() throws Exception {
        perform(get("/practice/questions"), null, buildExpiredAccessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40102));
    }

    /** JwtUtil：合法令牌校验通过，过期令牌校验失败 */
    @Test
    void jwtUtilDetectsExpiration() {
        String validToken = jwtUtil.generateAccessToken(2L, "study");
        assertTrue(jwtUtil.validate(validToken));
        assertFalse(jwtUtil.validate(buildExpiredAccessToken()));
    }

    /** 注销后的 token 进入黑名单：返回 40103 */
    @Test
    void blacklistedAccessTokenRejected() throws Exception {
        String token = jwtUtil.generateAccessToken(2L, "study");
        authService.logout(token);
        perform(get("/practice/questions"), null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40103));
    }

    /** refresh token 不能当作 access token 使用：返回 40101 */
    @Test
    void refreshTokenRejectedForApi() throws Exception {
        String refreshToken = buildRefreshTokenOffset();
        perform(get("/practice/questions"), null, refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));
    }

    /** 格式非法的 token：返回 40101 */
    @Test
    void malformedTokenRejected() throws Exception {
        perform(get("/practice/questions"), null, "garbage.token.value")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101));
    }

    /** 不携带令牌访问受保护资源：返回 40100 */
    @Test
    void missingTokenRejected() throws Exception {
        perform(get("/practice/questions"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }
}
