package com.ytt.modules.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ytt.common.BizException;
import com.ytt.common.ErrorCode;
import com.ytt.entity.ExamBank;
import com.ytt.mapper.ExamBankMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库类别服务：题库元数据查询与有效性校验。
 * 后续新增考试大类只需在 exam_bank 表插入一行（含科目定义 JSON）即可生效，
 * 不必修改本组卷、刷题、统计等业务代码。
 */
@Service
public class BankService {

    /** 首个上线的题库：考研408 */
    public static final String DEFAULT_BANK_CODE = "ky408";

    private final ExamBankMapper examBankMapper;

    public BankService(ExamBankMapper examBankMapper) {
        this.examBankMapper = examBankMapper;
    }

    public List<ExamBank> listEnabled() {
        return examBankMapper.selectList(
                new LambdaQueryWrapper<ExamBank>()
                        .eq(ExamBank::getStatus, 1)
                        .orderByAsc(ExamBank::getSort)
                        .orderByAsc(ExamBank::getId));
    }

    /** 按编码查询题库（不限状态），不存在返回 null */
    public ExamBank getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return examBankMapper.selectOne(
                new LambdaQueryWrapper<ExamBank>().eq(ExamBank::getCode, code.trim()));
    }

    public String normalize(String code) {
        return (code == null || code.trim().isEmpty()) ? DEFAULT_BANK_CODE : code.trim();
    }

    /** 校验题库存在且已上线，否则抛参数错误 */
    public ExamBank requireEnabled(String code) {
        String normalized = normalize(code);
        ExamBank bank = getByCode(normalized);
        if (bank == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题库不存在：" + normalized);
        }
        if (bank.getStatus() == null || bank.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "题库尚未上线：" + bank.getName());
        }
        return bank;
    }

    /** 校验科目编码属于该题库的科目定义 */
    public void requireSubjectInBank(ExamBank bank, String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_MISSING, "科目不能为空");
        }
        Set<String> codes = bank.getSubjects().stream()
                .map(ExamBank.Subject::getCode).collect(Collectors.toSet());
        if (!codes.contains(subject.trim())) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "科目 " + subject + " 不属于题库「" + bank.getName() + "」");
        }
    }
}
