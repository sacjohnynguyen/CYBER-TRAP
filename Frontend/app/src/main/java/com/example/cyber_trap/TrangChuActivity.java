package com.example.cyber_trap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TrangChuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trangchu);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ScrollView scrollView = findViewById(R.id.scrollView);

        ConstraintLayout card1 = findViewById(R.id.card1);
        ConstraintLayout card2 = findViewById(R.id.card2);
        ConstraintLayout card3 = findViewById(R.id.card3);
        ConstraintLayout cardGoiKhanCap = findViewById(R.id.cardGoiKhanCap);

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navSearch = findViewById(R.id.navSearch);
        LinearLayout navBaoCao = findViewById(R.id.navBaoCao);
        LinearLayout navDienDan = findViewById(R.id.navDienDan);

        if (navHome != null && scrollView != null) {
            navHome.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));
        }

        if (card1 != null) {
            card1.setOnClickListener(v -> openMainActivityWithTab(1));
        }

        if (navSearch != null) {
            navSearch.setOnClickListener(v -> openMainActivityWithTab(1));
        }

        View.OnClickListener openDienDanListener = v -> {
            Intent intent = new Intent(TrangChuActivity.this, NewsFeedActivity.class);
            startActivity(intent);
        };

        if (card2 != null) {
            card2.setOnClickListener(openDienDanListener);
        }

        if (navDienDan != null) {
            navDienDan.setOnClickListener(openDienDanListener);
        }

        View.OnClickListener openBaoCaoListener = v -> {
            Intent intent = new Intent(TrangChuActivity.this, BaoCaoActivity.class);
            startActivity(intent);
        };

        if (card3 != null) {
            card3.setOnClickListener(openBaoCaoListener);
        }

        if (navBaoCao != null) {
            navBaoCao.setOnClickListener(openBaoCaoListener);
        }

        View.OnClickListener callListener = v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:0692345860"));
            startActivity(callIntent);
        };

        if (cardGoiKhanCap != null) {
            cardGoiKhanCap.setOnClickListener(callListener);
        }
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void openMainActivityWithTab(int tabIndex) {
        Intent intent = new Intent(TrangChuActivity.this, GiaoDienTraCuu.class);
        intent.putExtra("TAB_INDEX", tabIndex);
        startActivity(intent);
    }
}