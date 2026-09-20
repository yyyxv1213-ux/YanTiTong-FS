package com.ytt.modules.student.controller;

import com.ytt.common.Result;
import com.ytt.entity.Question;
import com.ytt.modules.student.dto.PracticeSubmitRequest;
import com.ytt.modules.student.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学员端-刷题训练接口
 */
@Tag(name = "学员端-刷题训练", description = "按条件抽题与作答提交")
@RestController
@RequestMapping("/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @Operation(summary = "抽取训练题目（不返回答案）")
    @GetMapping("/questions")
    public Result<List<Question>> questions(@RequestParam(required = false) String bankCode,
                                            @RequestParam(required = false) String subject,
                                            @RequestParam(required = false) String chapter,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer difficulty,
                                            @RequestParam(required = false, defaultValue = "sequence") String mode,
                                            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(practiceService.listQuestions(
                bankCode, subject, chapter, type, difficulty, mode, limit));
    }

    @Operation(summary = "提交作答并判分")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@Validated @RequestBody PracticeSubmitRequest request) {
        return Result.success(practiceService.submit(request));
    }
}
