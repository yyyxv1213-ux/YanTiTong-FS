package com.ytt.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.entity.LoginLog;
import com.ytt.entity.SysRole;
import com.ytt.entity.SysUser;
import com.ytt.entity.SysUserRole;
import com.ytt.mapper.LoginLogMapper;
import com.ytt.mapper.SysRoleMapper;
import com.ytt.mapper.SysUserMapper;
import com.ytt.mapper.SysUserRoleMapper;
import com.ytt.modules.auth.dto.EmailCodeLoginRequest;
import com.ytt.modules.auth.dto.EmailCodeSendRequest;
import com.ytt.modules.auth.dto.LoginRequest;
import com.ytt.modules.auth.dto.RefreshRequest;
import com.ytt.modules.auth.dto.RegisterRequest;
import com.ytt.modules.auth.dto.TokenResponse;
import com.ytt.security.JwtUtil;
import com.ytt.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证服务：登录 / 注册 / 刷新令牌 / 注销
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final EmailCodeService emailCodeService;
    private final MailService mailService;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper, LoginLogMapper loginLogMapper,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       TokenBlacklist tokenBlacklist, EmailCodeService emailCodeService,
                       MailService mailService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.emailCodeService = emailCodeService;
        this.mailService = mailService;
    }

    /** 用户名登录：按用户名定位账号 -> 校验密码 -> 签发双 Token -> 写登录日志 */
    public TokenResponse login(LoginRequest request, String ip) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        SysUser user = username == null ? null : userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            saveLog(null, username, ip, 0, "用户不存在");
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            saveLog(user.getId(), username, ip, 0, "用户名或密码错误");
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            saveLog(user.getId(), username, ip, 0, "账号被禁用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        saveLog(user.getId(), username, ip, 1, "登录成功");
        return buildTokenResponse(user);
    }

    /**
     * 发送登录验证码：邮箱必须已注册且账号启用（防止 SMTP 被滥用于任意邮箱）。
     * 生成 6 位数字验证码（5 分钟有效）并发送邮件。
     */
    public void sendLoginCode(EmailCodeSendRequest request, String ip) {
        String email = request.getEmail().trim();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            saveLog(null, email, ip, 0, "邮箱尚未注册");
            throw new BizException(ErrorCode.USER_NOT_FOUND, "该邮箱尚未注册");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            saveLog(user.getId(), email, ip, 0, "账号被禁用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        // 先校验发送频率（60 秒防刷），通过后生成并发送
        String code = emailCodeService.createCode(EmailCodeService.PURPOSE_LOGIN, email);
        mailService.sendLoginCode(email, code);
        saveLog(user.getId(), email, ip, 1, "登录验证码已发送");
    }

    /**
     * 邮箱验证码登录：按邮箱定位账号 -> 校验验证码 -> 签发双 Token。
     * 验证码校验通过且令牌签发成功后才消费验证码（一次性使用）。
     */
    public TokenResponse emailLogin(EmailCodeLoginRequest request, String ip) {
        String email = request.getEmail().trim();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            saveLog(null, email, ip, 0, "邮箱尚未注册");
            throw new BizException(ErrorCode.USER_NOT_FOUND, "该邮箱尚未注册");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            saveLog(user.getId(), email, ip, 0, "账号被禁用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        // 验证码错误/过期直接抛异常，不消费条目（错误可重试，过期自然失效）
        emailCodeService.verify(EmailCodeService.PURPOSE_LOGIN, email, request.getCode());
        saveLog(user.getId(), email, ip, 1, "验证码登录成功");
        TokenResponse response = buildTokenResponse(user);
        // 业务完整成功，验证码立即失效
        emailCodeService.consume(EmailCodeService.PURPOSE_LOGIN, email);
        return response;
    }

    /** 注册：默认分配 STUDENT 角色，用户名与邮箱均需唯一 */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String email = request.getEmail().trim();
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (emailCount > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName() == null || request.getRealName().isEmpty()
                ? request.getUsername() : request.getRealName());
        user.setStatus(1);
        userMapper.insert(user);

        SysRole student = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "STUDENT"));
        if (student != null) {
            userRoleMapper.insert(new SysUserRole(user.getId(), student.getId()));
        }
        return buildTokenResponse(user);
    }

    /** 刷新令牌：旧 refresh 加入黑名单，签发新双 Token（旋转机制） */
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (tokenBlacklist.contains(refreshToken)) {
            throw new BizException(ErrorCode.TOKEN_BLACKLISTED);
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        if (!JwtUtil.TYPE_REFRESH.equals(jwtUtil.getType(claims))) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "请使用 refreshToken 刷新");
        }
        Long userId = jwtUtil.getUserId(claims);
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        // 旧 refresh token 加入黑名单（一次性使用）
        tokenBlacklist.add(refreshToken, claims.getExpiration().getTime());
        return buildTokenResponse(user);
    }

    /** 注销：当前 access token 加入黑名单 */
    public void logout(String accessToken) {
        if (accessToken != null && jwtUtil.validate(accessToken)) {
            Claims claims = jwtUtil.parse(accessToken);
            tokenBlacklist.add(accessToken, claims.getExpiration().getTime());
        }
    }

    /** 组装 Token 响应 */
    private TokenResponse buildTokenResponse(SysUser user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        List<String> roles = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(rel -> {
                    SysRole role = roleMapper.selectById(rel.getRoleId());
                    return role == null ? null : role.getCode();
                })
                .filter(code -> code != null)
                .collect(java.util.stream.Collectors.toList());
        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                user.getId(), user.getUsername(), user.getEmail(), user.getRealName(), roles);
        return new TokenResponse(accessToken, refreshToken, jwtUtil.getAccessExpire(), userInfo);
    }

    private void saveLog(Long userId, String username, String ip, Integer success, String message) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(ip);
        log.setSuccess(success);
        log.setMessage(message);
        loginLogMapper.insert(log);
    }
}
