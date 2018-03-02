package com.example.pluginbluetooth.screens;

import android.content.Intent;

public class BasePresenter {

    private ActivityLauncher mActivityLauncher;
    private MainController mMainController;

    public BasePresenter(ActivityLauncher activityLauncher, MainController mainController) {
        mActivityLauncher = activityLauncher;
        mMainController = mainController;
    }

    protected MainController getMainController() {
        return mMainController;
    }

    public interface ActivityLauncher {

        void startActivity(Intent intent);

        void startActivityForResult(Intent intent, int requestCode);
    }

    protected void startActivity(Intent intent) {
        mActivityLauncher.startActivity(intent);
    }

    protected void startActivityForResult(Intent intent, int requestCode) {
        mActivityLauncher.startActivityForResult(intent, requestCode);
    }

    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    }
}
