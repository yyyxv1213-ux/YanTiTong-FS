package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户分页查询参数
 */
@Schema(description = "用户分页查询参数")
public class AdminUserQuery {

    @Schema(description = "关键字（用户名/姓名模糊匹配）")
    private String keyword;

    @Schema(description = "状态：1 启用 0 禁用")
    private Integer status;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，最大 100", example = "10")
    private Long pageSize = 10L;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getPageNum() { return pageNum; }
    public void setPageNum(Long pageNum) { this.pageNum = pageNum; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
}
