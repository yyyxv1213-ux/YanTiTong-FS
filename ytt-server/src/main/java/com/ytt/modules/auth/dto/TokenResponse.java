package com.ytt.modules.auth.dto;

import java.util.List;

/**
 * 登录/刷新令牌响应
 */
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    /** access token 有效期（秒） */
    private Long expiresIn;
    private UserInfo user;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    /** 登录用户基本信息 */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String realName;
        private List<String> roles;

        public UserInfo() {
        }

        public UserInfo(Long id, String username, String email, String realName, List<String> roles) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.realName = realName;
            this.roles = roles;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }
}
