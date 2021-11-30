package com.great.library;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.great.adssource.MainAdsView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainAdsView mainAdsView = new MainAdsView(getBaseContext());
    }
}