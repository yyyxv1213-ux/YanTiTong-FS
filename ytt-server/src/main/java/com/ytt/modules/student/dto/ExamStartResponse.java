package com.ytt.modules.student.dto;

import com.ytt.entity.Question;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 模拟考试组卷响应
 */
@Schema(description = "模拟考试组卷响应")
public class ExamStartResponse {

    @Schema(description = "本次考试的会话标识，交卷时回传")
    private String examKey;

    @Schema(description = "考试时长（分钟）")
    private Integer minutes;

    @Schema(description = "试卷题目总数")
    private Integer totalCount;

    @Schema(description = "题目列表（不含答案与解析）")
    private List<Question> questions;

    public String getExamKey() { return examKey; }
    public void setExamKey(String examKey) { this.examKey = examKey; }
    public Integer getMinutes() { return minutes; }
    public void setMinutes(Integer minutes) { this.minutes = minutes; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
