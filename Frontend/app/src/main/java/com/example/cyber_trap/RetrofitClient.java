package com.example.cyber_trap;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Lưu ý kết nối mạng:
    // 1. Android Emulator (Máy ảo): Dùng "http://10.0.2.2:3000/" (10.0.2.2 trỏ về localhost máy tính)
    // 2. Thiết bị thật (Real Phone qua Wi-Fi): Thay bằng IPv4 của máy tính (ví dụ: "http://192.168.1.15:3000/")
    private static String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit;

    public static void setBaseUrl(String newBaseUrl) {
        if (newBaseUrl != null && !newBaseUrl.trim().isEmpty()) {
            if (!newBaseUrl.endsWith("/")) {
                newBaseUrl += "/";
            }
            BASE_URL = newBaseUrl;
            retrofit = null; // Reset để tạo lại instance với URL mới
        }
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
