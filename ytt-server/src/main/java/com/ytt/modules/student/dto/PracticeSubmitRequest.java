package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 刷题提交请求
 */
@Schema(description = "刷题提交请求")
public class PracticeSubmitRequest {

    @Schema(description = "题目 ID", required = true)
    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    @Schema(description = "用户作答选项下标数组", required = true)
    @NotNull(message = "作答不能为空")
    private List<Integer> userAnswer;

    @Schema(description = "训练模式：sequence/random/chapter/wrong")
    private String mode;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public List<Integer> getUserAnswer() { return userAnswer; }
    public void setUserAnswer(List<Integer> userAnswer) { this.userAnswer = userAnswer; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
