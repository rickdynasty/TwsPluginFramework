package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackContentProviderClient {
	private static final String ClassName = "android.content.ContentProviderClient";

	private static final String Field_mContentProvider = "mContentProvider";

	private Object instance;

	public HackContentProviderClient(Object instance) {
		this.instance = instance;
	}

	public Object getContentProvider() {
		return RefInvoker.getField(instance, ClassName, Field_mContentProvider);
	}

}
