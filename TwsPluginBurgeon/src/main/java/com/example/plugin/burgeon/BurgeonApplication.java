package com.example.plugin.burgeon;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by Administrator on 2018/1/21 0021.
 */
@SuppressLint("LongLogTag")
public class BurgeonApplication extends Application {
    private static final String TAG = "rick_Print:BurgeonApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.i(TAG, "attachBaseContext");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }
}
