package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.entity.SysRole;
import com.ytt.entity.SysUser;
import com.ytt.entity.SysUserRole;
import com.ytt.mapper.SysRoleMapper;
import com.ytt.mapper.SysUserMapper;
import com.ytt.mapper.SysUserRoleMapper;
import com.ytt.modules.student.dto.PasswordChangeRequest;
import com.ytt.modules.student.dto.ProfileUpdateRequest;
import com.ytt.security.JwtUtil;
import com.ytt.security.SecurityUtils;
import com.ytt.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学员端-个人中心服务
 */
@Service
public class ProfileService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    public ProfileService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                          SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, TokenBlacklist tokenBlacklist) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
    }

    public SysUser get() {
        SysUser user = currentUser();
        fillRoles(user);
        return user;
    }

    public SysUser update(ProfileUpdateRequest request) {
        SysUser user = currentUser();
        user.setRealName(request.getRealName());
        userMapper.updateById(user);
        fillRoles(user);
        return user;
    }

    /**
     * 修改密码：校验原密码 -> 更新 -> 当前 access token 加入黑名单强制重新登录
     */
    public void changePassword(PasswordChangeRequest request, String accessToken) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        SysUser user = currentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        if (accessToken != null && jwtUtil.validate(accessToken)) {
            Claims claims = jwtUtil.parse(accessToken);
            tokenBlacklist.add(accessToken, claims.getExpiration().getTime());
        }
    }

    private SysUser currentUser() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void fillRoles(SysUser user) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<String> roles = relations.stream()
                .map(rel -> {
                    SysRole role = roleMapper.selectById(rel.getRoleId());
                    return role == null ? null : role.getCode();
                })
                .filter(code -> code != null)
                .collect(Collectors.toList());
        user.setRoles(roles);
    }
}
