package com.ytt.modules.auth;

import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码服务（单机内存实现）。
 * - 6 位数字随机验证码，有效期 5 分钟
 * - 同一「场景 + 邮箱」60 秒内仅允许发送一次
 * - key 按「场景:邮箱」隔离，不同业务互不污染
 * - 校验通过不立即删除，待业务完整成功后由 {@link #consume} 消费，
 *   验证码错误或过期才不可再用（防止后续步骤失败导致验证码误失效）
 *
 * 集群部署时替换为 Redis 实现（key/TTL 语义保持一致），业务代码零改动。
 */
@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);

    /** 登录场景 */
    public static final String PURPOSE_LOGIN = "login";

    /** 验证码有效期（秒）：5 分钟 */
    public static final long EXPIRE_SECONDS = 300;

    /** 两次发送最小间隔（毫秒）：60 秒 */
    public static final long RESEND_INTERVAL_MS = 60_000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** key = purpose:email -> 验证码条目 */
    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成验证码并存储（同 key 旧码被覆盖），默认 5 分钟有效期。
     *
     * @return 6 位数字验证码明文（供调用方发送邮件）
     */
    public String createCode(String purpose, String email) {
        return createCode(purpose, email, EXPIRE_SECONDS);
    }

    /**
     * 生成验证码并存储，可指定有效期（主要用于测试过期场景）。
     * 60 秒内重复发送将被拒绝。
     */
    public String createCode(String purpose, String email, long ttlSeconds) {
        String key = buildKey(purpose, email);
        long now = System.currentTimeMillis();
        CodeEntry existing = store.get(key);
        if (existing != null && now - existing.createTime < RESEND_INTERVAL_MS) {
            long wait = (RESEND_INTERVAL_MS - (now - existing.createTime) + 999) / 1000;
            log.info("邮箱验证码发送过频：key={}，需等待 {} 秒", key, wait);
            throw new BizException(ErrorCode.EMAIL_CODE_TOO_FREQUENT);
        }
        String code = generateCode();
        store.put(key, new CodeEntry(code, now, now + ttlSeconds * 1000L));
        log.info("邮箱验证码已生成：key={}，有效期 {} 秒", key, ttlSeconds);
        return code;
    }

    /**
     * 校验验证码：过期抛 {@link ErrorCode#EMAIL_CODE_EXPIRED}，
     * 不匹配抛 {@link ErrorCode#EMAIL_CODE_ERROR}。校验通过不删除条目。
     */
    public void verify(String purpose, String email, String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new BizException(ErrorCode.EMAIL_CODE_ERROR, "请输入邮箱验证码");
        }
        String key = buildKey(purpose, email);
        CodeEntry entry = store.get(key);
        long now = System.currentTimeMillis();
        if (entry == null || now > entry.expireAt) {
            log.info("邮箱验证码不存在或已过期：key={}", key);
            throw new BizException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!entry.code.equals(input.trim())) {
            log.info("邮箱验证码错误：key={}", key);
            throw new BizException(ErrorCode.EMAIL_CODE_ERROR);
        }
    }

    /** 业务完整成功后消费（删除）验证码，保证一次性使用 */
    public void consume(String purpose, String email) {
        store.remove(buildKey(purpose, email));
        log.info("邮箱验证码已消费：key={}", buildKey(purpose, email));
    }

    /** 清空全部验证码（仅供测试在各用例间重置，生产代码不应调用） */
    public void clearStore() {
        store.clear();
    }

    /** 生成 6 位数字验证码（前导零保留） */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String buildKey(String purpose, String email) {
        return purpose + ":" + email.trim().toLowerCase();
    }

    /** 验证码条目 */
    private static class CodeEntry {
        final String code;
        final long createTime;
        final long expireAt;

        CodeEntry(String code, long createTime, long expireAt) {
            this.code = code;
            this.createTime = createTime;
            this.expireAt = expireAt;
        }
    }
}
