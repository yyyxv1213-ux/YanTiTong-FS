package com.ytt.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.io.Serializable;
import java.util.List;

/**
 * 考试明细表（每题一条）
 */
@TableName(value = "exam_detail", autoResultMap = true)
public class ExamDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long questionId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> userAnswer;

    private Integer correct;

    /** 关联题目（非数据库字段） */
    @TableField(exist = false)
    private Question question;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public List<Integer> getUserAnswer() { return userAnswer; }
    public void setUserAnswer(List<Integer> userAnswer) { this.userAnswer = userAnswer; }
    public Integer getCorrect() { return correct; }
    public void setCorrect(Integer correct) { this.correct = correct; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
