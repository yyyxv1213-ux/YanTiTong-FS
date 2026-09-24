package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 模拟考试交卷请求
 */
@Schema(description = "模拟考试交卷请求")
public class ExamSubmitRequest {

    @Schema(description = "组卷时下发的考试标识", required = true)
    @NotBlank(message = "考试标识不能为空")
    private String examKey;

    @Schema(description = "作答明细列表", required = true)
    @NotNull(message = "作答明细不能为空")
    @Valid
    private List<ExamAnswerItem> answers;

    @Schema(description = "考试用时（秒）", required = true)
    @NotNull(message = "考试用时不能为空")
    @Min(value = 0, message = "考试用时不能为负数")
    private Integer durationSeconds;

    /**
     * 单题作答明细
     */
    @Schema(description = "单题作答明细")
    public static class ExamAnswerItem {

        @Schema(description = "题目 ID", required = true)
        @NotNull(message = "题目 ID 不能为空")
        private Long questionId;

        @Schema(description = "用户作答选项下标数组，空数组表示未作答")
        private List<Integer> userAnswer;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public List<Integer> getUserAnswer() { return userAnswer; }
        public void setUserAnswer(List<Integer> userAnswer) { this.userAnswer = userAnswer; }
    }

    public String getExamKey() { return examKey; }
    public void setExamKey(String examKey) { this.examKey = examKey; }
    public List<ExamAnswerItem> getAnswers() { return answers; }
    public void setAnswers(List<ExamAnswerItem> answers) { this.answers = answers; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
