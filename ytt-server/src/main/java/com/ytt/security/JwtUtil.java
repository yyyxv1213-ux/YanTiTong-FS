package com.ytt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类：生成、解析、校验令牌
 * access token 2 小时，refresh token 7 天
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expire}")
    private Long accessExpire;

    @Value("${jwt.refresh-expire}")
    private Long refreshExpire;

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    /** 生成访问令牌 */
    public String generateAccessToken(Long userId, String username) {
        return buildToken(userId, username, TYPE_ACCESS, accessExpire * 1000);
    }

    /** 生成刷新令牌 */
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, TYPE_REFRESH, refreshExpire * 1000);
    }

    private String buildToken(Long userId, String username, String type, long ttlMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, type);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                // DEF-02 修复：增加 jti 随机声明，确保同秒签发的 Token 字节唯一
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMillis))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 解析令牌（签名或过期问题会抛异常） */
    public Claims parse(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /** 校验令牌是否有效（未过期且签名正确） */
    public boolean validate(String token) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(Claims claims) {
        Object uid = claims.get(CLAIM_USER_ID);
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public String getType(Claims claims) {
        Object typ = claims.get(CLAIM_TYPE);
        return typ == null ? null : typ.toString();
    }

    /** 剩余有效期（秒），用于黑名单 TTL */
    public long getRemainSeconds(Claims claims) {
        long remain = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remain / 1000, 0);
    }

    public Long getAccessExpire() {
        return accessExpire;
    }
}
