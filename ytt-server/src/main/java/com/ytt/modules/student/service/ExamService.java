package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.common.PageResult;
import com.ytt.entity.ExamDetail;
import com.ytt.entity.ExamRecord;
import com.ytt.entity.Question;
import com.ytt.mapper.ExamDetailMapper;
import com.ytt.mapper.ExamRecordMapper;
import com.ytt.mapper.QuestionMapper;
import com.ytt.entity.ExamBank;
import com.ytt.modules.common.service.BankService;
import com.ytt.modules.student.dto.ExamReportResponse;
import com.ytt.modules.student.dto.ExamStartRequest;
import com.ytt.modules.student.dto.ExamStartResponse;
import com.ytt.modules.student.dto.ExamSubmitRequest;
import com.ytt.modules.student.dto.ExamSubmitResponse;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 学员端-模拟考试服务
 */
@Service
public class ExamService {

    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamDetailMapper examDetailMapper;
    private final WrongBookService wrongBookService;
    private final BankService bankService;

    /**
     * 组卷会话暂存（examKey -> 会话）。
     * 单体应用使用内存实现；集群部署时需替换为 Redis 等共享存储。
     */
    // TODO 换 Redis：会话当前只在本机内存，重启或扩容后进行中的考试会丢失
    private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

    public ExamService(QuestionMapper questionMapper, ExamRecordMapper examRecordMapper,
                       ExamDetailMapper examDetailMapper, WrongBookService wrongBookService,
                       BankService bankService) {
        this.questionMapper = questionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examDetailMapper = examDetailMapper;
        this.wrongBookService = wrongBookService;
        this.bankService = bankService;
    }

    public ExamStartResponse start(ExamStartRequest request) {
        Long userId = SecurityUtils.getUserId();
        String bankCode = bankService.normalize(request.getBankCode());
        ExamBank bank = bankService.requireEnabled(bankCode);
        if (StringUtils.hasText(request.getSubject())) {
            bankService.requireSubjectInBank(bank, request.getSubject());
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getBankCode, bankCode)
                .eq(StringUtils.hasText(request.getSubject()), Question::getSubject, request.getSubject())
                .orderByAsc(Question::getId);
        List<Question> pool = questionMapper.selectList(wrapper);
        if (pool.size() < request.getCount()) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "可用题目不足 " + request.getCount() + " 道，当前仅 " + pool.size() + " 道");
        }
        Collections.shuffle(pool);
        List<Question> picked = new ArrayList<>(pool.subList(0, request.getCount()));

        // UUID 去掉连字符：只为了在 URL 和日志里更干净，无安全含义
        String examKey = UUID.randomUUID().toString().replace("-", "");
        // 交卷宽限 5 分钟，避免网络延迟导致压哨交卷失败
        long expireAt = System.currentTimeMillis() + (request.getMinutes() + 5L) * 60 * 1000;
        sessions.put(examKey, new ExamSession(userId,
                picked.stream().map(Question::getId).collect(Collectors.toList()), expireAt));

        picked.forEach(PracticeService::hideAnswer);
        ExamStartResponse result = new ExamStartResponse();
        result.setExamKey(examKey);
        result.setMinutes(request.getMinutes());
        result.setTotalCount(picked.size());
        result.setQuestions(picked);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmitResponse submit(ExamSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        ExamSession session = sessions.remove(request.getExamKey());
        if (session == null || !session.userId.equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "考试不存在或已交卷");
        }
        if (System.currentTimeMillis() > session.expireAt) {
            throw new BizException(ErrorCode.PARAM_ERROR, "考试已超时，成绩无效");
        }
        Map<Long, List<Integer>> answerMap = new HashMap<>();
        if (request.getAnswers() != null) {
            for (ExamSubmitRequest.ExamAnswerItem item : request.getAnswers()) {
                answerMap.put(item.getQuestionId(),
                        item.getUserAnswer() == null ? Collections.emptyList() : item.getUserAnswer());
            }
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(session.questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int total = session.questionIds.size();
        int correctCount = 0;
        List<ExamDetail> details = new ArrayList<>();
        for (Long questionId : session.questionIds) {
            Question question = questionMap.get(questionId);
            if (question == null) {
                continue;
            }
            List<Integer> userAnswer = answerMap.getOrDefault(questionId, Collections.emptyList());
            boolean correct = PracticeService.isCorrect(question.getAnswer(), userAnswer);
            if (correct) {
                correctCount++;
            }
            wrongBookService.recordResult(userId, questionId, userAnswer, correct);
            ExamDetail detail = new ExamDetail();
            detail.setQuestionId(questionId);
            detail.setUserAnswer(userAnswer);
            detail.setCorrect(correct ? 1 : 0);
            details.add(detail);
        }
        int score = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setTotalCount(total);
        record.setCorrectCount(correctCount);
        record.setScore(score);
        record.setDurationSeconds(request.getDurationSeconds());
        examRecordMapper.insert(record);
        for (ExamDetail detail : details) {
            detail.setExamId(record.getId());
            examDetailMapper.insert(detail);
        }

        ExamSubmitResponse result = new ExamSubmitResponse();
        result.setRecordId(record.getId());
        result.setScore(score);
        result.setCorrectCount(correctCount);
        result.setTotalCount(total);
        return result;
    }

    public PageResult<ExamRecord> history(Long pageNum, Long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<ExamRecord> page = examRecordMapper.selectPage(new Page<>(num, size),
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .orderByDesc(ExamRecord::getId));
        return PageResult.of(page);
    }

    /** 报告里的明细直接挂在 ExamDetail.question 上返回，前端拿到的结构不变 */
    public ExamReportResponse report(Long id) {
        Long userId = SecurityUtils.getUserId();
        ExamRecord record = examRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        List<ExamDetail> details = examDetailMapper.selectList(
                new LambdaQueryWrapper<ExamDetail>()
                        .eq(ExamDetail::getExamId, id)
                        .orderByAsc(ExamDetail::getId));
        if (!details.isEmpty()) {
            List<Long> questionIds = details.stream()
                    .map(ExamDetail::getQuestionId).collect(Collectors.toList());
            Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            for (ExamDetail detail : details) {
                detail.setQuestion(questionMap.get(detail.getQuestionId()));
            }
        }
        ExamReportResponse result = new ExamReportResponse();
        result.setRecord(record);
        result.setDetails(details);
        return result;
    }

    /** 组卷会话 */
    private static class ExamSession {
        private final Long userId;
        private final List<Long> questionIds;
        private final long expireAt;

        ExamSession(Long userId, List<Long> questionIds, long expireAt) {
            this.userId = userId;
            this.questionIds = questionIds;
            this.expireAt = expireAt;
        }
    }
}
