package com.ytt.modules.student.controller;

import com.ytt.common.Result;
import com.ytt.entity.SysUser;
import com.ytt.modules.student.dto.PasswordChangeRequest;
import com.ytt.modules.student.dto.ProfileUpdateRequest;
import com.ytt.modules.student.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 学员端-个人中心接口
 */
@Tag(name = "学员端-个人中心", description = "资料查询修改与密码修改")
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "查询个人信息")
    @GetMapping
    public Result<SysUser> get() {
        return Result.success(profileService.get());
    }

    @Operation(summary = "修改个人资料")
    @PutMapping
    public Result<SysUser> update(@Validated @RequestBody ProfileUpdateRequest request) {
        return Result.success(profileService.update(request));
    }

    @Operation(summary = "修改密码（成功后需重新登录）")
    @PutMapping("/password")
    public Result<Void> changePassword(@Validated @RequestBody PasswordChangeRequest request,
                                       HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        String accessToken = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        profileService.changePassword(request, accessToken);
        return Result.success();
    }
}
