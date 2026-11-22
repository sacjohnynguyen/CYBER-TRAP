package com.example.cyber_trap;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BaoCaoActivity extends AppCompatActivity {

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

        spnHinhThuc = findViewById(R.id.spnHinhThuc);
        etMoTa = findViewById(R.id.etMoTa);
        etDoiTuong = findViewById(R.id.etDoiTuong);
        tvCharCountMoTa = findViewById(R.id.tvCharCountMoTa);
        tvImageCount = findViewById(R.id.tvImageCount);
        btnChonAnh = findViewById(R.id.btnChonAnh);
        layoutSelectedImages = findViewById(R.id.layoutSelectedImages);
        btnGuiBaoCao = findViewById(R.id.btnGuiBaoCao);

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
                        if (imageUri != null) {
                            try {
                                hienThiVaCheMoAnh(imageUri);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        btnChonAnh.setOnClickListener(v -> {
            if (imageCount >= MAX_IMAGES) {
                Toast.makeText(this, "Chỉ được chọn tối đa 4 ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnGuiBaoCao.setOnClickListener(v -> {
            String type = spnHinhThuc.getSelectedItem().toString();
            String target = etDoiTuong.getText().toString().trim();
            String desc = etMoTa.getText().toString().trim();

            if (target.isEmpty() || type.equals("Chọn hình thức lừa đảo")) {
                Toast.makeText(this, "Vui lòng nhập đối tượng và hình thức!", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            hasNewReport = true;
            newReportType = type;
            newReportDesc = desc.isEmpty() ? "Không có mô tả thêm." : desc;
            newReportDate = currentDate;

            SharedPreferences prefs = getSharedPreferences("CyberTrapPrefs", MODE_PRIVATE);
            String token = prefs.getString("ACCESS_TOKEN", null);
            String loggedUser = prefs.getString("LOGGED_USER", "default");

            // Tăng số lượng Thống kê Báo cáo hôm nay (+1) và Số đã chặn (+1)
            SharedPreferences stats = getSharedPreferences("CyberTrapStats", MODE_PRIVATE);
            int reportsCount = stats.getInt("STAT_REPORTS", 0);
            int blockedCount = stats.getInt("STAT_BLOCKED", 0);
            stats.edit()
                    .putInt("STAT_REPORTS", reportsCount + 1)
                    .putInt("STAT_BLOCKED", blockedCount + 1)
                    .apply();
            String targetType = mapTargetType(type, target);
            String finalDesc = desc.isEmpty() ? "Không có mô tả thêm." : desc;
            ReportRequest request = new ReportRequest(target, targetType, type, finalDesc, 0);

            saveReportToLocalCache(prefs, loggedUser, new ReportItem(0, target, targetType, type, finalDesc, "PENDING", currentDate));

            if (token != null && !token.trim().isEmpty()) {
                btnGuiBaoCao.setEnabled(false);
                RetrofitClient.getApiService().createReport("Bearer " + token, request).enqueue(new Callback<ReportPostResponse>() {
                    @Override
                    public void onResponse(Call<ReportPostResponse> call, Response<ReportPostResponse> response) {
                        btnGuiBaoCao.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(BaoCaoActivity.this, "Đã gửi báo cáo thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BaoCaoActivity.this, "Đã lưu báo cáo trên thiết bị.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ReportPostResponse> call, Throwable t) {
                        btnGuiBaoCao.setEnabled(true);
                        Toast.makeText(BaoCaoActivity.this, "Đã lưu báo cáo trên thiết bị.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            etDoiTuong.setText("");
            etMoTa.setText("");
            spnHinhThuc.setSelection(0);
            layoutSelectedImages.removeAllViews();
            layoutSelectedImages.setVisibility(View.GONE);
            imageCount = 0;
            tvImageCount.setText("0/4");

            if (layoutForm != null) layoutForm.setVisibility(View.GONE);
            if (layoutSuccess != null) layoutSuccess.setVisibility(View.VISIBLE);
        });

        if (btnGuiBaoCaoKhac != null) {
            btnGuiBaoCaoKhac.setOnClickListener(v -> {
                if (layoutSuccess != null) layoutSuccess.setVisibility(View.GONE);
                if (layoutForm != null) layoutForm.setVisibility(View.VISIBLE);
                savedImageUris.clear();
            });
        }

        LinearLayout navHomeBtn = findViewById(R.id.navHomeBtn);
        if (navHomeBtn != null) {
            navHomeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoActivity.this, TrangChuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        LinearLayout navLichSuBtn = findViewById(R.id.navLichSuBtn);
        if (navLichSuBtn != null) {
            navLichSuBtn.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoActivity.this, LichSuBaoCaoActivity.class);
                startActivity(intent);
            });
        }

        LinearLayout navProfileBtn = findViewById(R.id.navProfileBtn);
        if (navProfileBtn != null) {
            navProfileBtn.setOnClickListener(v -> {
                Intent intent = new Intent(BaoCaoActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private ImageView createImageViewForGallery() {
        ImageView imageView = new ImageView(this);
        int size = (int) (80 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, 16, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setClipToOutline(true);
        imageView.setBackgroundResource(R.drawable.bg_white_rounded);

        layoutSelectedImages.addView(imageView);
        layoutSelectedImages.setVisibility(View.VISIBLE);
        imageCount++;
        tvImageCount.setText(imageCount + "/4");

        return imageView;
    }

    private void hienThiVaCheMoAnh(Uri uri) throws IOException {
        savedImageUris.add(uri);

        ImageView targetImageView = createImageViewForGallery();
        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        InputImage image = InputImage.fromBitmap(mutableBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    boolean hasRedacted = false;

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                                String text = element.getText();

                                if (text.matches(".*\\d+.*")) {
                                    Rect boundingBox = element.getBoundingBox();
                                    if (boundingBox != null) {
                                        boundingBox.left -= 5;
                                        boundingBox.right += 5;
                                        boundingBox.top -= 5;
                                        boundingBox.bottom += 5;

                                        canvas.drawRect(boundingBox, paint);
                                        hasRedacted = true;
                                    }
                                }
                            }
                        }
                    }

                    targetImageView.setImageBitmap(mutableBitmap);
                    targetImageView.setOnClickListener(v -> showImageDialog(mutableBitmap));

                    if (hasRedacted) {
                        Toast.makeText(this, "Đã tự động che OTP/CCCD!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    targetImageView.setImageBitmap(originalBitmap);
                    targetImageView.setOnClickListener(v -> showImageDialog(originalBitmap));
                    Toast.makeText(this, "Lỗi quét ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showImageDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        ImageView fullScreenImageView = new ImageView(this);
        fullScreenImageView.setImageBitmap(bitmap);
        fullScreenImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullScreenImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(fullScreenImageView);
        dialog.show();
    }

    private String mapTargetType(String type, String target) {
        if (target != null) {
            if (target.contains("@")) return "EMAIL";
            if (target.matches("^\\+?[0-9]{8,15}$")) return "PHONE";
            if (target.startsWith("http://") || target.startsWith("https://") || target.contains(".")) return "URL";
        }
        if (type != null && type.toLowerCase().contains("ngân hàng")) return "BANK_ACCOUNT";
        return "CONTENT";
    }

    private void saveReportToLocalCache(SharedPreferences prefs, String username, ReportItem item) {
        try {
            String key = "CACHE_REPORTS_" + username;
            String existingJson = prefs.getString(key, "[]");
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ReportItem>>() {}.getType();
            ArrayList<ReportItem> list = gson.fromJson(existingJson, listType);
            if (list == null) list = new ArrayList<>();
            list.add(0, item);
            prefs.edit().putString(key, gson.toJson(list)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}