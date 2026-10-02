package com.ytt.modules.auth;

import com.ytt.common.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 邮箱验证码服务单元测试：生成、校验、过期、防刷、消费与场景隔离。
 * 纯内存调用，不启动 Spring 上下文。
 */
class EmailCodeServiceTest {

    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        service = new EmailCodeService();
    }

    /** 生成的验证码为 6 位数字，且可正常校验通过 */
    @Test
    void generateAndVerify() {
        String code = service.createCode(EmailCodeService.PURPOSE_LOGIN, "tester@example.com");
        assertTrue(code.matches("\\d{6}"), "验证码应为 6 位数字");
        assertDoesNotThrow(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "tester@example.com", code));
    }

    /** 空输入 / 错误验证码：抛 40106 */
    @Test
    void wrongOrBlankCode() {
        service.createCode(EmailCodeService.PURPOSE_LOGIN, "tester@example.com");

        BizException blank = catchBiz(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "tester@example.com", ""));
        assertEquals(40106, blank.getErrorCode().getCode());

        BizException wrong = catchBiz(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "tester@example.com", "000000"));
        assertEquals(40106, wrong.getErrorCode().getCode());
    }

    /** 从未生成过验证码：按过期处理，抛 40107 */
    @Test
    void codeNotExist() {
        BizException e = catchBiz(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "nobody@example.com", "123456"));
        assertEquals(40107, e.getErrorCode().getCode());
    }

    /** 已过期验证码：抛 40107（TTL 传 -1 秒立即过期） */
    @Test
    void expiredCode() {
        String code = service.createCode(EmailCodeService.PURPOSE_LOGIN, "old@example.com", -1);
        BizException e = catchBiz(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "old@example.com", code));
        assertEquals(40107, e.getErrorCode().getCode());
    }

    /** 60 秒内重复发送：抛 42901 */
    @Test
    void resendTooFrequent() {
        service.createCode(EmailCodeService.PURPOSE_LOGIN, "freq@example.com");
        BizException e = catchBiz(() ->
                service.createCode(EmailCodeService.PURPOSE_LOGIN, "freq@example.com"));
        assertEquals(42901, e.getErrorCode().getCode());
    }

    /** 消费后验证码立即失效，再校验抛 40107 */
    @Test
    void consumeThenInvalid() {
        String code = service.createCode(EmailCodeService.PURPOSE_LOGIN, "once@example.com");
        service.consume(EmailCodeService.PURPOSE_LOGIN, "once@example.com");
        BizException e = catchBiz(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "once@example.com", code));
        assertEquals(40107, e.getErrorCode().getCode());
    }

    /** 场景隔离：login 与 register 同邮箱互不污染、不触发防刷 */
    @Test
    void purposeIsolation() {
        String loginCode = service.createCode(EmailCodeService.PURPOSE_LOGIN, "same@example.com");
        String registerCode = service.createCode("register", "same@example.com");

        assertDoesNotThrow(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "same@example.com", loginCode));
        assertDoesNotThrow(() ->
                service.verify("register", "same@example.com", registerCode));

        // 交叉校验必须失败
        BizException e = catchBiz(() ->
                service.verify("register", "same@example.com", loginCode));
        assertEquals(40106, e.getErrorCode().getCode());
    }

    /** 大小写邮箱归一化：不同大小写视为同一邮箱 */
    @Test
    void emailCaseInsensitive() {
        String code = service.createCode(EmailCodeService.PURPOSE_LOGIN, "User@Example.com");
        assertDoesNotThrow(() ->
                service.verify(EmailCodeService.PURPOSE_LOGIN, "user@example.com", code));
    }

    private BizException catchBiz(Runnable action) {
        try {
            action.run();
            throw new AssertionError("期望抛出 BizException");
        } catch (BizException e) {
            return e;
        }
    }
}
