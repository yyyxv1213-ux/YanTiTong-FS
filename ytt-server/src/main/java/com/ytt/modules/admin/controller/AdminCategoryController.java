package com.ytt.modules.admin.controller;

import com.ytt.common.Result;
import com.ytt.entity.QuestionCategory;
import com.ytt.modules.admin.dto.CategorySaveRequest;
import com.ytt.modules.admin.service.AdminQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-题目分类管理接口
 */
@Tag(name = "管理端-分类管理", description = "题目分类的查询、新增、编辑、删除")
@RestController
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminQuestionService adminQuestionService;

    public AdminCategoryController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @Operation(summary = "分类列表（可按题库、科目过滤）")
    @GetMapping
    public Result<List<QuestionCategory>> list(@RequestParam(required = false) String bankCode,
                                              @RequestParam(required = false) String subject) {
        return Result.success(adminQuestionService.listCategories(bankCode, subject));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CategorySaveRequest request) {
        adminQuestionService.createCategory(request);
        return Result.success();
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Validated @RequestBody CategorySaveRequest request) {
        request.setId(id);
        adminQuestionService.updateCategory(request);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminQuestionService.deleteCategory(id);
        return Result.success();
    }
}
