package com.example.cyber_trap;

public class ReportPostResponse {
    private boolean success;
    private String message;
    private ReportItem data;

    public ReportPostResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public ReportItem getData() {
        return data;
    }
}
