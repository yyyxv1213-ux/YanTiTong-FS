package com.ytt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.entity.Question;
import com.ytt.mapper.QuestionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端-题目管理集成测试：
 * 分页与条件筛选、新增、编辑、删除（逻辑删除）、参数校验、不存在资源处理。
 * 类执行前先做 H2 种子数据修正（见 test-h2-fix.sql）。
 */
@Sql(scripts = "/test-h2-fix.sql")
class AdminQuestionTest extends BaseIntegrationTest {

    @Autowired
    private QuestionMapper questionMapper;

    /** 题目分页列表：V4 扩容后考研408 题库共 120 题 */
    @Test
    void pageQuestions() throws Exception {
        perform(get("/admin/questions?pageNum=1&pageSize=10"), null, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(120))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /** 按题库编码筛选：仅返回该题库下的题目 */
    @Test
    void pageQuestionsByBank() throws Exception {
        perform(get("/admin/questions?pageNum=1&pageSize=10&bankCode=ky408"), null, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(120));
    }

    /** 条件筛选：科目 ds + 题型 multi 的记录字段匹配 */
    @Test
    void pageQuestionsWithFilter() throws Exception {
        perform(get("/admin/questions?pageNum=1&pageSize=5&subject=ds&type=multi"), null, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].subject").value("ds"))
                .andExpect(jsonPath("$.data.records[0].type").value("multi"));
    }

    /** 题目新增-编辑-详情-删除全流程 */
    @Test
    void questionCrudFlow() throws Exception {
        Object createRequest = body(
                "type", "single",
                "bankCode", "ky408",
                "subject", "os",
                "categoryId", 12,
                "chapter", "进程管理",
                "difficulty", 2,
                "stem", "集成测试-进程的基本状态题干",
                "options", Arrays.asList("就绪态", "运行态", "阻塞态", "以上都是"),
                "answer", Arrays.asList(3),
                "analysis", "进程基本状态包括就绪、运行、阻塞。",
                "source", "集成测试");

        // 新增
        perform(post("/admin/questions"), createRequest, tokenOf("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        Question created = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getStem, "集成测试-进程的基本状态题干"));
        assertNotNull(created, "新增题目未入库");
        Long id = created.getId();

        // 编辑（难度 2 -> 3）
        perform(put("/admin/questions/" + id), body(
                "type", "single",
                "bankCode", "ky408",
                "subject", "os",
                "categoryId", 12,
                "chapter", "进程管理",
                "difficulty", 3,
                "stem", "集成测试-进程的基本状态题干",
                "options", Arrays.asList("就绪态", "运行态", "阻塞态", "以上都是"),
                "answer", Arrays.asList(3),
                "analysis", "解析已更新",
                "source", "集成测试"), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
        perform(get("/admin/questions/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.difficulty").value(3))
                .andExpect(jsonPath("$.data.options.length()").value(4))
                .andExpect(jsonPath("$.data.answer[0]").value(3));

        // 删除（逻辑删除），详情应返回 40402
        perform(delete("/admin/questions/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(0));
        perform(get("/admin/questions/" + id), null, tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(40402));
        assertNull(questionMapper.selectById(id), "逻辑删除后不应再查询到该题目");
    }

    /** 新增题目参数校验失败：非法题型与过少选项均返回 40000 */
    @Test
    void createQuestionParamInvalid() throws Exception {
        // 非法题型
        perform(post("/admin/questions"), body(
                "type", "essay",
                "bankCode", "ky408",
                "subject", "os",
                "categoryId", 12,
                "chapter", "进程管理",
                "difficulty", 2,
                "stem", "非法题型题干",
                "options", Arrays.asList("A", "B"),
                "answer", Arrays.asList(0)), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(40000));

        // 选项少于 2 项
        perform(post("/admin/questions"), body(
                "type", "single",
                "bankCode", "ky408",
                "subject", "os",
                "categoryId", 12,
                "chapter", "进程管理",
                "difficulty", 2,
                "stem", "选项不足题干",
                "options", Arrays.asList("仅一项"),
                "answer", Arrays.asList(0)), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 编辑不存在的题目：返回 40402 */
    @Test
    void updateQuestionNotFound() throws Exception {
        perform(put("/admin/questions/999999"), body(
                "type", "single",
                "bankCode", "ky408",
                "subject", "os",
                "categoryId", 12,
                "chapter", "进程管理",
                "difficulty", 2,
                "stem", "不存在的题目",
                "options", Arrays.asList("A", "B"),
                "answer", Arrays.asList(0)), tokenOf("admin"))
                .andExpect(jsonPath("$.code").value(40402));
    }

    /** 学员角色访问题目管理：返回 40300 */
    @Test
    void questionManageForbiddenForStudent() throws Exception {
        perform(get("/admin/questions"), null, tokenOf("study"))
                .andExpect(jsonPath("$.code").value(40300));
    }
}
