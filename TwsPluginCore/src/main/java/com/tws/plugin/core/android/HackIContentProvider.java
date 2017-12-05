package com.tws.plugin.core.android;

import android.os.Bundle;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackIContentProvider {
	private static final String ClassName = "android.content.IContentProvider";

	private static final String Methdo_call = "call";

	private Object instance;

	public HackIContentProvider(Object instance) {
		this.instance = instance;
	}

	public Object call(String method, String arg, Bundle extras) {
		return RefInvoker.invokeMethod(instance, ClassName, Methdo_call, new Class[] { String.class, String.class, Bundle.class }, new Object[] { method, arg, extras });
	}
}
