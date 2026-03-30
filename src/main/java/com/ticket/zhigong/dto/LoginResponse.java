package com.ticket.zhigong.dto;

import com.ticket.zhigong.enums.UserRole;

public class LoginResponse {

    private String token;
    private long expiresIn;
    private Long userId;
    private String name;
    private UserRole role;

    public LoginResponse(String token, long expiresIn, Long userId, String name, UserRole role) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public String getToken() { return token; }
    public long getExpiresIn() { return expiresIn; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
}
