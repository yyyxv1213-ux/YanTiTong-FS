package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷题记录表
 */
@TableName(value = "practice_record", autoResultMap = true)
public class PracticeRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 用户作答（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> userAnswer;

    /** 是否正确：1 正确 0 错误 */
    private Integer correct;

    /** 训练模式：sequence 顺序 / random 随机 / chapter 章节 / wrong 错题 */
    private String mode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public List<Integer> getUserAnswer() { return userAnswer; }
    public void setUserAnswer(List<Integer> userAnswer) { this.userAnswer = userAnswer; }
    public Integer getCorrect() { return correct; }
    public void setCorrect(Integer correct) { this.correct = correct; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
