package com.example.cyber_trap;

public class LoginResponse {

    private String message;
    private String accessToken;
    private String refreshToken;

    public LoginResponse() {
    }

    public String getMessage() {
        return message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}