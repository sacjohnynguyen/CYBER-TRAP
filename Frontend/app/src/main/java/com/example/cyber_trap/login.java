package com.example.cyber_trap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class login extends AppCompatActivity {
    private EditText edt1, edt2;
    private Button btn1, btn2;
    private TextView t1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        edt1 = findViewById(R.id.edt1);
        edt2 = findViewById(R.id.edt2);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        t1 = findViewById(R.id.t1);

        // Bấm nút Login -> Gọi API Backend Express
        btn1.setOnClickListener(v -> {
            String username = edt1.getText().toString().trim();
            String password = edt2.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(login.this, "Tên đăng nhập và mật khẩu không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest loginRequest = new LoginRequest(username, password);

            RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        Toast.makeText(login.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();

                        // Chuyển sang Trang chủ
                        Intent intent = new Intent(login.this, trangchu.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Nhận thông báo lỗi từ Backend (401: Sai tài khoản hoặc mật khẩu)
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                JSONObject jsonObject = new JSONObject(errorJson);
                                String message = jsonObject.optString("message", "Đăng nhập thất bại!");
                                Toast.makeText(login.this, message, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(login.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(login.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(login.this, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        // Bấm nút Sign up -> Mở màn hình Đăng ký
        btn2.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, signup.class);
            startActivity(intent);
        });

        // Bấm Quên mật khẩu
        t1.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, trangchu.class);
            startActivity(intent);
        });
    }
}