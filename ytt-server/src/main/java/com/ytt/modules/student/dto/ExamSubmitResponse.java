package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 交卷判分响应
 */
@Schema(description = "交卷判分响应")
public class ExamSubmitResponse {

    @Schema(description = "考试记录 ID，用于查询成绩报告")
    private Long recordId;

    @Schema(description = "得分（百分制）")
    private int score;

    @Schema(description = "答对题数")
    private int correctCount;

    @Schema(description = "试卷题目总数")
    private int totalCount;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
