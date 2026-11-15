package com.example.cyber_trap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LichSuBaoCaoActivity extends AppCompatActivity {

    private LinearLayout layoutEmptyState;
    private LinearLayout layoutReportContainer;
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
        layoutReportContainer = findViewById(R.id.layoutReportContainer);
        cardBaoCaoItem1 = findViewById(R.id.cardBaoCaoItem1);
        tvResultCount = findViewById(R.id.tvResultCount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportHistory();
    }

    private void loadReportHistory() {
        SharedPreferences prefs = getSharedPreferences("CyberTrapPrefs", MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", null);
        String loggedUser = prefs.getString("LOGGED_USER", "default");
        int userId = prefs.getInt("USER_ID", -1);

        if (token != null && !token.trim().isEmpty() && userId > 0) {
            RetrofitClient.getApiService().getUserReports("Bearer " + token, userId).enqueue(new Callback<ReportListResponse>() {
                @Override
                public void onResponse(Call<ReportListResponse> call, Response<ReportListResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<ReportItem> reports = response.body().getData();
                        if (!reports.isEmpty()) {
                            saveReportsToLocalCache(prefs, loggedUser, reports);
                            displayReports(reports);
                        } else {
                            loadLocalCacheReports(prefs, loggedUser);
                        }
                    } else {
                        loadLocalCacheReports(prefs, loggedUser);
                    }
                }

                @Override
                public void onFailure(Call<ReportListResponse> call, Throwable t) {
                    loadLocalCacheReports(prefs, loggedUser);
                }
            });
        } else {
            loadLocalCacheReports(prefs, loggedUser);
        }
    }

    private void loadLocalCacheReports(SharedPreferences prefs, String username) {
        String key = "CACHE_REPORTS_" + username;
        String json = prefs.getString(key, "[]");
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ReportItem>>() {}.getType();
            List<ReportItem> list = gson.fromJson(json, listType);

            if ((list == null || list.isEmpty()) && BaoCaoActivity.hasNewReport) {
                list = new ArrayList<>();
                list.add(new ReportItem(
                        1,
                        "",
                        "",
                        BaoCaoActivity.newReportType,
                        BaoCaoActivity.newReportDesc,
                        "PENDING",
                        BaoCaoActivity.newReportDate
                ));
            }

            displayReports(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            displayReports(new ArrayList<>());
        }
    }

    private void saveReportsToLocalCache(SharedPreferences prefs, String username, List<ReportItem> reports) {
        try {
            String key = "CACHE_REPORTS_" + username;
            Gson gson = new Gson();
            prefs.edit().putString(key, gson.toJson(reports)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayReports(List<ReportItem> reports) {
        if (reports == null || reports.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (cardBaoCaoItem1 != null) cardBaoCaoItem1.setVisibility(View.GONE);
            if (tvResultCount != null) tvResultCount.setText("0 kết quả");
            return;
        }

        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (tvResultCount != null) tvResultCount.setText(reports.size() + " kết quả");

        if (layoutReportContainer != null) {
            for (int i = layoutReportContainer.getChildCount() - 1; i >= 0; i--) {
                View child = layoutReportContainer.getChildAt(i);
                if (child != layoutEmptyState && child != cardBaoCaoItem1) {
                    layoutReportContainer.removeViewAt(i);
                }
            }
        }

        for (int i = 0; i < reports.size(); i++) {
            ReportItem item = reports.get(i);
            if (i == 0 && cardBaoCaoItem1 != null) {
                cardBaoCaoItem1.setVisibility(View.VISIBLE);
                bindReportItem(cardBaoCaoItem1, item, i + 1);
            } else {
                MaterialCardView newCard = createReportCardView(item, i + 1);
                if (layoutReportContainer != null && newCard != null) {
                    layoutReportContainer.addView(newCard);
                }
            }
        }
    }

    private void bindReportItem(View cardView, ReportItem item, int index) {
        if (cardView == null) return;

        TextView tvReportItemType = cardView.findViewById(R.id.tvReportItemType);
        TextView tvReportItemDesc = cardView.findViewById(R.id.tvReportItemDesc);
        TextView tvReportItemDate = cardView.findViewById(R.id.tvReportItemDate);

        String category = item.getScamCategory() != null && !item.getScamCategory().isEmpty()
                ? item.getScamCategory()
                : (item.getTargetType() != null ? item.getTargetType() : "Báo cáo lừa đảo");

        String desc = item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription()
                : (item.getTargetValue() != null ? "Đối tượng: " + item.getTargetValue() : "Không có mô tả thêm.");

        String rawDate = item.getCreatedAt();
        String formattedDate = rawDate;
        if (rawDate != null && rawDate.contains("T")) {
            formattedDate = rawDate.split("T")[0];
        } else if (rawDate == null || rawDate.isEmpty()) {
            formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        }

        if (tvReportItemType != null) tvReportItemType.setText(category);
        if (tvReportItemDesc != null) tvReportItemDesc.setText(desc);
        if (tvReportItemDate != null) tvReportItemDate.setText(formattedDate);

        String finalFormattedDate = formattedDate;
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(LichSuBaoCaoActivity.this, ChiTietBaoCaoActivity.class);
            intent.putExtra("REPORT_TYPE", category);
            intent.putExtra("REPORT_DESC", desc);
            intent.putExtra("REPORT_DATE", finalFormattedDate);
            startActivity(intent);
        });
    }

    private MaterialCardView createReportCardView(ReportItem item, int index) {
        MaterialCardView cardView = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setRadius(12 * getResources().getDisplayMetrics().density);
        cardView.setCardElevation(2 * getResources().getDisplayMetrics().density);
        cardView.setStrokeColor(Color.parseColor("#EAECF0"));
        cardView.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        contentLayout.setPadding(padding, padding, padding, padding);

        TextView tvCode = new TextView(this);
        tvCode.setText("BC" + String.format(Locale.getDefault(), "%03d", index));
        tvCode.setTextColor(Color.parseColor("#E53935"));
        tvCode.setTextSize(12);
        tvCode.setTypeface(null, Typeface.BOLD);
        contentLayout.addView(tvCode);

        String category = item.getScamCategory() != null && !item.getScamCategory().isEmpty()
                ? item.getScamCategory()
                : (item.getTargetType() != null ? item.getTargetType() : "Báo cáo lừa đảo");

        TextView tvType = new TextView(this);
        tvType.setText(category);
        tvType.setTextColor(Color.parseColor("#212121"));
        tvType.setTextSize(16);
        tvType.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        typeParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
        tvType.setLayoutParams(typeParams);
        contentLayout.addView(tvType);

        String desc = item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription()
                : (item.getTargetValue() != null ? "Đối tượng: " + item.getTargetValue() : "Không có mô tả thêm.");

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextColor(Color.parseColor("#757575"));
        tvDesc.setTextSize(13);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
        tvDesc.setLayoutParams(descParams);
        contentLayout.addView(tvDesc);

        String rawDate = item.getCreatedAt();
        String formattedDate = rawDate;
        if (rawDate != null && rawDate.contains("T")) {
            formattedDate = rawDate.split("T")[0];
        } else if (rawDate == null || rawDate.isEmpty()) {
            formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        }

        TextView tvDate = new TextView(this);
        tvDate.setText("Ngày gửi: " + formattedDate);
        tvDate.setTextColor(Color.parseColor("#9E9E9E"));
        tvDate.setTextSize(12);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.setMargins(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
        tvDate.setLayoutParams(dateParams);
        contentLayout.addView(tvDate);

        cardView.addView(contentLayout);

        String finalFormattedDate = formattedDate;
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(LichSuBaoCaoActivity.this, ChiTietBaoCaoActivity.class);
            intent.putExtra("REPORT_TYPE", category);
            intent.putExtra("REPORT_DESC", desc);
            intent.putExtra("REPORT_DATE", finalFormattedDate);
            startActivity(intent);
        });

        return cardView;
    }
}
