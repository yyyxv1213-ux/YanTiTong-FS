package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.entity.ExamBank;
import com.ytt.entity.PracticeRecord;
import com.ytt.entity.Question;
import com.ytt.entity.StudyPlan;
import com.ytt.mapper.PracticeRecordMapper;
import com.ytt.mapper.QuestionMapper;
import com.ytt.mapper.StudyPlanMapper;
import com.ytt.modules.common.service.BankService;
import com.ytt.modules.student.dto.StatsOverviewResponse;
import com.ytt.modules.student.dto.SubjectStatItem;
import com.ytt.modules.student.dto.TrendItem;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员端-学习统计服务。
 * 所有统计均按题库大类隔离：切换到其他考试大类后，只统计该题库下题目的作答记录。
 */
@Service
public class StatsService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final QuestionMapper questionMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final BankService bankService;

    public StatsService(PracticeRecordMapper practiceRecordMapper,
                        QuestionMapper questionMapper, StudyPlanMapper studyPlanMapper,
                        BankService bankService) {
        this.practiceRecordMapper = practiceRecordMapper;
        this.questionMapper = questionMapper;
        this.studyPlanMapper = studyPlanMapper;
        this.bankService = bankService;
    }

    public StatsOverviewResponse overview(String bankCode) {
        Long userId = SecurityUtils.getUserId();
        String bank = bankService.normalize(bankCode);
        bankService.requireEnabled(bank);
        List<Long> questionIds = bankQuestionIds(bank);

        Long total = 0L;
        Long correct = 0L;
        Long today = 0L;
        if (!questionIds.isEmpty()) {
            total = practiceRecordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .in(PracticeRecord::getQuestionId, questionIds));
            correct = practiceRecordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .eq(PracticeRecord::getCorrect, 1)
                            .in(PracticeRecord::getQuestionId, questionIds));
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            today = practiceRecordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .ge(PracticeRecord::getCreateTime, todayStart)
                            .in(PracticeRecord::getQuestionId, questionIds));
        }

        StudyPlan plan = studyPlanMapper.selectOne(
                new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId));
        int dailyTarget = plan != null && plan.getDailyTarget() != null ? plan.getDailyTarget() : 30;
        LocalDate examDate = plan != null && plan.getExamDate() != null
                ? plan.getExamDate() : LocalDate.of(2026, 12, 19);
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), examDate);

        StatsOverviewResponse result = new StatsOverviewResponse();
        result.setTodayCount(today);
        result.setTotalCount(total);
        result.setCorrectCount(correct);
        // 百分数只保留一位小数，纯展示用途；先乘 1000 取整再除 10，避免浮点直接相除的尾差
        result.setAccuracy(total > 0 ? Math.round(1000.0 * correct / total) / 10.0 : 0.0);
        result.setDailyTarget(dailyTarget);
        result.setExamDate(examDate.toString());
        result.setDaysLeft(Math.max(daysLeft, 0));
        return result;
    }

    /** 按题库定义的科目固定返回，无数据的科目补零，保证前端图表横轴稳定 */
    public List<SubjectStatItem> subjectStats(String bankCode) {
        Long userId = SecurityUtils.getUserId();
        String bank = bankService.normalize(bankCode);
        ExamBank examBank = bankService.requireEnabled(bank);

        Map<String, long[]> grouped = new LinkedHashMap<>();
        for (ExamBank.Subject subject : examBank.getSubjects()) {
            grouped.put(subject.getCode(), new long[]{0L, 0L});
        }

        List<Long> questionIds = bankQuestionIds(bank);
        if (!questionIds.isEmpty()) {
            List<PracticeRecord> records = practiceRecordMapper.selectList(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .in(PracticeRecord::getQuestionId, questionIds)
                            .select(PracticeRecord::getQuestionId, PracticeRecord::getCorrect));
            Map<Long, String> subjectMap = questionMapper.selectBatchIds(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, Question::getSubject));
            for (PracticeRecord record : records) {
                String subject = subjectMap.get(record.getQuestionId());
                long[] counter = grouped.get(subject);
                if (counter != null) {
                    counter[0]++;
                    if (record.getCorrect() != null && record.getCorrect() == 1) {
                        counter[1]++;
                    }
                }
            }
        }
        List<SubjectStatItem> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : grouped.entrySet()) {
            SubjectStatItem item = new SubjectStatItem();
            item.setSubject(entry.getKey());
            item.setTotal(entry.getValue()[0]);
            item.setCorrect(entry.getValue()[1]);
            item.setAccuracy(entry.getValue()[0] > 0
                    ? Math.round(1000.0 * entry.getValue()[1] / entry.getValue()[0]) / 10.0 : 0.0);
            result.add(item);
        }
        return result;
    }

    /** 无数据的日期补零，前端折线图不需要再处理断点 */
    public List<TrendItem> trend(String bankCode) {
        Long userId = SecurityUtils.getUserId();
        String bank = bankService.normalize(bankCode);
        bankService.requireEnabled(bank);
        List<Long> questionIds = bankQuestionIds(bank);
        LocalDate startDate = LocalDate.now().minusDays(13);

        List<PracticeRecord> records = questionIds.isEmpty() ? java.util.Collections.emptyList()
                : practiceRecordMapper.selectList(
                    new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .in(PracticeRecord::getQuestionId, questionIds)
                        .ge(PracticeRecord::getCreateTime, startDate.atStartOfDay())
                        .select(PracticeRecord::getCorrect, PracticeRecord::getCreateTime));
        Map<LocalDate, long[]> grouped = new HashMap<>();
        for (PracticeRecord record : records) {
            if (record.getCreateTime() == null) {
                continue;
            }
            LocalDate date = record.getCreateTime().toLocalDate();
            long[] counter = grouped.computeIfAbsent(date, d -> new long[]{0L, 0L});
            counter[0]++;
            if (record.getCorrect() != null && record.getCorrect() == 1) {
                counter[1]++;
            }
        }
        List<TrendItem> result = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate date = startDate.plusDays(i);
            long[] counter = grouped.getOrDefault(date, new long[]{0L, 0L});
            TrendItem item = new TrendItem();
            item.setDate(date.toString());
            item.setTotal(counter[0]);
            item.setCorrect(counter[1]);
            result.add(item);
        }
        return result;
    }

    private List<Long> bankQuestionIds(String bank) {
        return questionMapper.selectList(
                        new LambdaQueryWrapper<Question>()
                                .eq(Question::getBankCode, bank)
                                .select(Question::getId)).stream()
                .map(Question::getId).collect(Collectors.toList());
    }
}
