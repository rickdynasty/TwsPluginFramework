package com.animaconnected;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.animaconnected.secondo.provider.ProviderFactory;
import com.animaconnected.secondo.screens.onboarding.OnboardingActivity;
import com.animaconnected.secondo.utils.ThreadUtils;

/**
 * Created by Administrator on 2018/2/27.
 */

public class SecondoApplication extends Application {
    private static SecondoApplication sApplication;

    public static SecondoApplication getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    public static Intent createStartApplicationIntent() {
        return new Intent(getContext(), OnboardingActivity.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sApplication = this;
    }

    public static void initialize() {
        sApplication.onInitializeCalled();
    }

    private void onInitializeCalled() {
        ThreadUtils.assertIsOnMainThread();
        // Refresh the notification criterion
//        ProviderFactory.getNotificationProvider().refresh();
//
//        if (mRemoteConfigController == null) {
//            mRemoteConfigController = RemoteConfigController.getInstance(getContext());
//            mRemoteConfigController.init();
//        }
//
//        StateNotificationReciever.newInstance(getContext());

        ProviderFactory.getStatusProvider();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

//        if (mGetDevInfoHander != null) {
//            mGetDevInfoHander.unInit();
//        }
//        if (mReceiversManager != null) {
//            mReceiversManager.unInit();
//        }
    }
}
