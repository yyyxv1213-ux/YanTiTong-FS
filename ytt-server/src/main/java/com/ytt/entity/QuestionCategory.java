package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 题目分类表
 */
@TableName("question_category")
public class QuestionCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题库编码 */
    private String bankCode;

    /** 所属科目 */
    private String subject;

    /** 分类名称 */
    private String name;

    /** 排序号 */
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
