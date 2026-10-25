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

public class signup extends AppCompatActivity {

    private EditText edtUsername, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Bấm nút Register -> Gọi API Backend Express
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String username = edtUsername != null ? edtUsername.getText().toString().trim() : "";
                String email = edtEmail != null ? edtEmail.getText().toString().trim() : "";
                String password = edtPassword != null ? edtPassword.getText().toString().trim() : "";
                String confirmPassword = edtConfirmPassword != null ? edtConfirmPassword.getText().toString().trim() : "";

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(signup.this, "Tên người dùng, email và mật khẩu không được để trống!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(signup.this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                    return;
                }

                RegisterRequest registerRequest = new RegisterRequest(username, email, password);

                RetrofitClient.getApiService().register(registerRequest).enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RegisterResponse regResponse = response.body();
                            Toast.makeText(signup.this, regResponse.getMessage(), Toast.LENGTH_SHORT).show();

                            // Đăng ký thành công -> Quay lại Đăng nhập
                            Intent intent = new Intent(signup.this, login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Đăng ký thất bại (409 Email trùng, 400 dữ liệu trống, v.v.)
                            try {
                                if (response.errorBody() != null) {
                                    String errorJson = response.errorBody().string();
                                    JSONObject jsonObject = new JSONObject(errorJson);
                                    String message = jsonObject.optString("message", "Đăng ký thất bại!");
                                    Toast.makeText(signup.this, message, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(signup.this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(signup.this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Toast.makeText(signup.this, "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        // Bấm "already have an account?" -> Quay lại Đăng nhập
        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> {
                Intent intent = new Intent(signup.this, login.class);
                startActivity(intent);
                finish();
            });
        }
    }
}