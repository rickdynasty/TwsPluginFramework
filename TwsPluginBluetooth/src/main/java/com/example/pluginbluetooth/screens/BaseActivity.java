package com.example.pluginbluetooth.screens;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

public abstract class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        updateSystemUiVisibility();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    protected String getScreenName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSystemUiVisibility();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void updateSystemUiVisibility() {
//		getWindow().getDecorView().setSystemUiVisibility(
//				View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
