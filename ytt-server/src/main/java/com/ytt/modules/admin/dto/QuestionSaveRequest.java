package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 题目新增/编辑请求。
 * id 为空表示新增；题干支持富文本 HTML。
 */
@Schema(description = "题目新增/编辑请求")
public class QuestionSaveRequest {

    @Schema(description = "题目 ID，编辑时必传")
    private Long id;

    @Schema(description = "所属题库编码，如 ky408", required = true)
    @NotBlank(message = "题库编码不能为空")
    @Size(max = 32, message = "题库编码最长 32 个字符")
    private String bankCode;

    @Schema(description = "题型：single 单选 / multi 多选 / judge 判断", required = true)
    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "single|multi|judge", message = "题型只能为 single/multi/judge")
    private String type;

    @Schema(description = "科目编码，必须属于所选题库", required = true)
    @NotBlank(message = "科目不能为空")
    private String subject;

    @Schema(description = "所属分类 ID", required = true)
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Schema(description = "章节", required = true)
    @NotBlank(message = "章节不能为空")
    @Size(max = 50, message = "章节最长 50 个字符")
    private String chapter;

    @Schema(description = "难度：1 简单 2 中等 3 困难", required = true)
    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度范围为 1-3")
    @Max(value = 3, message = "难度范围为 1-3")
    private Integer difficulty;

    @Schema(description = "题干（支持富文本 HTML）", required = true)
    @NotBlank(message = "题干不能为空")
    private String stem;

    @Schema(description = "选项列表，至少 2 项", required = true)
    @NotNull(message = "选项不能为空")
    @Size(min = 2, message = "选项至少 2 项")
    private List<String> options;

    @Schema(description = "正确选项下标数组（判断题 0=正确 1=错误）", required = true)
    @NotNull(message = "答案不能为空")
    @Size(min = 1, message = "答案至少 1 项")
    private List<Integer> answer;

    @Schema(description = "答案解析")
    @Size(max = 2000, message = "解析最长 2000 个字符")
    private String analysis;

    @Schema(description = "来源")
    @Size(max = 50, message = "来源最长 50 个字符")
    private String source;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public List<Integer> getAnswer() { return answer; }
    public void setAnswer(List<Integer> answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
