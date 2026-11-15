package com.example.cyber_trap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/v1/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest registerRequest);

    // Quét & Tra cứu lừa đảo (SĐT, Email, Link, Nội dung)
    @POST("api/v1/scan")
    Call<ScanResponseWrapper> scanScam(@Body ScanRequest scanRequest);

    // Gửi báo cáo lừa đảo
    @POST("api/v1/reports")
    Call<ReportPostResponse> createReport(
            @Header("Authorization") String authHeader,
            @Body ReportRequest reportRequest
    );

    // Lấy danh sách lịch sử báo cáo của người dùng theo ID
    @GET("api/v1/reports/user/{id}")
    Call<ReportListResponse> getUserReports(
            @Header("Authorization") String authHeader,
            @Path("id") int userId
    );
}
