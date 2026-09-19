package com.ytt.modules.auth.controller;

import com.ytt.common.RateLimit;
import com.ytt.common.Result;
import com.ytt.modules.auth.AuthService;
import com.ytt.modules.auth.dto.EmailCodeLoginRequest;
import com.ytt.modules.auth.dto.EmailCodeSendRequest;
import com.ytt.modules.auth.dto.LoginRequest;
import com.ytt.modules.auth.dto.RefreshRequest;
import com.ytt.modules.auth.dto.RegisterRequest;
import com.ytt.modules.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证接口：登录 / 注册 / 刷新令牌 / 注销 / 当前用户
 */
@Tag(name = "认证模块", description = "登录、注册、令牌刷新与注销")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录")
    @RateLimit(capacity = 5, period = 60, key = "login")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Validated @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, getClientIp(httpRequest)));
    }

    @Operation(summary = "用户注册")
    @RateLimit(capacity = 10, period = 60, key = "register")
    @PostMapping("/register")
    public Result<TokenResponse> register(@Validated @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Validated @RequestBody RefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    @Operation(summary = "发送邮箱登录验证码（6 位数字，5 分钟有效）")
    @RateLimit(capacity = 10, period = 60, key = "email-code")
    @PostMapping("/email/code")
    public Result<Void> sendEmailCode(@Validated @RequestBody EmailCodeSendRequest request,
                                      HttpServletRequest httpRequest) {
        authService.sendLoginCode(request, getClientIp(httpRequest));
        return Result.success();
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/email/login")
    public Result<TokenResponse> emailLogin(@Validated @RequestBody EmailCodeLoginRequest request,
                                            HttpServletRequest httpRequest) {
        return Result.success(authService.emailLogin(request, getClientIp(httpRequest)));
    }

    @Operation(summary = "注销登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return Result.success();
    }

    /** 获取客户端真实 IP（DEF-03 修复：不信任 X-Forwarded-For，防止伪造） */
    private String getClientIp(HttpServletRequest request) {
        // 仅使用真实连接 IP；若部署在反向代理后，应由网关层负责真实 IP 透传与校验
        return request.getRemoteAddr();
    }
}
