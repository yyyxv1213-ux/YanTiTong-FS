package com.ytt;

import com.ytt.modules.auth.EmailCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 邮箱验证码登录集成测试。
 * MailService（真实 SMTP 发信）以 MockBean 替换，保证测试不对外发邮件；
 * 验证码由 {@link EmailCodeService} 直接生成或经发送接口生成（5 分钟有效语义一致）。
 */
@Sql(scripts = "/test-h2-fix.sql")
class EmailLoginTest extends BaseIntegrationTest {

    /** 替换为 mock，避免测试期间真实发送邮件 */
    @MockBean
    private com.ytt.modules.auth.MailService mailService;

    /** 每方法清空验证码内存：60 秒发送窗口与已生成条目不跨用例残留 */
    @BeforeEach
    void resetCodeStore() {
        emailCodeService.clearStore();
    }

    /** 发送验证码：已注册邮箱成功；未注册邮箱返回 40401；60 秒内重发返回 42901 */
    @Test
    void sendCodeFlow() throws Exception {
        // 未注册邮箱
        perform(post("/auth/email/code"),
                body("email", "unregistered@example.com"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401));

        // 已注册邮箱，发送成功
        perform(post("/auth/email/code"),
                body("email", "study@yantitong.local"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 60 秒内重复发送
        perform(post("/auth/email/code"),
                body("email", "study@yantitong.local"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(42901));
    }

    /** 发送验证码参数校验：非法邮箱返回 40000 */
    @Test
    void sendCodeInvalidEmail() throws Exception {
        perform(post("/auth/email/code"), body("email", "not-an-email"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 验证码登录成功：签发双 Token；验证码一次性，再次使用返回 40107 */
    @Test
    void emailLoginSuccessAndOneTimeUse() throws Exception {
        String code = emailCodeService.createCode(
                EmailCodeService.PURPOSE_LOGIN, "study@yantitong.local");

        perform(post("/auth/email/login"),
                body("email", "study@yantitong.local", "code", code), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("study"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("STUDENT"));

        // 同一验证码再次使用：已被消费
        perform(post("/auth/email/login"),
                body("email", "study@yantitong.local", "code", code), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40107));
    }

    /** 验证码错误：返回 40106 */
    @Test
    void emailLoginWrongCode() throws Exception {
        emailCodeService.createCode(EmailCodeService.PURPOSE_LOGIN, "study@yantitong.local");
        perform(post("/auth/email/login"),
                body("email", "study@yantitong.local", "code", "000000"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40106));
    }

    /** 验证码已过期：返回 40107 */
    @Test
    void emailLoginExpiredCode() throws Exception {
        String code = emailCodeService.createCode(
                EmailCodeService.PURPOSE_LOGIN, "study@yantitong.local", -1);
        perform(post("/auth/email/login"),
                body("email", "study@yantitong.local", "code", code), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40107));
    }

    /** 登录邮箱不存在：返回 40401 */
    @Test
    void emailLoginEmailNotFound() throws Exception {
        perform(post("/auth/email/login"),
                body("email", "unregistered@example.com", "code", "123456"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401));
    }

    /** 登录参数校验：非 6 位数字返回 40000 */
    @Test
    void emailLoginParamInvalid() throws Exception {
        perform(post("/auth/email/login"),
                body("email", "study@yantitong.local", "code", "abc"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 禁用账号不允许验证码登录：返回 40105（先建禁用用户） */
    @Test
    void emailLoginDisabledAccount() throws Exception {
        perform(post("/admin/users"), body(
                "username", "disabled01",
                "email", "disabled01@example.com",
                "password", "abc123456",
                "status", 0,
                "roleCodes", new String[]{"STUDENT"}), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));

        String code = emailCodeService.createCode(
                EmailCodeService.PURPOSE_LOGIN, "disabled01@example.com");
        perform(post("/auth/email/login"),
                body("email", "disabled01@example.com", "code", code), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40105));
    }
}
