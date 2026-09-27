package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 刷题趋势单项（单日）
 */
@Schema(description = "刷题趋势单项（单日）")
public class TrendItem {

    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "当日刷题数")
    private Long total;

    @Schema(description = "当日答对数")
    private Long correct;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getCorrect() { return correct; }
    public void setCorrect(Long correct) { this.correct = correct; }
}
