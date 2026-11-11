package com.example.cyber_trap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edt1, edt2;
    private Button btn1, btn2;
    private TextView t1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

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
                Toast.makeText(LoginActivity.this, "Tên đăng nhập và mật khẩu không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            btn1.setEnabled(false);
            LoginRequest loginRequest = new LoginRequest(username, password);

            RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    btn1.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();

                        // Lưu token & username vào SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("CyberTrapPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        if (loginResponse.getAccessToken() != null) {
                            editor.putString("ACCESS_TOKEN", loginResponse.getAccessToken());
                        }
                        editor.putString("LOGGED_USER", username);
                        editor.apply();

                        String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Đăng nhập thành công!";
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();

                        // Chuyển sang Trang chủ
                        Intent intent = new Intent(LoginActivity.this, TrangChuActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Nhận thông báo lỗi từ Backend (401: Sai tài khoản hoặc mật khẩu)
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                JSONObject jsonObject = new JSONObject(errorJson);
                                String message = jsonObject.optString("message", "Sai tài khoản hoặc mật khẩu!");
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    btn1.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        // Bấm nút Sign up -> Mở màn hình Đăng ký
        btn2.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // Bấm Quên mật khẩu
        t1.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Chức năng khôi phục mật khẩu đang được phát triển. Vui lòng liên hệ ban quản trị!", Toast.LENGTH_LONG).show();
        });
    }
}
