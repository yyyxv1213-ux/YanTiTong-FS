package com.ytt;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证模块集成测试：登录 / 注册 / 刷新令牌 / 参数校验。
 * 注意：登录接口限流为每分钟 5 次（全测试套件共享），本类仅保留必要的登录调用，
 * 其余用直接签发令牌的方式获取凭据。
 */
class AuthTest extends BaseIntegrationTest {

    /** 登录成功：返回双 Token 与用户信息 */
    @Test
    void loginSuccess() throws Exception {
        perform(post("/auth/login"),
                body("username", "admin", "password", "123456"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));
    }

    /** 密码错误：返回 40104 用户名或密码错误 */
    @Test
    void loginWrongPassword() throws Exception {
        perform(post("/auth/login"),
                body("username", "admin", "password", "wrongpass1"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40104));
    }

    /** 登录参数校验失败：用户名为空返回 40000 */
    @Test
    void loginParamInvalid() throws Exception {
        perform(post("/auth/login"), body("username", "", "password", "123456"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 注册成功：自动分配 STUDENT 角色并直接签发 Token */
    @Test
    void registerSuccess() throws Exception {
        perform(post("/auth/register"), body(
                "username", "regtest01",
                "email", "regtest01@example.com",
                "password", "abc123456",
                "confirmPassword", "abc123456",
                "realName", "集成测试学员"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("regtest01"))
                .andExpect(jsonPath("$.data.user.email").value("regtest01@example.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("STUDENT"));
    }

    /** 注册用户名已存在：返回 40901 */
    @Test
    void registerDuplicateUsername() throws Exception {
        perform(post("/auth/register"), body(
                "username", "admin",
                "email", "whatever@example.com",
                "password", "abc123456",
                "confirmPassword", "abc123456"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901));
    }

    /** 注册邮箱已存在：返回 40905 */
    @Test
    void registerDuplicateEmail() throws Exception {
        perform(post("/auth/register"), body(
                "username", "another01",
                "email", "admin@yantitong.local",
                "password", "abc123456",
                "confirmPassword", "abc123456"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40905));
    }

    /** 注册两次密码不一致：返回非 0 错误码 */
    @Test
    void registerPasswordMismatch() throws Exception {
        perform(post("/auth/register"), body(
                "username", "regtest02",
                "email", "regtest02@example.com",
                "password", "abc123456",
                "confirmPassword", "abc654321"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }

    /** 刷新令牌：凭 refreshToken 换取新的双 Token（直接签发 refresh，避免占用登录限流） */
    @Test
    void refreshToken() throws Exception {
        String refreshToken = jwtUtil.generateRefreshToken(userIdOf("study"), "study");
        perform(post("/auth/refresh"), body("refreshToken", refreshToken), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("study"));
    }

    /** 非法 refreshToken：返回非 0 错误码 */
    @Test
    void refreshTokenInvalid() throws Exception {
        perform(post("/auth/refresh"), body("refreshToken", "invalid.token.value"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(0)));
    }
}
