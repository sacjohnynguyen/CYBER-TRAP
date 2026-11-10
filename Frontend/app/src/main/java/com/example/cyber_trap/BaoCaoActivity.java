package com.example.cyber_trap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BaoCaoActivity extends AppCompatActivity {

    // === CÁC BIẾN STATIC ĐỂ TRUYỀN DỮ LIỆU SANG TRANG LỊCH SỬ VÀ CHI TIẾT ===
    public static boolean hasNewReport = false;
    public static String newReportType = "";
    public static String newReportDesc = "";
    public static String newReportDate = "";
    public static ArrayList<Uri> savedImageUris = new ArrayList<>();

    private Spinner spnHinhThuc;
    private EditText etMoTa, etDoiTuong;
    private TextView tvCharCountMoTa, tvImageCount;
    private LinearLayout btnChonAnh, layoutSelectedImages;
    private MaterialButton btnGuiBaoCao;

    // CÁC BIẾN CHO GIAO DIỆN THÀNH CÔNG
    private View layoutForm;
    private LinearLayout layoutSuccess;
    private MaterialButton btnGuiBaoCaoKhac;

    private int imageCount = 0;
    private final int MAX_IMAGES = 4;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bao_cao);

        // Ánh xạ View cơ bản
        spnHinhThuc = findViewById(R.id.spnHinhThuc);
        etMoTa = findViewById(R.id.etMoTa);
        etDoiTuong = findViewById(R.id.etDoiTuong);
        tvCharCountMoTa = findViewById(R.id.tvCharCountMoTa);
        tvImageCount = findViewById(R.id.tvImageCount);
        btnChonAnh = findViewById(R.id.btnChonAnh);
        layoutSelectedImages = findViewById(R.id.layoutSelectedImages);
        btnGuiBaoCao = findViewById(R.id.btnGuiBaoCao);

        // Ánh xạ layout Thành công
        layoutForm = findViewById(R.id.layoutForm);
        layoutSuccess = findViewById(R.id.layoutSuccess);
        btnGuiBaoCaoKhac = findViewById(R.id.btnGuiBaoCaoKhac);

        String[] fraudTypes = {
                "Chọn hình thức lừa đảo", "Giả mạo ngân hàng", "Giả mạo công an",
                "Lừa đảo đầu tư", "Mua bán hàng giả", "Việc làm giả", "Lừa đảo tình cảm", "Khác"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fraudTypes);
        spnHinhThuc.setAdapter(adapter);

        etMoTa.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCountMoTa.setText(s.length() + "/1.000 ký tự");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) hienThiAnhDaChon(imageUri);
                    }
                }
        );

        btnChonAnh.setOnClickListener(v -> {
            if (imageCount >= MAX_IMAGES) {
                Toast.makeText(this, "Chỉ được chọn tối đa 4 ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // ===============================================
        // NÚT GỬI BÁO CÁO: LƯU DỮ LIỆU & HIỂN THỊ THÀNH CÔNG
        // ===============================================
        btnGuiBaoCao.setOnClickListener(v -> {
            String type = spnHinhThuc.getSelectedItem().toString();
            String target = etDoiTuong.getText().toString().trim();
            String desc = etMoTa.getText().toString().trim();

            if (target.isEmpty() || type.equals("Chọn hình thức lừa đảo")) {
                Toast.makeText(this, "Vui lòng nhập đối tượng và hình thức!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu dữ liệu vào biến toàn cục
            hasNewReport = true;
            newReportType = type;
            newReportDesc = desc.isEmpty() ? "Không có mô tả thêm." : desc;
            newReportDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

            // Xóa rỗng form
            etDoiTuong.setText("");
            etMoTa.setText("");
            spnHinhThuc.setSelection(0);
            layoutSelectedImages.removeAllViews();
            layoutSelectedImages.setVisibility(View.GONE);
            imageCount = 0;
            tvImageCount.setText("0/4");

            // ẨN FORM, HIỂN THỊ GIAO DIỆN THÀNH CÔNG
            if (layoutForm != null) layoutForm.setVisibility(View.GONE);
            if (layoutSuccess != null) layoutSuccess.setVisibility(View.VISIBLE);
        });

        // NÚT GỬI BÁO CÁO KHÁC
        if (btnGuiBaoCaoKhac != null) {
            btnGuiBaoCaoKhac.setOnClickListener(v -> {
                // Ẩn giao diện thành công, bật lại form
                if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);
                if (layoutForm != null) layoutForm.setVisibility(View.VISIBLE);

                // Xóa list ảnh đã gửi trước đó để gửi báo cáo mới
                savedImageUris.clear();
            });
        }

        // Điều hướng: Trang chủ
        LinearLayout navHomeBtn = findViewById(R.id.navHomeBtn);
        if (navHomeBtn != null) {
            navHomeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoActivity.this, TrangChuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Điều hướng: Lịch sử
        LinearLayout navLichSuBtn = findViewById(R.id.navLichSuBtn);
        if (navLichSuBtn != null) {
            navLichSuBtn.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoActivity.this, LichSuBaoCaoActivity.class);
                startActivity(intent);
            });
        }
    }

    private void hienThiAnhDaChon(Uri uri) {
        savedImageUris.add(uri);

        ImageView imageView = new ImageView(this);
        int size = (int) (80 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, 16, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);
        imageView.setClipToOutline(true);
        imageView.setBackgroundResource(R.drawable.bg_white_rounded);
        layoutSelectedImages.addView(imageView);
        layoutSelectedImages.setVisibility(View.VISIBLE);
        imageCount++;
        tvImageCount.setText(imageCount + "/4");
    }
}