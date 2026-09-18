package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytt.handler.SubjectListTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题库类别表（如考研408、软考、计算机二级等）
 */
@TableName(value = "exam_bank", autoResultMap = true)
public class ExamBank implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题库编码，如 ky408 */
    private String code;

    /** 题库名称 */
    private String name;

    /** 科目定义 JSON：[{code,name}] */
    @TableField(typeHandler = SubjectListTypeHandler.class)
    private List<Subject> subjects;

    /** 状态：1 已上线 0 未上线 */
    private Integer status;

    /** 排序号，越小越靠前 */
    private Integer sort;

    private LocalDateTime createTime;

    /** 题库内的科目定义 */
    public static class Subject implements Serializable {
        private String code;
        private String name;

        public Subject() {
        }

        public Subject(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Subject> getSubjects() { return subjects; }
    public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
