package com.example.cyber_trap;

import com.google.gson.annotations.SerializedName;

public class ScanResponseWrapper {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ScanResponse data;

    public ScanResponseWrapper() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ScanResponse getData() {
        return data;
    }

    public void setData(ScanResponse data) {
        this.data = data;
    }
}
