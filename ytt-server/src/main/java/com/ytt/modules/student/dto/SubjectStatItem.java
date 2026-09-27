package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分科统计单项
 */
@Schema(description = "分科统计单项")
public class SubjectStatItem {

    @Schema(description = "科目编码")
    private String subject;

    @Schema(description = "该科累计刷题数")
    private Long total;

    @Schema(description = "该科答对数")
    private Long correct;

    @Schema(description = "正确率（百分数，保留一位小数）")
    private double accuracy;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getCorrect() { return correct; }
    public void setCorrect(Long correct) { this.correct = correct; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
}
