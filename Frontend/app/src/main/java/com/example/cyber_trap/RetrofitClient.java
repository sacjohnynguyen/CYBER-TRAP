package com.example.cyber_trap;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Khi dùng Android Emulator: 10.0.2.2 đại diện cho localhost của máy tính (Backend VS Code)
    // Nếu test bằng máy thật Android qua Wi-Fi: Thay 10.0.2.2 bằng địa chỉ IPv4 máy tính (ví dụ: 192.168.1.x)
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit;

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
