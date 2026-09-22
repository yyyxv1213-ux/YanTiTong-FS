package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.entity.WrongQuestion;
import com.ytt.mapper.WrongQuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 错题本维护组件：刷题与考试模块共用的错题记录逻辑
 */
@Service
public class WrongBookService {

    private final WrongQuestionMapper wrongQuestionMapper;

    public WrongBookService(WrongQuestionMapper wrongQuestionMapper) {
        this.wrongQuestionMapper = wrongQuestionMapper;
    }

    /**
     * 记录作答结果：
     * 答错 -> 错题本不存在则新建（次数=1），已存在则次数+1 并恢复为未掌握；
     * 答对 -> 若该题在错题本中且未掌握，则标记为已掌握。
     */
    // 错题记录从不物理删除：答对只改掌握标记，错误次数保留下来供错题页按轻重排序
    public void recordResult(Long userId, Long questionId, List<Integer> userAnswer, boolean correct) {
        WrongQuestion wrong = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId));
        if (!correct) {
            if (wrong == null) {
                wrong = new WrongQuestion();
                wrong.setUserId(userId);
                wrong.setQuestionId(questionId);
                wrong.setWrongCount(1);
                wrong.setLastAnswer(userAnswer);
                wrong.setMastered(0);
                wrongQuestionMapper.insert(wrong);
            } else {
                wrong.setWrongCount(wrong.getWrongCount() == null ? 1 : wrong.getWrongCount() + 1);
                wrong.setLastAnswer(userAnswer);
                wrong.setMastered(0);
                wrongQuestionMapper.updateById(wrong);
            }
        } else if (wrong != null && wrong.getMastered() != null && wrong.getMastered() == 0) {
            wrong.setMastered(1);
            wrong.setLastAnswer(userAnswer);
            wrongQuestionMapper.updateById(wrong);
        }
    }
}
