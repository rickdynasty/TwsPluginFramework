package com.tws.plugin.core.android;

import android.content.ComponentName;

import com.tws.plugin.util.RefInvoker;


/**
 * Created by yongchen
 */

public class HackComponentName extends HackContextThemeWrapper {
    private static final String ClassName = ComponentName.class.getName();
    private static final String Field_mPackage = "mPackage";
    private static final String Field_mClass = "mClass";

    public HackComponentName(Object instance) {
        super(instance);
    }

    public final void setPackageName(String packageName) {
        RefInvoker.setField(instance, ClassName, Field_mPackage, packageName);
    }

    public final void setClassName(String className) {
        RefInvoker.setField(instance, ClassName, Field_mClass, className);
    }
}
