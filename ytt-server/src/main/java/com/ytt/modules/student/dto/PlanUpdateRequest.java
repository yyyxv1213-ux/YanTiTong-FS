package com.ytt.modules.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 学习计划修改请求
 */
@Schema(description = "学习计划修改请求")
public class PlanUpdateRequest {

    @Schema(description = "每日目标题数（1-500）", required = true, example = "30")
    @NotNull(message = "每日目标不能为空")
    @Min(value = 1, message = "每日目标至少 1 题")
    @Max(value = 500, message = "每日目标最多 500 题")
    private Integer dailyTarget;

    @Schema(description = "考研初试日期（yyyy-MM-dd）", required = true, example = "2026-12-19")
    @NotNull(message = "考试日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate examDate;

    public Integer getDailyTarget() { return dailyTarget; }
    public void setDailyTarget(Integer dailyTarget) { this.dailyTarget = dailyTarget; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
}
