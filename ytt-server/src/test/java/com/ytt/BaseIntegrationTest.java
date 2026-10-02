package com.ytt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytt.entity.SysUser;
import com.ytt.mapper.SysUserMapper;
import com.ytt.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 集成测试基类：启动完整 Spring 上下文。
 * 测试库为 H2 内存库（MySQL 兼容模式），由 Flyway 执行 V1/V2 迁移建表与种子数据，
 * 启动后 DataInitializer 自动创建内置账号：admin/123456(ADMIN)、study/123456(STUDENT)。
 * 所有子类共享同一应用上下文。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JwtUtil jwtUtil;
    @Autowired
    protected SysUserMapper sysUserMapper;
    @Autowired
    protected com.ytt.modules.auth.EmailCodeService emailCodeService;

    /**
     * 直接为指定用户签发 access token。
     * 登录接口有每分钟 5 次的限流，测试中获取 Token 应优先使用本方法，
     * 仅认证模块自身的用例才调用 /auth/login。
     */
    protected String tokenOf(String username) {
        SysUser user = findUser(username);
        return jwtUtil.generateAccessToken(user.getId(), user.getUsername());
    }

    /** 查询用户 ID */
    protected Long userIdOf(String username) {
        return findUser(username).getId();
    }

    private SysUser findUser(String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        assertNotNull(user, "测试账号不存在: " + username);
        return user;
    }

    /** 快速构建请求体 Map（键值成对传入） */
    protected Map<String, Object> body(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    /** 执行请求：可选 JSON 请求体与 Bearer Token */
    protected ResultActions perform(MockHttpServletRequestBuilder builder, Object requestBody, String token)
            throws Exception {
        return perform(builder, requestBody, token, null);
    }

    /** 执行请求：额外支持自定义请求头（如 X-Forwarded-For） */
    protected ResultActions perform(MockHttpServletRequestBuilder builder, Object requestBody, String token,
                                    Map<String, String> extraHeaders)
            throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (extraHeaders != null) {
            extraHeaders.forEach(builder::header);
        }
        if (requestBody != null) {
            builder.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody));
        }
        return mockMvc.perform(builder);
    }
}
