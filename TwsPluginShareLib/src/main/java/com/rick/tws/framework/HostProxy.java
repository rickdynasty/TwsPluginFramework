package com.rick.tws.framework;

import android.app.Application;
import android.content.Context;
import android.webkit.WebView;

import qrom.component.log.QRomLog;

public class HostProxy {
    private static final String TAG = "rick_Print:HostProxy";

    private static Application sApplication = null;

    public static void setApplication(Application context) {
        sApplication = context;
    }

    public static Application getApplication() {
        if (sApplication == null) {
            throw new IllegalStateException("框架尚未初始化，请确定在当前进程中的PluginLoader.initLoader方法已执行！");
        }
        return sApplication;
    }
}
