package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 题目分页查询参数
 */
@Schema(description = "题目分页查询参数")
public class QuestionQuery {

    @Schema(description = "题库编码，如 ky408")
    private String bankCode;

    @Schema(description = "关键字（题干模糊匹配）")
    private String keyword;

    @Schema(description = "科目：ds/co/os/cn")
    private String subject;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "题型：single/multi/judge")
    private String type;

    @Schema(description = "难度：1 简单 2 中等 3 困难")
    private Integer difficulty;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNum = 1L;

    @Schema(description = "每页条数，最大 100", example = "10")
    private Long pageSize = 10L;

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public Long getPageNum() { return pageNum; }
    public void setPageNum(Long pageNum) { this.pageNum = pageNum; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
}
