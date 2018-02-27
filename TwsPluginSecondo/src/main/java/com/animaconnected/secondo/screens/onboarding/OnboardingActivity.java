package com.animaconnected.secondo.screens.onboarding;

import android.os.Bundle;
import android.view.WindowManager;

import com.animaconnected.secondo.R;
import com.animaconnected.secondo.screens.BaseActivity;

/**
 * Created by Administrator on 2018/2/27.
 */

public class OnboardingActivity extends BaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
