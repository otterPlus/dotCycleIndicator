package com.example.dotcycle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.gitfpageindicator.GiftPageIndicator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GiftPageIndicator gift_page_indictor = findViewById(R.id.gift_page_indictor);
        ViewPager viewPager = new ViewPager(this);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 20;
            }
            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return false;
            }
        });
        gift_page_indictor.setViewPager(viewPager);

        Button next = findViewById(R.id.next);
        next.setOnClickListener(v -> {
            gift_page_indictor.gotoNext();
        });
        Button previous = findViewById(R.id.previous);
        previous.setOnClickListener(v -> {
            gift_page_indictor.gotoPreivous();
        });
    }
}