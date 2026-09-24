package com.ytt.modules.student.dto;

import com.ytt.entity.ExamDetail;
import com.ytt.entity.ExamRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 成绩报告响应
 */
@Schema(description = "成绩报告响应")
public class ExamReportResponse {

    @Schema(description = "考试记录")
    private ExamRecord record;

    @Schema(description = "逐题明细（含题面、答案、解析）")
    private List<ExamDetail> details;

    public ExamRecord getRecord() { return record; }
    public void setRecord(ExamRecord record) { this.record = record; }
    public List<ExamDetail> getDetails() { return details; }
    public void setDetails(List<ExamDetail> details) { this.details = details; }
}
