package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码请求
 */
@Schema(description = "修改密码请求")
public class PasswordChangeRequest {

    @Schema(description = "原密码", required = true)
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码（6-20 位）", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为 6-20 位")
    private String newPassword;

    @Schema(description = "确认新密码", required = true)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
