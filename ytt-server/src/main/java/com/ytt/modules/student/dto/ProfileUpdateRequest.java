package com.ytt.modules.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 个人资料修改请求
 */
@Schema(description = "个人资料修改请求")
public class ProfileUpdateRequest {

    @Schema(description = "真实姓名/昵称", required = true)
    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名最长 20 个字符")
    private String realName;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
}
