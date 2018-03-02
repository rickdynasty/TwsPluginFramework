package com.example.pluginbluetooth;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2018/3/1.
 */

public class SecondoApplication extends Application {
    private static SecondoApplication sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    public static SecondoApplication getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }
}
