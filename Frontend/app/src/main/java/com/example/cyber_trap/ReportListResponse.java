package com.example.cyber_trap;

import java.util.List;

public class ReportListResponse {
    private boolean success;
    private String message;
    private int trustScore;
    private List<ReportItem> data;

    public ReportListResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public List<ReportItem> getData() {
        return data;
    }
}
