package com.example.cyber_trap;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ScanResponse {

    @SerializedName(value = "isScam", alternate = {"is_scam", "isSuspicious"})
    private boolean isScam;

    @SerializedName(value = "riskLevel", alternate = {"risk_level"})
    private String riskLevel;   // "CRITICAL", "HIGH", "MEDIUM", "LOW"

    @SerializedName(value = "score", alternate = {"riskScore", "ruleScore", "final_score", "rule_score"})
    private int riskScore;      // 0 - 100

    @SerializedName(value = "explanation", alternate = {"message"})
    private String message;

    @SerializedName("category")
    private String category;

    @SerializedName("totalReports")
    private int totalReports;

    @SerializedName(value = "details", alternate = {"reasons", "reasonList"})
    private List<String> details;

    @SerializedName("blacklistMatch")
    private Boolean blacklistMatch;

    @SerializedName("scanId")
    private Integer scanId;

    @SerializedName("inputType")
    private String inputType;

    @SerializedName("inputValue")
    private String inputValue;

    @SerializedName("normalizedEmail")
    private String normalizedEmail;

    @SerializedName("normalizedPhone")
    private String normalizedPhone;

    @SerializedName("normalizedUrl")
    private String normalizedUrl;

    @SerializedName("hostname")
    private String hostname;

    @SerializedName("protocol")
    private String protocol;

    public ScanResponse() {
    }

    public boolean isScam() {
        return isScam;
    }

    public void setScam(boolean scam) {
        isScam = scam;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(int totalReports) {
        this.totalReports = totalReports;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Boolean getBlacklistMatch() {
        return blacklistMatch;
    }

    public void setBlacklistMatch(Boolean blacklistMatch) {
        this.blacklistMatch = blacklistMatch;
    }

    public Integer getScanId() {
        return scanId;
    }

    public void setScanId(Integer scanId) {
        this.scanId = scanId;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public void setNormalizedEmail(String normalizedEmail) {
        this.normalizedEmail = normalizedEmail;
    }

    public String getNormalizedPhone() {
        return normalizedPhone;
    }

    public void setNormalizedPhone(String normalizedPhone) {
        this.normalizedPhone = normalizedPhone;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
