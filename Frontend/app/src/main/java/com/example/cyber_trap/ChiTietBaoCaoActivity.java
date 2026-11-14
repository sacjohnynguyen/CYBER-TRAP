package com.example.cyber_trap;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class ChiTietBaoCaoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_bao_cao);

        // Nút Quay lại
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Ánh xạ dữ liệu chung
        TextView tvChiTietLoai = findViewById(R.id.tvChiTietLoai);
        TextView tvChiTietMoTa = findViewById(R.id.tvChiTietMoTa);
        TextView tvChiTietNgay = findViewById(R.id.tvChiTietNgay);

        // Đổ dữ liệu từ màn hình Gửi Báo Cáo sang
        if (BaoCaoActivity.hasNewReport) {
            if (tvChiTietLoai != null) tvChiTietLoai.setText(BaoCaoActivity.newReportType);
            if (tvChiTietMoTa != null) tvChiTietMoTa.setText(BaoCaoActivity.newReportDesc);
            if (tvChiTietNgay != null) tvChiTietNgay.setText(BaoCaoActivity.newReportDate);
        }

        // Hiển thị danh sách ảnh bằng chứng
        LinearLayout layoutChiTietImages = findViewById(R.id.layoutChiTietImages);
        MaterialCardView cardAnhBangChung = findViewById(R.id.cardAnhBangChung);

        if (cardAnhBangChung != null && layoutChiTietImages != null) {
            if (BaoCaoActivity.savedImageUris != null && !BaoCaoActivity.savedImageUris.isEmpty()) {
                cardAnhBangChung.setVisibility(View.VISIBLE);
                layoutChiTietImages.removeAllViews();

                for (Uri uri : BaoCaoActivity.savedImageUris) {
                    ImageView imgView = new ImageView(this);
                    // Đã sửa lỗi thiếu dấu nhân (*)
                    int size = (int) (80 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.setMargins(0, 0, 16, 0);
                    imgView.setLayoutParams(params);
                    imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgView.setClipToOutline(true);
                    imgView.setBackgroundResource(R.drawable.bg_white_rounded);

                    layoutChiTietImages.addView(imgView);

                    // LOGIC CHE MỜ VÀ GẮN SỰ KIỆN PHÓNG TO ẢNH
                    try {
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
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    imgView.setImageBitmap(mutableBitmap);
                                    imgView.setOnClickListener(v -> showImageDialog(mutableBitmap));
                                })
                                .addOnFailureListener(e -> {
                                    // Báo lỗi quét nhưng vẫn hiển thị ảnh gốc và cho xem to
                                    imgView.setImageBitmap(originalBitmap);
                                    imgView.setOnClickListener(v -> showImageDialog(originalBitmap));
                                });

                    } catch (IOException e) {
                        e.printStackTrace();
                        // Phương án dự phòng nếu lỗi không tải được Bitmap
                        imgView.setImageURI(uri);
                    }
                }
            } else {
                cardAnhBangChung.setVisibility(View.GONE);
            }
        }
    }

    // Hàm hiển thị ảnh phóng to toàn màn hình
    private void showImageDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        ImageView fullScreenImageView = new ImageView(this);
        fullScreenImageView.setImageBitmap(bitmap);
        fullScreenImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullScreenImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Bấm vào ảnh to sẽ tự động đóng
        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(fullScreenImageView);
        dialog.show();
    }
}