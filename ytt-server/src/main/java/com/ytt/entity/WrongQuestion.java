package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本表（每用户每题一条，唯一索引 user_id+question_id）
 */
@TableName(value = "wrong_question", autoResultMap = true)
public class WrongQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 最近一次作答（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> lastAnswer;

    /** 是否已掌握：1 已掌握（移出错题本展示） 0 未掌握 */
    private Integer mastered;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 关联题目（非数据库字段） */
    @TableField(exist = false)
    private Question question;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public List<Integer> getLastAnswer() { return lastAnswer; }
    public void setLastAnswer(List<Integer> lastAnswer) { this.lastAnswer = lastAnswer; }
    public Integer getMastered() { return mastered; }
    public void setMastered(Integer mastered) { this.mastered = mastered; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
