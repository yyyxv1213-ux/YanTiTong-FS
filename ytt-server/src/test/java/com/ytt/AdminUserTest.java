package com.ytt;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端-用户账号管理集成测试：
 * 分页查询、新增（含查重与参数校验）、禁用/启用、重置密码、编辑、角色分配、越权与未登录拦截。
 * tuser02 相关用例存在顺序依赖（先建后改），故显式指定执行顺序。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminUserTest extends BaseIntegrationTest {

    /** 用户分页列表：总数不少于 2（内置 admin 与 study） */
    @Test
    void pageUsers() throws Exception {
        perform(get("/admin/users?pageNum=1&pageSize=10"), null, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.current").value(1));
    }

    /** 关键字搜索：能定位到内置 admin 账号 */
    @Test
    void pageUsersByKeyword() throws Exception {
        perform(get("/admin/users?pageNum=1&pageSize=10&keyword=admin"), null, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    /** 未登录访问管理端：返回 40100 */
    @Test
    void pageUsersUnauthorized() throws Exception {
        perform(get("/admin/users"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));
    }

    /** 学员角色访问管理端：返回 40300 */
    @Test
    void pageUsersForbiddenForStudent() throws Exception {
        perform(get("/admin/users"), null, tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40300));
    }

    /** 新增用户：成功后重复用户名返回 40901 */
    @Test
    void createUserAndDuplicate() throws Exception {
        Object request = body(
                "username", "tuser01",
                "email", "tuser01@example.com",
                "realName", "测试用户一",
                "password", "abc123456",
                "status", 1,
                "roleCodes", new String[]{"STUDENT"});
        perform(post("/admin/users"), request, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        perform(post("/admin/users"), request, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901));
    }

    /** 新增用户参数校验失败：用户名不满足 4-20 位返回 40000 */
    @Test
    void createUserParamInvalid() throws Exception {
        perform(post("/admin/users"), body(
                "username", "ab",
                "password", "abc123456",
                "status", 1,
                "roleCodes", new String[]{"STUDENT"}), tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 禁用/启用用户：状态变更可通过详情接口验证（同时创建 tuser02 供后续用例使用） */
    @Test
    @Order(1)
    void disableAndEnableUser() throws Exception {
        perform(post("/admin/users"), body(
                "username", "tuser02",
                "email", "tuser02@example.com",
                "realName", "测试用户二",
                "password", "abc123456",
                "status", 1,
                "roleCodes", new String[]{"STUDENT"}), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
        Long id = userIdOf("tuser02");

        perform(put("/admin/users/" + id + "/status"), body("status", 0), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
        perform(get("/admin/users/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(0));

        perform(put("/admin/users/" + id + "/status"), body("status", 1), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
        perform(get("/admin/users/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    /** 编辑用户：不传密码表示不修改密码，姓名更新生效（依赖 tuser02 已创建） */
    @Test
    @Order(2)
    void updateUser() throws Exception {
        Long id = userIdOf("tuser02");
        perform(put("/admin/users/" + id), body(
                "username", "tuser02",
                "email", "tuser02@example.com",
                "realName", "改名后的用户",
                "status", 1,
                "roleCodes", new String[]{"STUDENT"}), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));

        perform(get("/admin/users/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.realName").value("改名后的用户"));
    }

    /** 重置密码安全闭环：注册新账号 -> 重置密码 -> 用新密码登录 */
    @Test
    @Order(5)
    void resetPasswordLoginRoundTrip() throws Exception {
        perform(post("/auth/register"), body(
                "username", "tuser03",
                "email", "tuser03@example.com",
                "password", "abc123456",
                "confirmPassword", "abc123456"), null)
                .andExpect(jsonPath("$.code").value(0));
        Long id = userIdOf("tuser03");

        perform(put("/admin/users/" + id + "/password"),
                body("password", "newpass123"), tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        perform(post("/auth/login"), body("username", "tuser03", "password", "newpass123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("tuser03"));
    }

    /** 重置密码：新密码可正常登录（依赖 tuser02 已创建） */
    @Test
    @Order(3)
    void resetPasswordAndLogin() throws Exception {
        Long id = userIdOf("tuser02");
        perform(put("/admin/users/" + id + "/password"),
                body("password", "newpass123"), tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        perform(post("/auth/login"), body("username", "tuser02", "password", "newpass123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("tuser02"));
    }

    /** 分配角色：学员可被授予 ADMIN 角色（依赖 tuser02 已创建） */
    @Test
    @Order(4)
    void assignRoles() throws Exception {
        Long id = userIdOf("tuser02");
        perform(put("/admin/users/" + id + "/roles"), body(
                "username", "tuser02",
                "email", "tuser02@example.com",
                "realName", "改名后的用户",
                "status", 1,
                "roleCodes", new String[]{"STUDENT", "ADMIN"}), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));

        perform(get("/admin/users/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
    }
}
