package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen
 */

public class HackActivityManagerNative {

	private static final String ClassName = "android.app.ActivityManagerNative";

	private static final String Method_getDefault = "getDefault";

	private static final String Field_gDefault = "gDefault";

	public static Object getDefault() {
		return RefInvoker.invokeMethod(null, ClassName, Method_getDefault, (Class[]) null, (Object[]) null);
	}

	public static Object getGDefault() {
		return RefInvoker.getField(null, ClassName, Field_gDefault);
	}

	public static void setGDefault(Object gDefault) {
		RefInvoker.setField(null, ClassName, Field_gDefault, gDefault);
	}
}
