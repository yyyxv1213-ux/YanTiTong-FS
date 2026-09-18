package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目表
 */
@TableName(value = "question", autoResultMap = true)
public class Question implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题型：single 单选 / multi 多选 / judge 判断 */
    private String type;

    /** 所属题库编码：ky408 考研408（后续可扩展软考、计算机二级等） */
    private String bankCode;

    /** 科目：ds 数据结构 / co 组成原理 / os 操作系统 / cn 计算机网络 */
    private String subject;

    /** 所属分类 */
    private Long categoryId;

    /** 章节 */
    private String chapter;

    /** 难度：1 简单 2 中等 3 困难 */
    private Integer difficulty;

    /** 题干（支持富文本 HTML） */
    private String stem;

    /** 选项列表（JSON 存储） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    /** 正确选项下标（JSON 数组，判断题 0=正确 1=错误） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> answer;

    /** 答案解析 */
    private String analysis;

    /** 来源 */
    private String source;

    /** 创建人 */
    private Long createBy;

    /** 乐观锁 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    /** 分类名称（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public List<Integer> getAnswer() { return answer; }
    public void setAnswer(List<Integer> answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
