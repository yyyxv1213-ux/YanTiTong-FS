package com.ytt.modules.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送邮箱验证码请求
 */
public class EmailCodeSendRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
