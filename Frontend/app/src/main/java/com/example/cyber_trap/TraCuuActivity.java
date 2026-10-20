package com.example.cyber_trap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TraCuuActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnTypePhone, btnTypeEmail, btnTypeUrl, btnTypeContent, btnScan;
    private EditText edtTarget;
    private TextView btnPaste, btnClear;
    private ProgressBar progressBar;

    private CardView cardResult;
    private LinearLayout layoutResultContainer;
    private TextView tvRiskBadge, tvRiskScore, tvResultMessage, tvCategory, tvTotalReports, tvDetails;

    // Default type: PHONE
    private String selectedType = "PHONE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tra_cuu);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupTypeSelectors();
        setupActionButtons();

        // Check if intent passed a pre-filled type or target
        if (getIntent() != null) {
            String initialType = getIntent().getStringExtra("SCAN_TYPE");
            String initialTarget = getIntent().getStringExtra("SCAN_TARGET");
            if (initialType != null) {
                switchType(initialType);
            }
            if (initialTarget != null) {
                edtTarget.setText(initialTarget);
            }
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnTypePhone = findViewById(R.id.btnTypePhone);
        btnTypeEmail = findViewById(R.id.btnTypeEmail);
        btnTypeUrl = findViewById(R.id.btnTypeUrl);
        btnTypeContent = findViewById(R.id.btnTypeContent);
        btnScan = findViewById(R.id.btnScan);

        edtTarget = findViewById(R.id.edtTarget);
        btnPaste = findViewById(R.id.btnPaste);
        btnClear = findViewById(R.id.btnClear);
        progressBar = findViewById(R.id.progressBar);

        cardResult = findViewById(R.id.cardResult);
        layoutResultContainer = findViewById(R.id.layoutResultContainer);
        tvRiskBadge = findViewById(R.id.tvRiskBadge);
        tvRiskScore = findViewById(R.id.tvRiskScore);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        tvCategory = findViewById(R.id.tvCategory);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvDetails = findViewById(R.id.tvDetails);
    }

    private void setupTypeSelectors() {
        btnTypePhone.setOnClickListener(v -> switchType("PHONE"));
        btnTypeEmail.setOnClickListener(v -> switchType("EMAIL"));
        btnTypeUrl.setOnClickListener(v -> switchType("URL"));
        btnTypeContent.setOnClickListener(v -> switchType("CONTENT"));

        // Highlight initial selected type
        switchType("PHONE");
    }

    private void switchType(String type) {
        selectedType = type;

        // Reset all button styles visually
        btnTypePhone.setAlpha(0.6f);
        btnTypeEmail.setAlpha(0.6f);
        btnTypeUrl.setAlpha(0.6f);
        btnTypeContent.setAlpha(0.6f);

        switch (type) {
            case "PHONE":
                btnTypePhone.setAlpha(1.0f);
                edtTarget.setHint("Nhập số điện thoại (ví dụ: 0912345678)...");
                break;
            case "EMAIL":
                btnTypeEmail.setAlpha(1.0f);
                edtTarget.setHint("Nhập địa chỉ email (ví dụ: cskh@bank-scam.com)...");
                break;
            case "URL":
                btnTypeUrl.setAlpha(1.0f);
                edtTarget.setHint("Nhập liên kết / website (ví dụ: https://nhan-qua-free.com)...");
                break;
            case "CONTENT":
                btnTypeContent.setAlpha(1.0f);
                edtTarget.setHint("Nhập hoặc dán nội dung tin nhắn, SMS nghi vấn...");
                break;
        }
    }

    private void setupActionButtons() {
        btnBack.setOnClickListener(v -> finish());

        // Dán từ Bộ nhớ tạm
        btnPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) {
                    edtTarget.setText(item.getText().toString());
                    Toast.makeText(this, "Đã dán từ bộ nhớ tạm", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bộ nhớ tạm trống!", Toast.LENGTH_SHORT).show();
            }
        });

        // Xóa text
        btnClear.setOnClickListener(v -> {
            edtTarget.setText("");
            cardResult.setVisibility(View.GONE);
        });

        // Nút Quét Lừa Đảo (Gọi API Backend)
        btnScan.setOnClickListener(v -> executeScan());
    }

    private void executeScan() {
        String target = edtTarget.getText().toString().trim();

        if (target.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập thông tin cần quét!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        cardResult.setVisibility(View.GONE);

        ScanRequest request = new ScanRequest(selectedType, target);

        RetrofitClient.getApiService().scanScam(request).enqueue(new Callback<ScanResponseWrapper>() {
            @Override
            public void onResponse(Call<ScanResponseWrapper> call, Response<ScanResponseWrapper> response) {
                progressBar.setVisibility(View.GONE);
                btnScan.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ScanResponseWrapper wrapper = response.body();
                    if (wrapper.getData() != null) {
                        displayScanResult(wrapper.getData());
                    } else {
                        String msg = wrapper.getMessage() != null ? wrapper.getMessage() : "Không thể kiểm tra vào lúc này!";
                        Toast.makeText(TraCuuActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorMsg = "Không thể kiểm tra vào lúc này!";
                        if (response.errorBody() != null) {
                            JSONObject jsonObject = new JSONObject(response.errorBody().string());
                            errorMsg = jsonObject.optString("message", errorMsg);
                        }
                        Toast.makeText(TraCuuActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(TraCuuActivity.this, "Lỗi từ máy chủ Backend!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ScanResponseWrapper> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnScan.setEnabled(true);
                Toast.makeText(TraCuuActivity.this, "Lỗi kết nối máy chủ Retrofit: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayScanResult(ScanResponse result) {
        cardResult.setVisibility(View.VISIBLE);

        String level = result.getRiskLevel() != null ? result.getRiskLevel().toUpperCase() : "SAFE";
        boolean isScam = result.isScam();

        // 1. Theme Color & Badge according to Risk Level
        if (isScam || "DANGEROUS".equals(level) || "CRITICAL".equals(level) || "HIGH".equals(level)) {
            layoutResultContainer.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
            tvRiskBadge.setText("🔴 CỰC KỲ NGUY HIỂM");
            tvRiskBadge.setBackgroundColor(Color.parseColor("#C62828"));
            tvRiskScore.setTextColor(Color.parseColor("#B71C1C"));
        } else if ("WARNING".equals(level) || "MEDIUM".equals(level)) {
            layoutResultContainer.setBackgroundColor(Color.parseColor("#FFFDE7")); // Light Yellow
            tvRiskBadge.setText("🟡 NGHI VẤN / CẢNH BÁO");
            tvRiskBadge.setBackgroundColor(Color.parseColor("#F57F17"));
            tvRiskScore.setTextColor(Color.parseColor("#E65100"));
        } else {
            layoutResultContainer.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
            tvRiskBadge.setText("🟢 AN TOÀN - CHƯA PHÁT HIỆN RỦI RO");
            tvRiskBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            tvRiskScore.setTextColor(Color.parseColor("#1B5E20"));
        }

        // 2. Score & Message
        tvRiskScore.setText("Mức rủi ro: " + result.getRiskScore() + "%");

        if (result.getMessage() != null && !result.getMessage().trim().isEmpty()) {
            tvResultMessage.setText(result.getMessage());
        } else if (isScam || "CRITICAL".equals(level) || "HIGH".equals(level)) {
            tvResultMessage.setText("Phát hiện nguy cơ lừa đảo / giả mạo cao!");
        } else if ("MEDIUM".equals(level) || "WARNING".equals(level)) {
            tvResultMessage.setText("Phát hiện một số dấu hiệu bất thường cần cảnh giác!");
        } else {
            tvResultMessage.setText("Dữ liệu hợp lệ, chưa phát hiện dấu hiệu bất thường.");
        }

        // 3. Category & Total Reports
        String defaultCat = "URL".equals(selectedType) ? "Kiểm tra Link / Website" : ("PHONE".equals(selectedType) ? "Kiểm tra SĐT" : ("EMAIL".equals(selectedType) ? "Kiểm tra Email" : "Quét tự động"));
        String categoryText = result.getCategory() != null ? result.getCategory() : defaultCat;
        tvCategory.setText("Danh mục: " + categoryText);
        tvTotalReports.setText("Báo cáo: " + result.getTotalReports() + " lượt");

        // 4. Details / Reasons list
        StringBuilder detailsBuilder = new StringBuilder();

        if (Boolean.TRUE.equals(result.getBlacklistMatch())) {
            detailsBuilder.append("• Dữ liệu khớp trong danh sách đen (Blacklist)!\n");
        }

        if (result.getNormalizedUrl() != null && !result.getNormalizedUrl().isEmpty()) {
            detailsBuilder.append("• Link chuẩn hóa: ").append(result.getNormalizedUrl()).append("\n");
        } else if (result.getHostname() != null && !result.getHostname().isEmpty()) {
            detailsBuilder.append("• Tên miền (Domain): ").append(result.getHostname()).append("\n");
        }

        if (result.getNormalizedEmail() != null && !result.getNormalizedEmail().isEmpty()) {
            detailsBuilder.append("• Email chuẩn hóa: ").append(result.getNormalizedEmail()).append("\n");
        }
        if (result.getNormalizedPhone() != null && !result.getNormalizedPhone().isEmpty()) {
            detailsBuilder.append("• SĐT chuẩn hóa: ").append(result.getNormalizedPhone()).append("\n");
        }

        List<String> detailsList = result.getDetails();
        if (detailsList != null && !detailsList.isEmpty()) {
            for (String detail : detailsList) {
                detailsBuilder.append("• ").append(detail).append("\n");
            }
        } else {
            detailsBuilder.append("• Chưa có ghi nhận vi phạm trong cơ sở dữ liệu.");
        }
        tvDetails.setText(detailsBuilder.toString().trim());
    }
}
