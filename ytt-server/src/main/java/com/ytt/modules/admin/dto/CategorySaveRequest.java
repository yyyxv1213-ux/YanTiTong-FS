package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 题目分类新增/编辑请求
 */
@Schema(description = "题目分类新增/编辑请求")
public class CategorySaveRequest {

    @Schema(description = "分类 ID，编辑时必传")
    private Long id;

    @Schema(description = "所属题库编码，如 ky408", required = true)
    @NotBlank(message = "题库编码不能为空")
    @Size(max = 32, message = "题库编码最长 32 个字符")
    private String bankCode;

    @Schema(description = "所属科目编码，必须属于所选题库", required = true)
    @NotBlank(message = "科目不能为空")
    private String subject;

    @Schema(description = "分类名称", required = true)
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称最长 30 个字符")
    private String name;

    @Schema(description = "排序号，越小越靠前")
    private Integer sort;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
