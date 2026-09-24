package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 模拟考试组卷请求
 */
@Schema(description = "模拟考试组卷请求")
public class ExamStartRequest {

    @Schema(description = "题目数量（5-100）", example = "20")
    @Min(value = 5, message = "题目数量至少 5 道")
    @Max(value = 100, message = "题目数量最多 100 道")
    private Integer count = 20;

    @Schema(description = "考试时长（分钟，5-180）", example = "30")
    @Min(value = 5, message = "考试时长至少 5 分钟")
    @Max(value = 180, message = "考试时长最多 180 分钟")
    private Integer minutes = 30;

    @Schema(description = "题库编码，默认 ky408", example = "ky408")
    private String bankCode = "ky408";

    @Schema(description = "限定科目（须属于所选题库），为空表示该题库全部科目混合")
    private String subject;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Integer getMinutes() { return minutes; }
    public void setMinutes(Integer minutes) { this.minutes = minutes; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}
