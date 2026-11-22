package com.example.cyber_trap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitials, tvUserName, tvUserEmail, tvStatPosts, tvStatReports;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvStatPosts = findViewById(R.id.tvStatPosts);
        tvStatReports = findViewById(R.id.tvStatReports);

        loadUserInfo();

        findViewById(R.id.itemLichSuBaoCao).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LichSuBaoCaoActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.itemEditProfile).setOnClickListener(v -> showEditProfileDialog());

        findViewById(R.id.itemAboutApp).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Về CYBER-TRAP")
                    .setMessage("Ứng dụng hỗ trợ tra cứu, báo cáo và chia sẻ cảnh báo lừa đảo trực tuyến, giúp bảo vệ bạn và cộng đồng trên không gian mạng.")
                    .setPositiveButton("Đóng", null)
                    .show();
        });

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, TrangChuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            Intent intent = new Intent(this, GiaoDienTraCuu.class);
            intent.putExtra("TAB_INDEX", 1);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navBaoCao).setOnClickListener(v -> {
            Intent intent = new Intent(this, BaoCaoActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navDienDan).setOnClickListener(v -> {
            Intent intent = new Intent(this, NewsFeedActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvStatPosts != null) {
            tvStatPosts.setText(String.valueOf(NewsFeedActivity.allPosts.size()));
        }
        if (tvStatReports != null) {
            SharedPreferences stats = getSharedPreferences("CyberTrapStats", MODE_PRIVATE);
            int reportsCount = stats.getInt("STAT_REPORTS", 0);
            tvStatReports.setText(String.valueOf(reportsCount));
        }
    }

    private void loadUserInfo() {
        String name = UserUtils.getLoggedUsername(this);
        String email = UserUtils.getLoggedUserEmail(this);

        tvUserName.setText(name);
        tvUserEmail.setText(email);
        tvAvatarInitials.setText(UserUtils.getInitials(name));
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        EditText etName = new EditText(this);
        etName.setHint("Họ và tên");
        etName.setText(tvUserName.getText().toString());
        layout.addView(etName);

        EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setText(tvUserEmail.getText().toString());
        layout.addView(etEmail);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa thông tin")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    if (!newName.isEmpty() && !newEmail.isEmpty()) {
                        prefs.edit()
                                .putString("user_name", newName)
                                .putString("user_email", newEmail)
                                .apply();
                        loadUserInfo();
                        Toast.makeText(this, "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "CT";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}