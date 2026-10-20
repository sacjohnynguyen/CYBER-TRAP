package com.example.cyber_trap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class trangchu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trangchu);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Bấm Card 1 "Tra cứu lừa đảo" -> Mở TraCuuActivity
        View card1 = findViewById(R.id.card1);
        if (card1 != null) {
            card1.setOnClickListener(v -> {
                Intent intent = new Intent(trangchu.this, TraCuuActivity.class);
                startActivity(intent);
            });
        }

        // Bấm BottomNav "Tra cứu" -> Mở TraCuuActivity
        View navSearch = findViewById(R.id.navSearch);
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> {
                Intent intent = new Intent(trangchu.this, TraCuuActivity.class);
                startActivity(intent);
            });
        }
    }
}
