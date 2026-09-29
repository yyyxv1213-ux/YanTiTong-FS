package com.ytt.modules.admin.controller;

import com.ytt.common.PageResult;
import com.ytt.common.Result;
import com.ytt.entity.SysRole;
import com.ytt.entity.SysUser;
import com.ytt.modules.admin.dto.AdminPasswordRequest;
import com.ytt.modules.admin.dto.AdminStatusRequest;
import com.ytt.modules.admin.dto.AdminUserQuery;
import com.ytt.modules.admin.dto.AdminUserSaveRequest;
import com.ytt.modules.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-用户账号管理接口
 */
@Tag(name = "管理端-用户管理", description = "用户列表、新增、编辑、启禁用、重置密码、角色分配")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户分页列表")
    @GetMapping
    public Result<PageResult<SysUser>> page(AdminUserQuery query) {
        return Result.success(adminUserService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.success(adminUserService.detail(id));
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(adminUserService.allRoles());
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody AdminUserSaveRequest request) {
        adminUserService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Validated @RequestBody AdminUserSaveRequest request) {
        request.setId(id);
        adminUserService.update(request);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Validated @RequestBody AdminStatusRequest request) {
        adminUserService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Validated @RequestBody AdminPasswordRequest request) {
        adminUserService.resetPassword(id, request.getPassword());
        return Result.success();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @Validated @RequestBody AdminUserSaveRequest request) {
        adminUserService.assignRoles(id, request.getRoleCodes());
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.success();
    }
}
