package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 17/5/18.
 * for 8.0 — gDefault这个变量在8.0被移到了ActivityManager类里面了
 */

public class HackActivityManager {

    private static final String ClassName = "android.app.ActivityManager";

    private static final String Field_IActivityManagerSingleton = "IActivityManagerSingleton";

    public static Object getIActivityManagerSingleton() {
        return RefInvoker.getField(null, ClassName, Field_IActivityManagerSingleton);
    }

    public static Object setIActivityManagerSingleton(Object activityManagerSingleton) {
       return RefInvoker.getField(null, ClassName, Field_IActivityManagerSingleton);
    }

}
