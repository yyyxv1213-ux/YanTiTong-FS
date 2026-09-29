package com.ytt.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.common.PageResult;
import com.ytt.entity.SysRole;
import com.ytt.entity.SysUser;
import com.ytt.entity.SysUserRole;
import com.ytt.mapper.SysRoleMapper;
import com.ytt.mapper.SysUserMapper;
import com.ytt.mapper.SysUserRoleMapper;
import com.ytt.modules.admin.dto.AdminUserQuery;
import com.ytt.modules.admin.dto.AdminUserSaveRequest;
import com.ytt.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员-用户账号管理服务
 */
@Service
public class AdminUserService {

    /** 内置管理员用户名，禁止删除/改名/降权 */
    private static final String BUILTIN_ADMIN = "admin";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                            SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 分页查询用户（关键字模糊 + 状态筛选），并组装角色编码 */
    public PageResult<SysUser> page(AdminUserQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillRoles(page.getRecords());
        return PageResult.of(page);
    }

    /** 查询用户详情（含角色） */
    public SysUser detail(Long id) {
        SysUser user = getOrThrow(id);
        fillRoles(Collections.singletonList(user));
        return user;
    }

    /** 新增用户：BCrypt 加密密码并分配角色 */
    @Transactional(rollbackFor = Exception.class)
    public void create(AdminUserSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新增用户必须设置密码");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String email = request.getEmail().trim();
        Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email));
        if (emailCount > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        List<SysRole> roles = validateRoles(request.getRoleCodes());
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(StringUtils.hasText(request.getRealName())
                ? request.getRealName() : request.getUsername());
        user.setStatus(request.getStatus());
        userMapper.insert(user);
        insertUserRoles(user.getId(), roles);
    }

    /** 编辑用户：更新基础信息，可选改密，重分配角色 */
    @Transactional(rollbackFor = Exception.class)
    public void update(AdminUserSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "缺少用户 ID");
        }
        SysUser existing = getOrThrow(request.getId());
        if (BUILTIN_ADMIN.equals(existing.getUsername()) && !BUILTIN_ADMIN.equals(request.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "内置管理员用户名不可修改");
        }
        if (!existing.getUsername().equals(request.getUsername())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.getUsername()));
            if (count > 0) {
                throw new BizException(ErrorCode.USERNAME_EXISTS);
            }
        }
        String email = request.getEmail().trim();
        if (!email.equals(existing.getEmail())) {
            Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, email));
            if (emailCount > 0) {
                throw new BizException(ErrorCode.EMAIL_EXISTS);
            }
        }
        List<SysRole> roles = validateRoles(request.getRoleCodes());
        if (BUILTIN_ADMIN.equals(existing.getUsername())) {
            ensureKeepAdmin(roles);
        }
        existing.setUsername(request.getUsername());
        existing.setEmail(email);
        existing.setRealName(StringUtils.hasText(request.getRealName())
                ? request.getRealName() : existing.getUsername());
        existing.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(existing);
        replaceUserRoles(existing.getId(), roles);
    }

    /** 启用/禁用用户：不能禁用当前登录的管理员自己 */
    public void updateStatus(Long id, Integer status) {
        SysUser target = getOrThrow(id);
        Long currentId = SecurityUtils.getUserId();
        if (target.getId().equals(currentId) && status != null && status == 0) {
            throw new BizException(ErrorCode.CANNOT_DISABLE_SELF);
        }
        target.setStatus(status);
        userMapper.updateById(target);
    }

    /** 重置用户密码 */
    public void resetPassword(Long id, String newPassword) {
        SysUser target = getOrThrow(id);
        target.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(target);
    }

    /** 分配角色：删除旧关联后批量插入 */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, List<String> roleCodes) {
        SysUser target = getOrThrow(id);
        List<SysRole> roles = validateRoles(roleCodes);
        if (BUILTIN_ADMIN.equals(target.getUsername())) {
            ensureKeepAdmin(roles);
        }
        replaceUserRoles(target.getId(), roles);
    }

    /** 删除用户（逻辑删除）：内置管理员不可删除 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser target = getOrThrow(id);
        if (BUILTIN_ADMIN.equals(target.getUsername())) {
            throw new BizException(ErrorCode.CANNOT_DELETE_ADMIN);
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    /** 全部角色列表（供前端分配角色时下拉选择） */
    public List<SysRole> allRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    // ==================== 内部方法 ====================

    private SysUser getOrThrow(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** 批量填充用户角色编码，避免 N+1 查询 */
    private void fillRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        Map<Long, String> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getCode));
        Map<Long, List<String>> userRoles = relations.stream().collect(Collectors.groupingBy(
                SysUserRole::getUserId,
                Collectors.mapping(rel -> roleMap.get(rel.getRoleId()), Collectors.toList())));
        for (SysUser user : users) {
            List<String> codes = userRoles.getOrDefault(user.getId(), Collections.emptyList())
                    .stream().filter(code -> code != null).collect(Collectors.toList());
            user.setRoles(codes);
        }
    }

    /** 校验角色编码存在性，返回角色实体列表 */
    private List<SysRole> validateRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "至少分配一个角色");
        }
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getCode, roleCodes));
        if (roles.size() != roleCodes.stream().distinct().count()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在无效的角色编码");
        }
        return roles;
    }

    /** 内置管理员必须保留 ADMIN 角色 */
    private void ensureKeepAdmin(List<SysRole> roles) {
        boolean hasAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
        if (!hasAdmin) {
            throw new BizException(ErrorCode.PARAM_ERROR, "内置管理员必须保留 ADMIN 角色");
        }
    }

    private void insertUserRoles(Long userId, List<SysRole> roles) {
        for (SysRole role : roles) {
            userRoleMapper.insert(new SysUserRole(userId, role.getId()));
        }
    }

    private void replaceUserRoles(Long userId, List<SysRole> roles) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        insertUserRoles(userId, roles);
    }
}
