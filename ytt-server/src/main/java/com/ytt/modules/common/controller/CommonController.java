package com.ytt.modules.common.controller;

import com.ytt.common.Result;
import com.ytt.entity.ExamBank;
import com.ytt.modules.common.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共接口：题库类别等基础元数据
 */
@Tag(name = "公共接口", description = "题库类别等全局元数据")
@RestController
@RequestMapping("/common")
public class CommonController {

    private final BankService bankService;

    public CommonController(BankService bankService) {
        this.bankService = bankService;
    }

    @Operation(summary = "已上线题库类别列表")
    @GetMapping("/banks")
    public Result<List<ExamBank>> banks() {
        return Result.success(bankService.listEnabled());
    }
}
