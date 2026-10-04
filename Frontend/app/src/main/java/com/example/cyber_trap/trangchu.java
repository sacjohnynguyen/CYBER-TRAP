package com.example.s2;

import android.content.Intent;
import android.net.Uri;
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

        // Xử lý giao diện tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --------------------------------------------------------
        // 1. XỬ LÝ CHUYỂN TRANG
        // --------------------------------------------------------
        View cardTraCuu = findViewById(R.id.card1);

        // Chỉ xử lý nhấn vào mục "Tra cứu lừa đảo"
        cardTraCuu.setOnClickListener(v -> {
            Intent intent = new Intent(trangchu.this, TraCuuCanhBaoActivity.class);
            intent.putExtra("TAB_INDEX", 0); // Gửi cờ vị trí số 0 (Tab Tra Cứu)
            startActivity(intent);
        });

        // Tạm thời đóng băng tính năng của thẻ "Cảnh báo mới" để làm sau
        /*
        View cardCanhBao = findViewById(R.id.card2);
        cardCanhBao.setOnClickListener(v -> {
            Intent intent = new Intent(trangchu.this, TraCuuCanhBaoActivity.class);
            intent.putExtra("TAB_INDEX", 1);
            startActivity(intent);
        });
        */

        // --------------------------------------------------------
        // 2. SỰ KIỆN GỌI ĐIỆN THOẠI KHẨN CẤP
        // --------------------------------------------------------
        View cardGoiKhanCap = findViewById(R.id.cardGoiKhanCap);

        cardGoiKhanCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Số điện thoại đường dây nóng
                String phoneInput = "0692345860";

                // Sử dụng cấu trúc Intent của bài giảng
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneInput));
                startActivity(intent);
            }
        });
    }
}