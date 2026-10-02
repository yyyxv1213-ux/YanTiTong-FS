package com.ytt;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 学员端-刷题训练集成测试：抽题（不泄露答案）、提交作答判分、异常题目处理。
 * 类执行前先做 H2 种子数据修正（见 test-h2-fix.sql）。
 */
@Sql(scripts = "/test-h2-fix.sql")
class PracticeTest extends BaseIntegrationTest {

    /** 抽取训练题目：返回 5 题且不包含答案与解析字段 */
    @Test
    void listQuestionsWithoutAnswer() throws Exception {
        perform(get("/practice/questions?subject=ds&limit=5"), null, tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].stem").isNotEmpty())
                .andExpect(jsonPath("$.data[0].options").isArray())
                .andExpect(jsonPath("$.data[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data[0].analysis").doesNotExist());
    }

    /** 提交正确作答：判分 correct=true 并返回正确答案（内置题库第 1 题答案为 [1]） */
    @Test
    void submitCorrectAnswer() throws Exception {
        perform(post("/practice/submit"), body(
                "questionId", 1,
                "userAnswer", Arrays.asList(1),
                "mode", "sequence"), tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.answer[0]").value(1));
    }

    /** 提交错误作答：判分 correct=false */
    @Test
    void submitWrongAnswer() throws Exception {
        perform(post("/practice/submit"), body(
                "questionId", 1,
                "userAnswer", Arrays.asList(0),
                "mode", "sequence"), tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.correct").value(false));
    }

    /** 空作答判错 */
    @Test
    void submitEmptyAnswer() throws Exception {
        perform(post("/practice/submit"), body(
                "questionId", 1,
                "userAnswer", Collections.emptyList(),
                "mode", "sequence"), tokenOf("study"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.correct").value(false));
    }

    /** 作答不存在的题目：返回 40402 */
    @Test
    void submitQuestionNotFound() throws Exception {
        perform(post("/practice/submit"), body(
                "questionId", 999999,
                "userAnswer", Arrays.asList(0),
                "mode", "sequence"), tokenOf("study"))
                .andExpect(jsonPath("$.code").value(40402));
    }

    /** 未登录访问刷题接口：返回 40100 */
    @Test
    void practiceUnauthorized() throws Exception {
        perform(get("/practice/questions"), null, null)
                .andExpect(jsonPath("$.code").value(40100));
    }
}
