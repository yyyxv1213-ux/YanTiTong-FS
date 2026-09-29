package com.ytt.modules.admin.controller;

import com.ytt.common.PageResult;
import com.ytt.common.Result;
import com.ytt.entity.Question;
import com.ytt.modules.admin.dto.QuestionQuery;
import com.ytt.modules.admin.dto.QuestionSaveRequest;
import com.ytt.modules.admin.service.AdminQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-题目管理接口
 */
@Tag(name = "管理端-题目管理", description = "题目分页查询、新增、编辑、删除")
@RestController
@RequestMapping("/admin/questions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    public AdminQuestionController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @Operation(summary = "题目分页列表")
    @GetMapping
    public Result<PageResult<Question>> page(QuestionQuery query) {
        return Result.success(adminQuestionService.page(query));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        return Result.success(adminQuestionService.detail(id));
    }

    @Operation(summary = "新增题目")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody QuestionSaveRequest request) {
        adminQuestionService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑题目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Validated @RequestBody QuestionSaveRequest request) {
        request.setId(id);
        adminQuestionService.update(request);
        return Result.success();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminQuestionService.delete(id);
        return Result.success();
    }
}
