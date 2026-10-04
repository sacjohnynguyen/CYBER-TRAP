package com.example.s2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TraCuuCanhBaoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tra_cuu_canh_bao);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Set Adapter cho ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Liên kết TabLayout và ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("🔍 Kiểm Tra");
                    break;
                case 1:
                    tab.setText("⚠️ Cảnh Báo");
                    break;
            }
        }).attach();
        // Lấy cờ vị trí tab từ Intent và chuyển ViewPager đến đúng màn hình
        int selectedTab = getIntent().getIntExtra("TAB_INDEX", 0);
        viewPager.setCurrentItem(selectedTab);
    }
}