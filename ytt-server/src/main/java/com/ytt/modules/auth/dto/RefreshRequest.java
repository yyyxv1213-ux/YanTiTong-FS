package com.ytt.modules.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * 刷新令牌请求
 */
public class RefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
