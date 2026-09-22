package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.entity.Question;
import com.ytt.entity.WrongQuestion;
import com.ytt.mapper.QuestionMapper;
import com.ytt.mapper.WrongQuestionMapper;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员端-错题本服务
 */
@Service
public class WrongService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;

    public WrongService(WrongQuestionMapper wrongQuestionMapper, QuestionMapper questionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.questionMapper = questionMapper;
    }

    /** 错题列表（未掌握，含完整题面与解析，按最近错误时间倒序） */
    public List<WrongQuestion> list() {
        Long userId = SecurityUtils.getUserId();
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getMastered, 0)
                        .orderByDesc(WrongQuestion::getUpdateTime));
        if (wrongList.isEmpty()) {
            return wrongList;
        }
        List<Long> questionIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        for (WrongQuestion wrong : wrongList) {
            wrong.setQuestion(questionMap.get(wrong.getQuestionId()));
        }
        return wrongList;
    }

    /** 标记已掌握：仅可操作本人的错题记录 */
    public void master(Long id) {
        Long userId = SecurityUtils.getUserId();
        WrongQuestion wrong = wrongQuestionMapper.selectById(id);
        if (wrong == null) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        if (!wrong.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        wrong.setMastered(1);
        wrongQuestionMapper.updateById(wrong);
    }
}
