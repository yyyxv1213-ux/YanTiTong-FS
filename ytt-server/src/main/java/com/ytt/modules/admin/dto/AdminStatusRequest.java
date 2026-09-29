package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 启用/禁用用户请求
 */
@Schema(description = "启用/禁用用户请求")
public class AdminStatusRequest {

    @Schema(description = "状态：1 启用 0 禁用", required = true)
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
