package com.example.doannhom;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class GiaoDienTraCuu extends AppCompatActivity {

    // Tiêu đề trên cùng và 4 khung nội dung hiển thị bên dưới 4 Tab
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private LinearLayout layoutScreenTraCuu, layoutScreenEmail, layoutScreenWeb, layoutScreenText;

    // --- 4 TAB CHỌN DANH MỤC ---
    private MaterialCardView tabPhone, tabEmail, tabWebsite, tabAccount;
    private TextView tvTabPhone, tvTabEmail, tvTabWebsite, tvTabAccount;

    // --- THÀNH PHẦN TAB 1: TRA CỨU SỐ ĐIỆN THOẠI (CHỈ NHẬP SỐ) ---
    private TextView tvSearchIcon;
    private EditText etLookupInput;
    private MaterialButton btnLookup;
    private LinearLayout layoutCategoryTabs, layoutQuickLookup;
    private MaterialCardView cardResultSafe, cardResultDanger;
    private TextView tvSafePhone, tvSafeType, tvSafeDescription;
    private TextView tvDangerPhone, tvDangerType, tvDangerReportCount, tvDangerDescription;

    // --- THÀNH PHẦN TAB 2: TRA CỨU GMAIL ---
    private TextView tvEmailSearchIcon;
    private EditText etEmailInput;
    private MaterialButton btnLookupEmail;
    private LinearLayout layoutQuickEmailLookup;
    private MaterialCardView cardEmailResultSafe, cardEmailResultDanger;
    private TextView tvSafeEmail, tvSafeEmailType, tvSafeEmailDescription;
    private TextView tvDangerEmail, tvDangerEmailType, tvDangerEmailReportCount, tvDangerEmailDescription;

    // --- THÀNH PHẦN TAB 3: QUÉT WEBSITE ---
    private EditText etUrl;
    private ImageButton btnClearUrl;
    private MaterialButton btnCheckSafety;
    private MaterialCardView cardStateDefault, cardStateSafe, cardStateDanger;
    private TextView tvSafeDomain, tvDangerDomain;

    // --- THÀNH PHẦN TAB 4: PHÂN TÍCH TIN NHẮN (VĂN BẢN) ---
    private EditText etTextContent;
    private TextView tvCharCount, tvQuickPaste, tvTrySample, btnClearText;
    private MaterialButton btnAnalyzeText;
    private MaterialCardView cardTextResult;
    private LinearLayout layoutTextResultHeader;
    private TextView tvTextRiskIcon, tvTextRiskBadge, tvTextRiskTitle, tvTextRiskDesc, tvTextRiskScore;
    private ProgressBar pbTextRisk;
    private TextView tvSign1, tvSign2, tvSign3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giaodientracuu);

        // Ánh xạ tiêu đề và 4 khung nội dung bên dưới
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        layoutScreenTraCuu = findViewById(R.id.layoutScreenTraCuu);
        layoutScreenEmail = findViewById(R.id.layoutScreenEmail);
        layoutScreenWeb = findViewById(R.id.layoutScreenWeb);
        layoutScreenText = findViewById(R.id.layoutScreenText);

        // Ánh xạ 4 nút Tab
        tabPhone = findViewById(R.id.tabPhone);
        tabEmail = findViewById(R.id.tabEmail);
        tabWebsite = findViewById(R.id.tabWebsite);
        tabAccount = findViewById(R.id.tabAccount);

        tvTabPhone = findViewById(R.id.tvTabPhone);
        tvTabEmail = findViewById(R.id.tvTabEmail);
        tvTabWebsite = findViewById(R.id.tvTabWebsite);
        tvTabAccount = findViewById(R.id.tvTabAccount);

        // Bấm vào từng Tab sẽ hiện nội dung tương ứng ngay bên dưới:
        // 1: Số điện thoại | 2: Tin nhắn (giữ tương thích với Trang chủ) | 3: Website | 4: Gmail
        tabPhone.setOnClickListener(v -> selectLookupTab(1));
        tabEmail.setOnClickListener(v -> selectLookupTab(4));
        tabWebsite.setOnClickListener(v -> selectLookupTab(3));
        tabAccount.setOnClickListener(v -> selectLookupTab(2));

        // Thanh điều hướng dưới cùng (Bottom Navigation)
        View navLookup = findViewById(R.id.navLookup);
        if (navLookup != null) {
            navLookup.setOnClickListener(v -> selectLookupTab(1));
        }

        View navWarning = findViewById(R.id.navWarning);
        if (navWarning != null) {
            navWarning.setOnClickListener(v -> selectLookupTab(3));
        }

        // Bấm vào nút "Trang chủ" ở thanh dưới cùng để quay về Trang chủ
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(GiaoDienTraCuu.this, trangchu.class);
                startActivity(intent);
                finish();
            });
        }

        // =====================================================================
        // 1. KHỞI TẠO TAB SỐ ĐIỆN THOẠI (CHỈ CHO NHẬP SỐ)
        // =====================================================================
        tvSearchIcon = findViewById(R.id.tvSearchIcon);
        etLookupInput = findViewById(R.id.etLookupInput);
        btnLookup = findViewById(R.id.btnLookup);
        layoutCategoryTabs = findViewById(R.id.layoutCategoryTabs);
        layoutQuickLookup = findViewById(R.id.layoutQuickLookup);

        cardResultSafe = findViewById(R.id.cardResultSafe);
        cardResultDanger = findViewById(R.id.cardResultDanger);
        tvSafePhone = findViewById(R.id.tvSafePhone);
        tvSafeType = findViewById(R.id.tvSafeType);
        tvSafeDescription = findViewById(R.id.tvSafeDescription);

        tvDangerPhone = findViewById(R.id.tvDangerPhone);
        tvDangerType = findViewById(R.id.tvDangerType);
        tvDangerReportCount = findViewById(R.id.tvDangerReportCount);
        tvDangerDescription = findViewById(R.id.tvDangerDescription);

        // Cố định chỉ cho phép nhập số
        etLookupInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Bấm vào 3 gợi ý "Tra cứu nhanh" SĐT
        findViewById(R.id.chipPhone1).setOnClickListener(v -> {
            etLookupInput.setText("0987654321");
            etLookupInput.setSelection(etLookupInput.getText().length());
        });
        findViewById(R.id.chipPhone2).setOnClickListener(v -> {
            etLookupInput.setText("0912345678");
            etLookupInput.setSelection(etLookupInput.getText().length());
        });
        findViewById(R.id.chipPhone3).setOnClickListener(v -> {
            etLookupInput.setText("0901234567");
            etLookupInput.setSelection(etLookupInput.getText().length());
        });

        etLookupInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                btnLookup.setEnabled(hasText);
                btnLookup.setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor(hasText ? "#D92D20" : "#F2A6A6")
                ));
                if (!hasText) {
                    showLookupInitialState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLookup.setOnClickListener(v -> {
            String input = etLookupInput.getText().toString().trim();

            if (input.equals("0987654321") || input.equals("0912345678")) {
                showPhoneDangerResult(
                        input,
                        "Giả mạo ngân hàng",
                        "247 lần",
                        "Xưng nhân viên VietcomBank, yêu cầu cung cấp OTP."
                );
            } else {
                showPhoneSafeResult(
                        input,
                        "Không có báo cáo",
                        "Không tìm thấy thông tin lừa đảo. Vẫn nên cẩn thận khi giao dịch."
                );
            }
        });

        // =====================================================================
        // 2. KHỞI TẠO TAB TRA CỨU GMAIL
        // =====================================================================
        tvEmailSearchIcon = findViewById(R.id.tvEmailSearchIcon);
        etEmailInput = findViewById(R.id.etEmailInput);
        btnLookupEmail = findViewById(R.id.btnLookupEmail);
        layoutQuickEmailLookup = findViewById(R.id.layoutQuickEmailLookup);

        cardEmailResultSafe = findViewById(R.id.cardEmailResultSafe);
        cardEmailResultDanger = findViewById(R.id.cardEmailResultDanger);
        tvSafeEmail = findViewById(R.id.tvSafeEmail);
        tvSafeEmailType = findViewById(R.id.tvSafeEmailType);
        tvSafeEmailDescription = findViewById(R.id.tvSafeEmailDescription);

        tvDangerEmail = findViewById(R.id.tvDangerEmail);
        tvDangerEmailType = findViewById(R.id.tvDangerEmailType);
        tvDangerEmailReportCount = findViewById(R.id.tvDangerEmailReportCount);
        tvDangerEmailDescription = findViewById(R.id.tvDangerEmailDescription);

        // Bấm vào 2 gợi ý "Tra cứu nhanh" Gmail
        findViewById(R.id.chipEmail1).setOnClickListener(v -> {
            etEmailInput.setText("hotro.nhanthuong@gmail.com");
            etEmailInput.setSelection(etEmailInput.getText().length());
        });
        findViewById(R.id.chipEmail2).setOnClickListener(v -> {
            etEmailInput.setText("nguoidung@gmail.com");
            etEmailInput.setSelection(etEmailInput.getText().length());
        });

        etEmailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                btnLookupEmail.setEnabled(hasText);
                btnLookupEmail.setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor(hasText ? "#D92D20" : "#F2A6A6")
                ));
                if (!hasText) {
                    showEmailLookupInitialState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLookupEmail.setOnClickListener(v -> {
            String email = etEmailInput.getText().toString().trim();
            String lowerEmail = email.toLowerCase();

            if (!email.contains("@")) {
                Toast.makeText(this, "Vui lòng nhập đúng định dạng địa chỉ Email!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra mẫu cho địa chỉ Gmail
            if (lowerEmail.equals("hotro.nhanthuong@gmail.com")
                    || lowerEmail.equals("abc@gmail.com")
                    || lowerEmail.contains("nhanthuong")
                    || lowerEmail.contains("scam")
                    || lowerEmail.contains("luadao")) {
                showEmailDangerResult(
                        email,
                        "Giả mạo trúng thưởng / Ngân hàng",
                        "185 lần",
                        "Gửi email thông báo trúng thưởng giả mạo hoặc phát tán đường dẫn đánh cắp thông tin đăng nhập và mã OTP."
                );
            } else {
                showEmailSafeResult(
                        email,
                        "Không có báo cáo",
                        "Địa chỉ Gmail này chưa từng bị báo cáo lừa đảo. Tuy nhiên vẫn nên cẩn trọng với các tệp đính kèm lạ."
                );
            }
        });

        // =====================================================================
        // 3. KHỞI TẠO TAB QUÉT WEBSITE
        // =====================================================================
        etUrl = findViewById(R.id.etUrl);
        btnClearUrl = findViewById(R.id.btnClearUrl);
        btnCheckSafety = findViewById(R.id.btnCheckSafety);
        cardStateDefault = findViewById(R.id.cardStateDefault);
        cardStateSafe = findViewById(R.id.cardStateSafe);
        cardStateDanger = findViewById(R.id.cardStateDanger);
        tvSafeDomain = findViewById(R.id.tvSafeDomain);
        tvDangerDomain = findViewById(R.id.tvDangerDomain);

        etUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                btnClearUrl.setVisibility(hasText ? View.VISIBLE : View.GONE);
                btnCheckSafety.setEnabled(hasText);
                btnCheckSafety.setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor(hasText ? "#E02424" : "#F2A6A6")
                ));
                if (!hasText) {
                    showWebDefaultState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearUrl.setOnClickListener(v -> etUrl.setText(""));

        btnCheckSafety.setOnClickListener(v -> {
            String rawInput = etUrl.getText().toString().trim();
            String domain = extractDomain(rawInput);

            if (domain.contains("vay-tien-nhanh") || domain.contains("lua-dao") || domain.contains("scam")) {
                showWebDangerResult(domain);
            } else {
                showWebSafeResult(domain);
            }
        });

        // =====================================================================
        // 4. KHỞI TẠO TAB PHÂN TÍCH TIN NHẮN (VĂN BẢN)
        // =====================================================================
        etTextContent = findViewById(R.id.etTextContent);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvQuickPaste = findViewById(R.id.tvQuickPaste);
        tvTrySample = findViewById(R.id.tvTrySample);
        btnClearText = findViewById(R.id.btnClearText);
        btnAnalyzeText = findViewById(R.id.btnAnalyzeText);
        cardTextResult = findViewById(R.id.cardTextResult);

        layoutTextResultHeader = findViewById(R.id.layoutTextResultHeader);
        tvTextRiskIcon = findViewById(R.id.tvTextRiskIcon);
        tvTextRiskBadge = findViewById(R.id.tvTextRiskBadge);
        tvTextRiskTitle = findViewById(R.id.tvTextRiskTitle);
        tvTextRiskDesc = findViewById(R.id.tvTextRiskDesc);
        tvTextRiskScore = findViewById(R.id.tvTextRiskScore);
        pbTextRisk = findViewById(R.id.pbTextRisk);
        tvSign1 = findViewById(R.id.tvSign1);
        tvSign2 = findViewById(R.id.tvSign2);
        tvSign3 = findViewById(R.id.tvSign3);

        etTextContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + " ký tự");
                if (s.toString().trim().isEmpty()) {
                    cardTextResult.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tvTrySample.setOnClickListener(v -> {
            String sample = "Tài khoản của bạn sắp bị khóa. Vui lòng nhấp vào đường dẫn và cung cấp mã OTP ngay lập tức để xác minh.";
            etTextContent.setText(sample);
            etTextContent.setSelection(sample.length());
        });

        tvQuickPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    ClipData.Item item = clip.getItemAt(0);
                    if (item != null && item.getText() != null) {
                        String pasted = item.getText().toString();
                        etTextContent.setText(pasted);
                        etTextContent.setSelection(pasted.length());
                        return;
                    }
                }
            }
            String sample = "Tài khoản của bạn sắp bị khóa. Vui lòng nhấp vào đường dẫn và cung cấp mã OTP ngay lập tức để xác minh.";
            etTextContent.setText(sample);
            etTextContent.setSelection(sample.length());
        });

        btnClearText.setOnClickListener(v -> {
            etTextContent.setText("");
            cardTextResult.setVisibility(View.GONE);
        });

        btnAnalyzeText.setOnClickListener(v -> {
            String content = etTextContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung cần phân tích!", Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeTextContent(content);
        });

        // Kiểm tra xem có nhận tab đầu tiên từ Trang chủ chuyển sang không
        int initialTab = getIntent().getIntExtra("TAB_INDEX", 1);
        selectLookupTab(initialTab);
    }

    // =========================================================================
    // HÀM CHUYỂN ĐỔI 4 TAB HIỂN THỊ NGAY BÊN DƯỚI
    // tabIndex: 1 = SĐT | 4 = Gmail | 3 = Website | 2 = Tin nhắn
    // =========================================================================

    private void selectLookupTab(int tabIndex) {
        // Đổi màu viền và nền cho Tab đang được chọn
        tabPhone.setCardBackgroundColor(Color.parseColor(tabIndex == 1 ? "#FEF3F2" : "#FFFFFF"));
        tabPhone.setStrokeColor(Color.parseColor(tabIndex == 1 ? "#D92D20" : "#EAECF0"));
        tvTabPhone.setTextColor(Color.parseColor(tabIndex == 1 ? "#D92D20" : "#475467"));

        tabEmail.setCardBackgroundColor(Color.parseColor(tabIndex == 4 ? "#FEF3F2" : "#FFFFFF"));
        tabEmail.setStrokeColor(Color.parseColor(tabIndex == 4 ? "#D92D20" : "#EAECF0"));
        tvTabEmail.setTextColor(Color.parseColor(tabIndex == 4 ? "#D92D20" : "#475467"));

        tabWebsite.setCardBackgroundColor(Color.parseColor(tabIndex == 3 ? "#FEF3F2" : "#FFFFFF"));
        tabWebsite.setStrokeColor(Color.parseColor(tabIndex == 3 ? "#D92D20" : "#EAECF0"));
        tvTabWebsite.setTextColor(Color.parseColor(tabIndex == 3 ? "#D92D20" : "#475467"));

        tabAccount.setCardBackgroundColor(Color.parseColor(tabIndex == 2 ? "#FEF3F2" : "#FFFFFF"));
        tabAccount.setStrokeColor(Color.parseColor(tabIndex == 2 ? "#D92D20" : "#EAECF0"));
        tvTabAccount.setTextColor(Color.parseColor(tabIndex == 2 ? "#D92D20" : "#475467"));

        // Ẩn/Hiện nội dung tương ứng ngay bên dưới 4 nút Tab
        layoutScreenTraCuu.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutScreenEmail.setVisibility(tabIndex == 4 ? View.VISIBLE : View.GONE);
        layoutScreenWeb.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
        layoutScreenText.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        // Cập nhật dòng tiêu đề trên thanh màu đỏ theo từng Tab
        if (tabIndex == 1 || tabIndex == 4) {
            tvHeaderTitle.setText("Tra cứu lừa đảo");
            tvHeaderSubtitle.setText("Kiểm tra thông tin trước khi giao dịch");
        } else if (tabIndex == 2) {
            tvHeaderTitle.setText("Phân tích tin nhắn");
            tvHeaderSubtitle.setText("Nhận diện dấu hiệu đáng ngờ trong tin nhắn");
        } else if (tabIndex == 3) {
            tvHeaderTitle.setText("Quét website");
            tvHeaderSubtitle.setText("Phát hiện đường dẫn giả mạo trước khi truy cập");
        }
    }

    // Các hàm mở trực tiếp (giữ lại nếu đội Backend muốn gọi)
    public void openLookupScreen() { selectLookupTab(1); }
    public void openEmailLookupScreen() { selectLookupTab(4); }
    public void openTextAnalyzerScreen() { selectLookupTab(2); }
    public void openWebScannerScreen() { selectLookupTab(3); }

    // =========================================================================
    // HÀM XỬ LÝ PHÂN TÍCH VĂN BẢN
    // =========================================================================

    private void analyzeTextContent(String text) {
        String lower = text.toLowerCase();
        int score = 0;
        List<String> signs = new ArrayList<>();

        if (lower.contains("otp") || lower.contains("mã xác thực") || lower.contains("mật khẩu")) {
            score += 25;
            signs.add("⚠   Yêu cầu cung cấp mã OTP hoặc mã xác thực");
        }
        if (lower.contains("đường dẫn") || lower.contains("link") || lower.contains("http") || lower.contains("nhấp vào")) {
            score += 25;
            signs.add("⚠   Yêu cầu truy cập đường dẫn không xác định");
        }
        if (lower.contains("ngay lập tức") || lower.contains("sắp bị khóa") || lower.contains("khẩn cấp") || lower.contains("bị khóa")) {
            score += 25;
            signs.add("⚠   Tạo áp lực thời gian để người nhận hành động vội");
        }
        if (lower.contains("chuyển khoản") || lower.contains("trúng thưởng") || lower.contains("hoàn tiền")) {
            score += 25;
            signs.add("⚠   Dụ dỗ lợi ích tài chính hoặc yêu cầu chuyển tiền");
        }

        if (score > 100) score = 100;

        cardTextResult.setVisibility(View.VISIBLE);
        tvTextRiskScore.setText(score + "/100");
        pbTextRisk.setProgress(score);

        if (score >= 50) {
            tvTextRiskIcon.setText("⚠");
            tvTextRiskIcon.setTextColor(Color.parseColor("#D92D20"));
            tvTextRiskBadge.setText("RỦI RO CAO");
            tvTextRiskBadge.setTextColor(Color.parseColor("#B42318"));
            tvTextRiskTitle.setText("Nguy cơ lừa đảo cao");
            tvTextRiskTitle.setTextColor(Color.parseColor("#D92D20"));
            tvTextRiskDesc.setText("Nội dung có nhiều dấu hiệu thường gặp trong các kịch bản lừa đảo.");
            tvTextRiskScore.setTextColor(Color.parseColor("#D92D20"));
            pbTextRisk.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#D92D20")));
            layoutTextResultHeader.setBackgroundColor(Color.parseColor("#FFF5F5"));
            cardTextResult.setStrokeColor(Color.parseColor("#FECDCA"));
        } else if (score > 0) {
            tvTextRiskIcon.setText("⚠");
            tvTextRiskIcon.setTextColor(Color.parseColor("#D97706"));
            tvTextRiskBadge.setText("CẢNH GIÁC");
            tvTextRiskBadge.setTextColor(Color.parseColor("#B54708"));
            tvTextRiskTitle.setText("Có dấu hiệu đáng ngờ");
            tvTextRiskTitle.setTextColor(Color.parseColor("#D97706"));
            tvTextRiskDesc.setText("Nội dung có chứa từ khóa nhạy cảm, cần xác minh kỹ trước khi thực hiện.");
            tvTextRiskScore.setTextColor(Color.parseColor("#D97706"));
            pbTextRisk.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));
            layoutTextResultHeader.setBackgroundColor(Color.parseColor("#FFFAEB"));
            cardTextResult.setStrokeColor(Color.parseColor("#FEDF89"));
        } else {
            tvTextRiskIcon.setText("✔");
            tvTextRiskIcon.setTextColor(Color.parseColor("#027A48"));
            tvTextRiskBadge.setText("AN TOÀN");
            tvTextRiskBadge.setTextColor(Color.parseColor("#027A48"));
            tvTextRiskTitle.setText("Chưa phát hiện rủi ro");
            tvTextRiskTitle.setTextColor(Color.parseColor("#027A48"));
            tvTextRiskDesc.setText("Nội dung hiện tại chưa có các dấu hiệu lừa đảo phổ biến.");
            tvTextRiskScore.setTextColor(Color.parseColor("#027A48"));
            pbTextRisk.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#027A48")));
            layoutTextResultHeader.setBackgroundColor(Color.parseColor("#ECFDF3"));
            cardTextResult.setStrokeColor(Color.parseColor("#A6F4C5"));
            signs.add("✓   Không tìm thấy từ khóa đe dọa hoặc yêu cầu cung cấp mã bảo mật");
        }

        tvSign1.setVisibility(signs.size() >= 1 ? View.VISIBLE : View.GONE);
        if (signs.size() >= 1) tvSign1.setText(signs.get(0));

        tvSign2.setVisibility(signs.size() >= 2 ? View.VISIBLE : View.GONE);
        if (signs.size() >= 2) tvSign2.setText(signs.get(1));

        tvSign3.setVisibility(signs.size() >= 3 ? View.VISIBLE : View.GONE);
        if (signs.size() >= 3) tvSign3.setText(signs.get(2));
    }

    // =========================================================================
    // CÁC HÀM GIAO DIỆN ĐỂ ĐỘI BACKEND GỌI KHI CÓ KẾT QUẢ (SĐT, GMAIL, WEB)
    // =========================================================================

    // --- Tab 1: Số điện thoại ---
    public void showLookupInitialState() {
        btnLookup.setText("Tra cứu");
        tvSearchIcon.setVisibility(View.VISIBLE);
        layoutQuickLookup.setVisibility(View.VISIBLE);
        cardResultSafe.setVisibility(View.GONE);
        cardResultDanger.setVisibility(View.GONE);
    }

    public void showPhoneSafeResult(String phone, String fraudType, String description) {
        btnLookup.setText("Kiểm tra");
        tvSearchIcon.setVisibility(View.GONE);
        layoutQuickLookup.setVisibility(View.GONE);

        tvSafePhone.setText(phone);
        tvSafeType.setText(fraudType);
        tvSafeDescription.setText(description);

        cardResultSafe.setVisibility(View.VISIBLE);
        cardResultDanger.setVisibility(View.GONE);
    }

    public void showPhoneDangerResult(String phone, String fraudType, String reportCount, String description) {
        btnLookup.setText("Kiểm tra");
        tvSearchIcon.setVisibility(View.GONE);
        layoutQuickLookup.setVisibility(View.GONE);

        tvDangerPhone.setText(phone);
        tvDangerType.setText(fraudType);
        tvDangerReportCount.setText(reportCount);
        tvDangerDescription.setText(description);

        cardResultSafe.setVisibility(View.GONE);
        cardResultDanger.setVisibility(View.VISIBLE);
    }

    // --- Tab 2: Gmail ---
    public void showEmailLookupInitialState() {
        btnLookupEmail.setText("Tra cứu");
        tvEmailSearchIcon.setVisibility(View.VISIBLE);
        layoutQuickEmailLookup.setVisibility(View.VISIBLE);
        cardEmailResultSafe.setVisibility(View.GONE);
        cardEmailResultDanger.setVisibility(View.GONE);
    }

    public void showEmailSafeResult(String email, String fraudType, String description) {
        btnLookupEmail.setText("Kiểm tra");
        tvEmailSearchIcon.setVisibility(View.GONE);
        layoutQuickEmailLookup.setVisibility(View.GONE);

        tvSafeEmail.setText(email);
        tvSafeEmailType.setText(fraudType);
        tvSafeEmailDescription.setText(description);

        cardEmailResultSafe.setVisibility(View.VISIBLE);
        cardEmailResultDanger.setVisibility(View.GONE);
    }

    public void showEmailDangerResult(String email, String fraudType, String reportCount, String description) {
        btnLookupEmail.setText("Kiểm tra");
        tvEmailSearchIcon.setVisibility(View.GONE);
        layoutQuickEmailLookup.setVisibility(View.GONE);

        tvDangerEmail.setText(email);
        tvDangerEmailType.setText(fraudType);
        tvDangerEmailReportCount.setText(reportCount);
        tvDangerEmailDescription.setText(description);

        cardEmailResultSafe.setVisibility(View.GONE);
        cardEmailResultDanger.setVisibility(View.VISIBLE);
    }

    // --- Tab 3: Website ---
    public void showWebDefaultState() {
        cardStateDefault.setVisibility(View.VISIBLE);
        cardStateSafe.setVisibility(View.GONE);
        cardStateDanger.setVisibility(View.GONE);
    }

    public void showWebSafeResult(String domain) {
        tvSafeDomain.setText(domain);
        cardStateDefault.setVisibility(View.GONE);
        cardStateSafe.setVisibility(View.VISIBLE);
        cardStateDanger.setVisibility(View.GONE);
    }

    public void showWebDangerResult(String domain) {
        tvDangerDomain.setText(domain);
        cardStateDefault.setVisibility(View.GONE);
        cardStateSafe.setVisibility(View.GONE);
        cardStateDanger.setVisibility(View.VISIBLE);
    }

    private String extractDomain(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return url;
    }
}