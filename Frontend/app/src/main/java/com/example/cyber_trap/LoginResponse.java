package com.example.cyber_trap;

public class LoginResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
    private UserData user;

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

    public UserData getUser() {
        return user;
    }

    public static class UserData {
        private int id;
        private String username;
        private String email;

        public UserData() {
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }
    }
}