package com.example.cyber_trap;

public class ReportRequest {
    private String targetValue;
    private String targetType;
    private String scamCategory;
    private String description;
    private double lostAmount;

    public ReportRequest(String targetValue, String targetType, String scamCategory, String description, double lostAmount) {
        this.targetValue = targetValue;
        this.targetType = targetType;
        this.scamCategory = scamCategory;
        this.description = description;
        this.lostAmount = lostAmount;
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
}
