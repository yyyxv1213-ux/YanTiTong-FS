package com.ytt.modules.student.controller;

import com.ytt.common.Result;
import com.ytt.modules.student.dto.StatsOverviewResponse;
import com.ytt.modules.student.dto.SubjectStatItem;
import com.ytt.modules.student.dto.TrendItem;
import com.ytt.modules.student.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学员端-学习统计接口
 */
@Tag(name = "学员端-学习统计", description = "总览、分科统计与刷题趋势（均按题库隔离）")
@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @Operation(summary = "学习总览")
    @GetMapping("/overview")
    public Result<StatsOverviewResponse> overview(@RequestParam(required = false) String bankCode) {
        return Result.success(statsService.overview(bankCode));
    }

    @Operation(summary = "分科统计")
    @GetMapping("/subject")
    public Result<List<SubjectStatItem>> subject(@RequestParam(required = false) String bankCode) {
        return Result.success(statsService.subjectStats(bankCode));
    }

    @Operation(summary = "近 14 天刷题趋势")
    @GetMapping("/trend")
    public Result<List<TrendItem>> trend(@RequestParam(required = false) String bankCode) {
        return Result.success(statsService.trend(bankCode));
    }
}
