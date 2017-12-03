package com.example.plugindemo.activity;

import android.app.Activity;
import android.os.Bundle;

import com.example.plugindemo.R;

/**
 * 测试透明Activity
 */
public class TransparentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
    }
}
