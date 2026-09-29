package com.ytt.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.common.PageResult;
import com.ytt.entity.ExamBank;
import com.ytt.entity.Question;
import com.ytt.entity.QuestionCategory;
import com.ytt.mapper.QuestionCategoryMapper;
import com.ytt.mapper.QuestionMapper;
import com.ytt.modules.admin.dto.CategorySaveRequest;
import com.ytt.modules.common.service.BankService;
import com.ytt.modules.admin.dto.QuestionQuery;
import com.ytt.modules.admin.dto.QuestionSaveRequest;
import com.ytt.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员-题目与分类管理服务
 */
@Service
public class AdminQuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final BankService bankService;

    public AdminQuestionService(QuestionMapper questionMapper, QuestionCategoryMapper categoryMapper,
                               BankService bankService) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.bankService = bankService;
    }

    // ==================== 题目管理 ====================

    /** 分页查询题目（关键字/科目/分类/题型/难度筛选），并组装分类名称 */
    public PageResult<Question> page(QuestionQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Question::getStem, query.getKeyword().trim());
        }
        wrapper.eq(StringUtils.hasText(query.getBankCode()), Question::getBankCode, query.getBankCode())
                .eq(StringUtils.hasText(query.getSubject()), Question::getSubject, query.getSubject())
                .eq(query.getCategoryId() != null, Question::getCategoryId, query.getCategoryId())
                .eq(StringUtils.hasText(query.getType()), Question::getType, query.getType())
                .eq(query.getDifficulty() != null, Question::getDifficulty, query.getDifficulty())
                .orderByDesc(Question::getId);
        Page<Question> page = questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillCategoryName(page.getRecords());
        return PageResult.of(page);
    }

    /** 题目详情 */
    public Question detail(Long id) {
        Question question = getOrThrow(id);
        fillCategoryName(java.util.Collections.singletonList(question));
        return question;
    }

    /** 新增题目 */
    public void create(QuestionSaveRequest request) {
        validateRequest(request);
        Question question = new Question();
        applyRequest(question, request);
        question.setCreateBy(SecurityUtils.getUserId());
        questionMapper.insert(question);
    }

    /** 编辑题目 */
    public void update(QuestionSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "缺少题目 ID");
        }
        Question question = getOrThrow(request.getId());
        validateRequest(request);
        applyRequest(question, request);
        questionMapper.updateById(question);
    }

    /** 删除题目（逻辑删除） */
    public void delete(Long id) {
        getOrThrow(id);
        questionMapper.deleteById(id);
    }

    // ==================== 分类管理 ====================

    /** 分类列表（可按题库、科目过滤，按排序号升序） */
    public List<QuestionCategory> listCategories(String bankCode, String subject) {
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(bankCode), QuestionCategory::getBankCode, bankCode)
                .eq(StringUtils.hasText(subject), QuestionCategory::getSubject, subject)
                .orderByAsc(QuestionCategory::getSort)
                .orderByAsc(QuestionCategory::getId);
        return categoryMapper.selectList(wrapper);
    }

    /** 新增分类 */
    public void createCategory(CategorySaveRequest request) {
        ExamBank bank = bankService.requireEnabled(request.getBankCode());
        bankService.requireSubjectInBank(bank, request.getSubject());
        QuestionCategory category = new QuestionCategory();
        category.setBankCode(bank.getCode());
        category.setSubject(request.getSubject().trim());
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        categoryMapper.insert(category);
    }

    /** 编辑分类 */
    public void updateCategory(CategorySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "缺少分类 ID");
        }
        QuestionCategory category = categoryMapper.selectById(request.getId());
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        ExamBank bank = bankService.requireEnabled(request.getBankCode());
        bankService.requireSubjectInBank(bank, request.getSubject());
        category.setBankCode(bank.getCode());
        category.setSubject(request.getSubject().trim());
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? category.getSort() : request.getSort());
        categoryMapper.updateById(category);
    }

    /** 删除分类：分类下存在题目时拒绝删除，避免产生悬空引用 */
    public void deleteCategory(Long id) {
        QuestionCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Long count = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getCategoryId, id));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该分类下存在 " + count + " 道题目，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    // ==================== 内部方法 ====================

    private Question getOrThrow(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return question;
    }

    /** 业务校验：题库已上线、科目属于该题库、分类存在且题库科目均匹配、答案下标不越界 */
    private void validateRequest(QuestionSaveRequest request) {
        ExamBank bank = bankService.requireEnabled(request.getBankCode());
        bankService.requireSubjectInBank(bank, request.getSubject());
        QuestionCategory category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (!bank.getCode().equals(category.getBankCode())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "所选分类不属于该题库");
        }
        if (!category.getSubject().equals(request.getSubject().trim())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "所选分类与科目不匹配");
        }
        int optionCount = request.getOptions().size();
        for (Integer index : request.getAnswer()) {
            if (index == null || index < 0 || index >= optionCount) {
                throw new BizException(ErrorCode.PARAM_ERROR, "答案下标超出选项范围");
            }
        }
        if ("single".equals(request.getType()) && request.getAnswer().size() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单选题答案只能有一项");
        }
        if ("multi".equals(request.getType()) && request.getAnswer().size() < 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "多选题答案至少两项");
        }
        if ("judge".equals(request.getType()) && request.getOptions().size() != 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "判断题选项必须为 2 项");
        }
    }

    private void applyRequest(Question question, QuestionSaveRequest request) {
        question.setBankCode(bankService.normalize(request.getBankCode()));
        question.setType(request.getType());
        question.setSubject(request.getSubject().trim());
        question.setCategoryId(request.getCategoryId());
        question.setChapter(request.getChapter());
        question.setDifficulty(request.getDifficulty());
        question.setStem(request.getStem());
        question.setOptions(request.getOptions());
        question.setAnswer(request.getAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setSource(StringUtils.hasText(request.getSource()) ? request.getSource() : "管理员录入");
    }

    /** 批量填充分类名称，避免 N+1 查询 */
    private void fillCategoryName(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<Long> categoryIds = questions.stream()
                .map(Question::getCategoryId).filter(id -> id != null)
                .distinct().collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName));
        for (Question question : questions) {
            question.setCategoryName(nameMap.get(question.getCategoryId()));
        }
    }
}
