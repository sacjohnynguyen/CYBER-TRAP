package com.example.cyber_trap;

import android.app.Dialog;
import android.content.Intent;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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
            // ĐÃ SỬA: Thay đổi cách gọi Intent để chọn ảnh ổn định hơn trên máy ảo
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

            hasNewReport = true;
            newReportType = type;
            newReportDesc = desc.isEmpty() ? "Không có mô tả thêm." : desc;
            newReportDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

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
    }

    private ImageView createImageViewForGallery() {
        ImageView imageView = new ImageView(this);
        // Đã sửa lỗi thiếu dấu nhân (*) ở đây
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

                                // Sử dụng biểu thức Regex mạnh tay nhất để che mọi chữ số
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
}