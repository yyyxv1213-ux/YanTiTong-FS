package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 管理员重置用户密码请求
 */
@Schema(description = "管理员重置用户密码请求")
public class AdminPasswordRequest {

    @Schema(description = "新密码（6-20 位）", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度须为 6-20 位")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
