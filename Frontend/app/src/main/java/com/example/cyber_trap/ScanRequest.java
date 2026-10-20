package com.example.cyber_trap;

import com.google.gson.annotations.SerializedName;

public class ScanRequest {

    @SerializedName(value = "inputType", alternate = {"type"})
    private String inputType;   // "PHONE", "EMAIL", "URL", "CONTENT"

    @SerializedName(value = "inputValue", alternate = {"target", "value"})
    private String inputValue;  // Số điện thoại, email, link, hoặc nội dung

    public ScanRequest() {
    }

    public ScanRequest(String inputType, String inputValue) {
        this.inputType = inputType;
        this.inputValue = inputValue;
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
}
