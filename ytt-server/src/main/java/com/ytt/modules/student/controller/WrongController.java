package com.ytt.modules.student.controller;

import com.ytt.common.Result;
import com.ytt.entity.WrongQuestion;
import com.ytt.modules.student.service.WrongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员端-错题本接口
 */
@Tag(name = "学员端-错题本", description = "错题列表与标记掌握")
@RestController
@RequestMapping("/wrong")
public class WrongController {

    private final WrongService wrongService;

    public WrongController(WrongService wrongService) {
        this.wrongService = wrongService;
    }

    @Operation(summary = "错题列表（含题面与解析）")
    @GetMapping("/list")
    public Result<List<WrongQuestion>> list() {
        return Result.success(wrongService.list());
    }

    @Operation(summary = "标记已掌握")
    @PutMapping("/{id}/master")
    public Result<Void> master(@PathVariable Long id) {
        wrongService.master(id);
        return Result.success();
    }
}
