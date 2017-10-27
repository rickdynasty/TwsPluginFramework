package com.tws.plugin.core.android;

import android.content.Intent;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackPendingIntent {

	private static final String ClassName = "android.app.PendingIntent";

	private static final String Method_getIntent = "getIntent";

	private Object instance;

	public HackPendingIntent(Object instance) {
		this.instance = instance;
	}

	public Intent getIntent() {
		return (Intent) RefInvoker.invokeMethod(instance, ClassName, Method_getIntent, (Class[]) null, (Object[]) null);
	}
}
