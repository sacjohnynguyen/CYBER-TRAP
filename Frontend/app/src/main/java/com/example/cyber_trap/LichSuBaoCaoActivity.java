package com.example.cyber_trap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class LichSuBaoCaoActivity extends AppCompatActivity {

    private LinearLayout layoutEmptyState;
    private MaterialCardView cardBaoCaoItem1;
    private TextView tvResultCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lich_su_bao_cao);

        // Ánh xạ Menu dưới
        LinearLayout navHomeBtnLichSu = findViewById(R.id.navHomeBtnLichSu);
        LinearLayout navBaoCaoBtnLichSu = findViewById(R.id.navBaoCaoBtnLichSu);

        if (navHomeBtnLichSu != null) {
            navHomeBtnLichSu.setOnClickListener(v -> {
                Intent intent = new Intent(LichSuBaoCaoActivity.this, TrangChuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (navBaoCaoBtnLichSu != null) {
            navBaoCaoBtnLichSu.setOnClickListener(v -> {
                Intent intent = new Intent(LichSuBaoCaoActivity.this, BaoCaoActivity.class);
                startActivity(intent);
                finish();
            });
        }

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        cardBaoCaoItem1 = findViewById(R.id.cardBaoCaoItem1);
        tvResultCount = findViewById(R.id.tvResultCount);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView tvReportItemType = findViewById(R.id.tvReportItemType);
        TextView tvReportItemDesc = findViewById(R.id.tvReportItemDesc);
        TextView tvReportItemDate = findViewById(R.id.tvReportItemDate);

        // Kiểm tra xem đã gửi báo cáo nào chưa
        if (BaoCaoActivity.hasNewReport) {
            layoutEmptyState.setVisibility(View.GONE);
            cardBaoCaoItem1.setVisibility(View.VISIBLE);
            tvResultCount.setText("1 kết quả");

            if (tvReportItemType != null) tvReportItemType.setText(BaoCaoActivity.newReportType);
            if (tvReportItemDesc != null) tvReportItemDesc.setText(BaoCaoActivity.newReportDesc);
            if (tvReportItemDate != null) tvReportItemDate.setText(BaoCaoActivity.newReportDate);

            if (cardBaoCaoItem1 != null) {
                cardBaoCaoItem1.setOnClickListener(v -> {
                    Intent intent = new Intent(LichSuBaoCaoActivity.this, ChiTietBaoCaoActivity.class);
                    // Không cần gửi trạng thái đi nữa
                    startActivity(intent);
                });
            }
        } else {
            layoutEmptyState.setVisibility(View.VISIBLE);
            cardBaoCaoItem1.setVisibility(View.GONE);
            tvResultCount.setText("0 kết quả");
        }
    }
}