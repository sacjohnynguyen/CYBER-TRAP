package com.example.cyber_trap;

public class ReportItem {
    private int reportId;
    private Integer scamReportId;
    private String targetValue;
    private String targetType;
    private String scamCategory;
    private String description;
    private double lostAmount;
    private String status;
    private String createdAt;

    public ReportItem() {
    }

    public ReportItem(int reportId, String targetValue, String targetType, String scamCategory, String description, String status, String createdAt) {
        this.reportId = reportId;
        this.targetValue = targetValue;
        this.targetType = targetType;
        this.scamCategory = scamCategory;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getReportId() {
        return reportId;
    }

    public Integer getScamReportId() {
        return scamReportId;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getScamCategory() {
        return scamCategory;
    }

    public String getDescription() {
        return description;
    }

    public double getLostAmount() {
        return lostAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
