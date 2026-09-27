package com.ytt.modules.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.entity.StudyPlan;
import com.ytt.mapper.StudyPlanMapper;
import com.ytt.modules.student.dto.PlanUpdateRequest;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 学员端-学习计划服务（每用户一条记录）
 */
@Service
public class PlanService {

    private static final int DEFAULT_DAILY_TARGET = 30;

    /** 默认考试日期：2027 届考研初试 */
    private static final LocalDate DEFAULT_EXAM_DATE = LocalDate.of(2026, 12, 19);

    private final StudyPlanMapper studyPlanMapper;

    public PlanService(StudyPlanMapper studyPlanMapper) {
        this.studyPlanMapper = studyPlanMapper;
    }

    /** 查询学习计划：不存在时创建默认计划 */
    public StudyPlan get() {
        Long userId = SecurityUtils.getUserId();
        StudyPlan plan = studyPlanMapper.selectOne(
                new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId));
        if (plan == null) {
            plan = new StudyPlan();
            plan.setUserId(userId);
            plan.setDailyTarget(DEFAULT_DAILY_TARGET);
            plan.setExamDate(DEFAULT_EXAM_DATE);
            studyPlanMapper.insert(plan);
        }
        return plan;
    }

    /** 更新学习计划（不存在则创建） */
    public StudyPlan update(PlanUpdateRequest request) {
        Long userId = SecurityUtils.getUserId();
        StudyPlan plan = studyPlanMapper.selectOne(
                new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId));
        if (plan == null) {
            plan = new StudyPlan();
            plan.setUserId(userId);
            plan.setDailyTarget(request.getDailyTarget());
            plan.setExamDate(request.getExamDate());
            studyPlanMapper.insert(plan);
        } else {
            plan.setDailyTarget(request.getDailyTarget());
            plan.setExamDate(request.getExamDate());
            studyPlanMapper.updateById(plan);
        }
        return plan;
    }
}
