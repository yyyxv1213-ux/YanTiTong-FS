package com.ytt;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 题库类别体系集成测试（V4）：
 * 1. /common/banks 公开返回已上线题库及其科目定义；
 * 2. 考研408 扩容后库存 120 题、四科各 30 题；
 * 3. 刷题 / 组卷 / 统计接口按 bankCode 隔离，非法题库与越权科目返回参数错误。
 */
@Sql(scripts = "/test-h2-fix.sql")
class BankSwitchTest extends BaseIntegrationTest {

    /** 公开接口：返回已上线题库，首个为考研408 且含 4 个科目定义 */
    @Test
    void listEnabledBanks() throws Exception {
        perform(get("/common/banks"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].code").value("ky408"))
                .andExpect(jsonPath("$.data[0].name").value("考研408·计算机学科专业基础"))
                .andExpect(jsonPath("$.data[0].subjects", hasSize(4)))
                .andExpect(jsonPath("$.data[0].subjects[0].code").isNotEmpty())
                .andExpect(jsonPath("$.data[0].subjects[0].name").isNotEmpty());
    }

    /** 扩容验收：数据结构/组成原理/操作系统/计算机网络各 30 题 */
    @Test
    void questionCountPerSubject() throws Exception {
        for (String subject : Arrays.asList("ds", "co", "os", "cn")) {
            perform(get("/practice/questions?bankCode=ky408&subject=" + subject + "&limit=50"),
                    null, tokenOf("study"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data", hasSize(30)));
        }
    }

    /** 不传 bankCode 时默认题库 ky408 同样可取到全部 120 题 */
    @Test
    void defaultBankWhenMissing() throws Exception {
        perform(get("/practice/questions?limit=50"), null, tokenOf("study"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(50)));
    }

    /** 非法题库编码：刷题接口返回参数错误 40000 */
    @Test
    void practiceWithUnknownBank() throws Exception {
        perform(get("/practice/questions?bankCode=not-exist&limit=5"), null, tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 组卷：合法题库 + 科目正常生成试卷 */
    @Test
    void examStartWithinBank() throws Exception {
        perform(post("/exam/start"), body(
                "bankCode", "ky408",
                "subject", "ds",
                "count", 5,
                "minutes", 30), tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.questions", hasSize(5)))
                .andExpect(jsonPath("$.data.questions[0].answer").doesNotExist());
    }

    /** 组卷：科目不属于该题库时返回参数错误 40000 */
    @Test
    void examStartWithSubjectOutOfBank() throws Exception {
        perform(post("/exam/start"), body(
                "bankCode", "ky408",
                "subject", "math",
                "count", 5,
                "minutes", 30), tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 组卷：非法题库编码返回参数错误 40000 */
    @Test
    void examStartWithUnknownBank() throws Exception {
        perform(post("/exam/start"), body(
                "bankCode", "not-exist",
                "count", 5,
                "minutes", 30), tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** 学习统计接口支持 bankCode 参数且正常返回 */
    @Test
    void statsWithBankCode() throws Exception {
        perform(get("/stats/overview?bankCode=ky408"), null, tokenOf("study"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isMap());
        perform(get("/stats/subject?bankCode=ky408"), null, tokenOf("study"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(4)));
    }
}
