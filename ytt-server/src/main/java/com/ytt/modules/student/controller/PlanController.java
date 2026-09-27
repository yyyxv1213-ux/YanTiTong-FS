package com.ytt.modules.student.controller;

import com.ytt.common.Result;
import com.ytt.entity.StudyPlan;
import com.ytt.modules.student.dto.PlanUpdateRequest;
import com.ytt.modules.student.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端-学习计划接口
 */
@Tag(name = "学员端-学习计划", description = "每日目标与考试日期管理")
@RestController
@RequestMapping("/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "查询学习计划")
    @GetMapping
    public Result<StudyPlan> get() {
        return Result.success(planService.get());
    }

    @Operation(summary = "更新学习计划")
    @PutMapping
    public Result<StudyPlan> update(@Validated @RequestBody PlanUpdateRequest request) {
        return Result.success(planService.update(request));
    }
}
