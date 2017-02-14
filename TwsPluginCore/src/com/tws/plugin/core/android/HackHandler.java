package com.tws.plugin.core.android;

import android.os.Handler;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackHandler {
	private static final String ClassName = "android.os.Handler";

	private static final String Field_mCallback = "mCallback";
	private Object instance;

	public HackHandler(Object instance) {
		this.instance = instance;
	}

	public void setCallback(Handler.Callback callback) {
		RefInvoker.setField(instance, ClassName, Field_mCallback, callback);
	}

}
