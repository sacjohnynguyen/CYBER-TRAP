package com.example.cyber_trap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class trangchu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trangchu);

        ScrollView scrollView = findViewById(R.id.scrollView);

        // 1. Ánh xạ các thẻ tính năng
        ConstraintLayout card1 = findViewById(R.id.card1);
        ConstraintLayout card2 = findViewById(R.id.card2);
        ConstraintLayout card3 = findViewById(R.id.card3);
        ConstraintLayout cardGoiKhanCap = findViewById(R.id.cardGoiKhanCap);

        // 2. Ánh xạ thanh điều hướng bên dưới
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navSearch = findViewById(R.id.navSearch);
        LinearLayout navCall = findViewById(R.id.navCall);

        if (navHome != null && scrollView != null) {
            navHome.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));
        }

        // Sự kiện mở trang Tra cứu lừa đảo theo Tab
        if (card1 != null) {
            card1.setOnClickListener(v -> openMainActivityWithTab(1));
        }
        if (card2 != null) {
            card2.setOnClickListener(v -> openMainActivityWithTab(3));
        }
        if (card3 != null) {
            card3.setOnClickListener(v -> openMainActivityWithTab(2));
        }
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> openMainActivityWithTab(1));
        }

        // Sự kiện gọi điện khẩn cấp
        View.OnClickListener callListener = v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:0692345860"));
            startActivity(callIntent);
        };

        if (cardGoiKhanCap != null) {
            cardGoiKhanCap.setOnClickListener(callListener);
        }
        if (navCall != null) {
            navCall.setOnClickListener(callListener);
        }
    }

    private void openMainActivityWithTab(int tabIndex) {
        Intent intent = new Intent(trangchu.this, GiaoDienTraCuu.class);
        intent.putExtra("TAB_INDEX", tabIndex);
        startActivity(intent);
    }
}