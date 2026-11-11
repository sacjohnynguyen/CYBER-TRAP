package com.example.cyber_trap;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

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
                    int size = (int) (80 * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.setMargins(0, 0, 16, 0);
                    imgView.setLayoutParams(params);
                    imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgView.setImageURI(uri);
                    imgView.setClipToOutline(true);
                    imgView.setBackgroundResource(R.drawable.bg_white_rounded);

                    layoutChiTietImages.addView(imgView);
                }
            } else {
                cardAnhBangChung.setVisibility(View.GONE);
            }
        }
    }
}