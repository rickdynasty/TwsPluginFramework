package com.tws.plugin.core.android;

import android.content.pm.ProviderInfo;
import android.os.Build;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by cailiming on 2017/11/27.
 */

public class HackContentProviderHolder {
    private static final String ClassName = "android.app.IActivityManager$ContentProviderHolder";
    private static final String ClassName8 = "android.app.ContentProviderHolder";

    private Object instance;

    public HackContentProviderHolder(Object instance) {
        this.instance = instance;
    }

    public static Object newInstance(ProviderInfo info) {
        if (Build.VERSION.SDK_INT <= 25) {//Build.VERSION_CODES.N_MR1
            return RefInvoker.newInstance(ClassName8, new Class[]{ProviderInfo.class}, new Object[]{info});
        } else {
            return RefInvoker.newInstance(ClassName8, new Class[]{ProviderInfo.class}, new Object[]{info});
        }
    }

}
