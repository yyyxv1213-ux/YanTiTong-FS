package com.ytt.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 管理员新增/编辑用户请求。
 * id 为空表示新增，非空表示编辑；
 * 密码新增时必填，编辑时留空表示不修改密码。
 */
@Schema(description = "管理员新增/编辑用户请求")
public class AdminUserSaveRequest {

    @Schema(description = "用户 ID，编辑时必传")
    private Long id;

    @Schema(description = "登录用户名（4-20 位字母数字下划线）", required = true)
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名须为 4-20 位字母、数字或下划线")
    private String username;

    @Schema(description = "邮箱（登录凭据，必填唯一）", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    @Schema(description = "真实姓名/昵称")
    @Size(max = 20, message = "姓名最长 20 个字符")
    private String realName;

    @Schema(description = "登录密码（新增必填，编辑留空表示不修改）")
    @Size(min = 6, max = 20, message = "密码长度须为 6-20 位")
    private String password;

    @Schema(description = "状态：1 启用 0 禁用", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "角色编码列表", required = true)
    @NotEmpty(message = "至少分配一个角色")
    private List<String> roleCodes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
