package com.ytt.modules.student.controller;

import com.ytt.common.PageResult;
import com.ytt.common.Result;
import com.ytt.entity.ExamRecord;
import com.ytt.modules.student.dto.ExamReportResponse;
import com.ytt.modules.student.dto.ExamStartRequest;
import com.ytt.modules.student.dto.ExamStartResponse;
import com.ytt.modules.student.dto.ExamSubmitRequest;
import com.ytt.modules.student.dto.ExamSubmitResponse;
import com.ytt.modules.student.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端-模拟考试接口
 */
@Tag(name = "学员端-模拟考试", description = "组卷、交卷、历史与成绩报告")
@RestController
@RequestMapping("/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(summary = "开始考试（组卷）")
    @PostMapping("/start")
    public Result<ExamStartResponse> start(@Validated @RequestBody ExamStartRequest request) {
        return Result.success(examService.start(request));
    }

    @Operation(summary = "交卷判分")
    @PostMapping("/submit")
    public Result<ExamSubmitResponse> submit(@Validated @RequestBody ExamSubmitRequest request) {
        return Result.success(examService.submit(request));
    }

    @Operation(summary = "考试历史")
    @GetMapping("/history")
    public Result<PageResult<ExamRecord>> history(@RequestParam(required = false, defaultValue = "1") Long pageNum,
                                                  @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.success(examService.history(pageNum, pageSize));
    }

    @Operation(summary = "成绩报告（含逐题明细）")
    @GetMapping("/{id}")
    public Result<ExamReportResponse> report(@PathVariable Long id) {
        return Result.success(examService.report(id));
    }
}
