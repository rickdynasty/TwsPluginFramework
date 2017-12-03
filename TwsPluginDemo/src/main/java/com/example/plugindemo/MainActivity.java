package com.example.plugindemo;

import android.app.Activity;
import android.os.Bundle;

import com.rick.tws.framework.HostProxy;

import qrom.component.log.QRomLog;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QRomLog.d(TAG, "onCreate host pkg:" + HostProxy.getApplication().getPackageName());
    }
}
