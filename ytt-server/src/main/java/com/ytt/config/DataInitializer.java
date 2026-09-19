package com.ytt.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.entity.SysRole;
import com.ytt.entity.SysUser;
import com.ytt.entity.SysUserRole;
import com.ytt.mapper.SysRoleMapper;
import com.ytt.mapper.SysUserMapper;
import com.ytt.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内置账号初始化：首次启动时创建管理员与演示学员账号。
 * 密码必须由 PasswordEncoder 动态加密（BCrypt 每次结果不同），
 * 无法以明文哈希形式写入 SQL 种子脚本，故在应用启动后补齐。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** 内置账号初始密码，首次登录后建议修改 */
    private static final String INIT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        createUserIfAbsent("admin", "系统管理员", "admin@yantitong.local", "ADMIN");
        createUserIfAbsent("study", "演示学员", "study@yantitong.local", "STUDENT");
    }

    /** 若指定用户名不存在则创建账号并分配角色，已存在则跳过（幂等） */
    private void createUserIfAbsent(String username, String realName, String email, String roleCode) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count > 0) {
            return;
        }
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode));
        if (role == null) {
            log.warn("角色 {} 不存在，跳过内置账号 {} 创建", roleCode, username);
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(INIT_PASSWORD));
        user.setRealName(realName);
        user.setStatus(1);
        userMapper.insert(user);
        userRoleMapper.insert(new SysUserRole(user.getId(), role.getId()));
        log.info("内置账号已创建：{}/{}（角色 {}）", username, INIT_PASSWORD, roleCode);
    }
}
