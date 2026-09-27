package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 学习总览响应
 */
@Schema(description = "学习总览响应")
public class StatsOverviewResponse {

    @Schema(description = "今日刷题数")
    private Long todayCount;

    @Schema(description = "累计刷题数")
    private Long totalCount;

    @Schema(description = "累计答对数")
    private Long correctCount;

    @Schema(description = "正确率（百分数，保留一位小数）")
    private double accuracy;

    @Schema(description = "每日目标题数")
    private int dailyTarget;

    @Schema(description = "考试日期（yyyy-MM-dd）")
    private String examDate;

    @Schema(description = "距考试剩余天数")
    private long daysLeft;

    public Long getTodayCount() { return todayCount; }
    public void setTodayCount(Long todayCount) { this.todayCount = todayCount; }
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public Long getCorrectCount() { return correctCount; }
    public void setCorrectCount(Long correctCount) { this.correctCount = correctCount; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public int getDailyTarget() { return dailyTarget; }
    public void setDailyTarget(int dailyTarget) { this.dailyTarget = dailyTarget; }
    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }
    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
}
