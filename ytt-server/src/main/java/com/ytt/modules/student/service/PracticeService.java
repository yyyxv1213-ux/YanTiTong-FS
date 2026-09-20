package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.entity.PracticeRecord;
import com.ytt.entity.Question;
import com.ytt.entity.WrongQuestion;
import com.ytt.mapper.PracticeRecordMapper;
import com.ytt.mapper.QuestionMapper;
import com.ytt.mapper.WrongQuestionMapper;
import com.ytt.modules.common.service.BankService;
import com.ytt.modules.student.dto.PracticeSubmitRequest;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员端-刷题训练服务
 */
@Service
public class PracticeService {

    private final QuestionMapper questionMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final WrongBookService wrongBookService;
    private final BankService bankService;

    public PracticeService(QuestionMapper questionMapper, PracticeRecordMapper practiceRecordMapper,
                           WrongQuestionMapper wrongQuestionMapper, WrongBookService wrongBookService,
                           BankService bankService) {
        this.questionMapper = questionMapper;
        this.practiceRecordMapper = practiceRecordMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.wrongBookService = wrongBookService;
        this.bankService = bankService;
    }

    /**
     * 抽取训练题目（不返回答案与解析）。
     * mode=wrong 时从当前用户错题本抽取未掌握题目；random 时随机排序。
     * bankCode 用于在不同题库大类之间隔离题目，空值取默认题库 ky408。
     */
    public List<Question> listQuestions(String bankCode, String subject, String chapter, String type,
                                        Integer difficulty, String mode, Integer limit) {
        String bank = bankService.normalize(bankCode);
        bankService.requireEnabled(bank);
        int lim = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
        List<Question> questions;
        if ("wrong".equals(mode)) {
            // 错题本可能混有不同题库的错题，这里按当前题库（及科目）过滤
            questions = listWrongQuestions(lim).stream()
                    .filter(q -> bank.equals(q.getBankCode()))
                    .filter(q -> !StringUtils.hasText(subject) || subject.equals(q.getSubject()))
                    .collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Question::getBankCode, bank)
                    .eq(StringUtils.hasText(subject), Question::getSubject, subject)
                    .eq(StringUtils.hasText(chapter), Question::getChapter, chapter)
                    .eq(StringUtils.hasText(type), Question::getType, type)
                    .eq(difficulty != null, Question::getDifficulty, difficulty)
                    .orderByAsc(Question::getId);
            questions = questionMapper.selectList(wrapper);
            if ("random".equals(mode)) {
                Collections.shuffle(questions);
            }
            if (questions.size() > lim) {
                questions = new ArrayList<>(questions.subList(0, lim));
            }
        }
        questions.forEach(PracticeService::hideAnswer);
        return questions;
    }

    /** 提交作答：判分 -> 写刷题记录 -> 维护错题本，事务保证一致性 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(PracticeSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        Question question = questionMapper.selectById(request.getQuestionId());
        if (question == null) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }
        boolean correct = isCorrect(question.getAnswer(), request.getUserAnswer());

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setUserAnswer(request.getUserAnswer());
        record.setCorrect(correct ? 1 : 0);
        record.setMode(StringUtils.hasText(request.getMode()) ? request.getMode() : "sequence");
        practiceRecordMapper.insert(record);

        wrongBookService.recordResult(userId, question.getId(), request.getUserAnswer(), correct);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("answer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        return result;
    }

    /** 判分：作答下标集合与答案集合完全一致才判对（空作答判错） */
    public static boolean isCorrect(List<Integer> answer, List<Integer> userAnswer) {
        if (answer == null || userAnswer == null || userAnswer.isEmpty()) {
            return false;
        }
        // 多选题作答顺序无关，用 Set 比较而不是逐位对齐
        return new HashSet<>(answer).equals(new HashSet<>(userAnswer));
    }

    /** 隐藏答案与解析，防止前端作弊 */
    public static void hideAnswer(Question question) {
        question.setAnswer(null);
        question.setAnalysis(null);
    }

    /** 错题本题目：按最近错误时间倒序 */
    private List<Question> listWrongQuestions(int limit) {
        Long userId = SecurityUtils.getUserId();
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getMastered, 0)
                        .orderByDesc(WrongQuestion::getUpdateTime));
        if (wrongList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> questionIds = wrongList.stream()
                .map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        List<Question> questions = new ArrayList<>();
        for (WrongQuestion wrong : wrongList) {
            Question question = questionMap.get(wrong.getQuestionId());
            if (question != null) {
                questions.add(question);
            }
            if (questions.size() >= limit) {
                break;
            }
        }
        return questions;
    }
}
