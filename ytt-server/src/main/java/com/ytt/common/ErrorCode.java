package com.ytt.common;

/**
 * 统一错误码体系：
 * 0     成功
 * 400xx 参数/客户端错误
 * 401xx 认证错误
 * 403xx 权限错误
 * 404xx 资源不存在
 * 409xx 业务冲突
 * 429xx 请求频率超限
 * 500xx 系统错误
 */
public enum ErrorCode {

    SUCCESS(0, "操作成功"),

    /* 400xx 参数错误 */
    PARAM_ERROR(40000, "请求参数错误"),
    PARAM_MISSING(40001, "缺少必要参数"),
    PARAM_TYPE_ERROR(40002, "参数类型不匹配"),

    /* 401xx 认证错误 */
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),
    TOKEN_BLACKLISTED(40103, "Token 已被注销"),
    LOGIN_FAILED(40104, "用户名或密码错误"),
    ACCOUNT_DISABLED(40105, "账号已被禁用，请联系管理员"),
    EMAIL_CODE_ERROR(40106, "邮箱验证码错误"),
    EMAIL_CODE_EXPIRED(40107, "验证码已过期，请重新获取"),

    /* 403xx 权限错误 */
    FORBIDDEN(40300, "无权限访问该资源"),
    ROLE_DENIED(40301, "当前角色无权执行该操作"),

    /* 404xx 资源不存在 */
    NOT_FOUND(40400, "请求的资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    QUESTION_NOT_FOUND(40402, "题目不存在"),
    CATEGORY_NOT_FOUND(40403, "分类不存在"),
    RECORD_NOT_FOUND(40404, "记录不存在"),

    /* 409xx 业务冲突 */
    USERNAME_EXISTS(40901, "用户名已被占用"),
    OPTIMISTIC_LOCK_FAIL(40902, "数据已被他人修改，请刷新后重试"),
    CANNOT_DISABLE_SELF(40903, "不能禁用当前登录的管理员账号"),
    CANNOT_DELETE_ADMIN(40904, "不能删除内置管理员账号"),
    EMAIL_EXISTS(40905, "邮箱已被注册"),

    /* 429xx 限流 */
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),
    EMAIL_CODE_TOO_FREQUENT(42901, "验证码发送过于频繁，请 1 分钟后再试"),

    /* 500xx 系统错误 */
    SYSTEM_ERROR(50000, "系统内部错误"),
    DATABASE_ERROR(50001, "数据库操作失败"),
    MAIL_SEND_FAILED(50002, "验证码邮件发送失败，请稍后重试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
