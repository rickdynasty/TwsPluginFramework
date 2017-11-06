package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackSingleton {
	private static final String ClassName = "android.util.Singleton";

	private static final String Field_mInstance = "mInstance";

	private Object instance;

	public HackSingleton(Object instance) {
		this.instance = instance;
	}

	public void setInstance(Object object) {
		RefInvoker.setField(instance, ClassName, Field_mInstance, object);

	}
}
