package com.ytt.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单（内存实现）：注销/改密后的旧 Token 立即失效。
 * 记录过期时间并惰性清理。生产环境集群部署时可替换为 Redis（key: token, value: "", TTL=剩余有效期）。
 */
@Component
public class TokenBlacklist {

    /** token -> 应失效截止时间戳（即 token 原过期时间） */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /** 加入黑名单 */
    public void add(String token, long expireAtMillis) {
        blacklist.put(token, expireAtMillis);
    }

    /** 是否在黑名单中（顺带惰性清理过期项） */
    public boolean contains(String token) {
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
