package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackAppBindData {
	private static final String ClassName = "android.app.ActivityThread$AppBindData";

	private static final String Field_compatInfo = "compatInfo";
	private static final String Field_info = "info";

	private Object instance;

	public HackAppBindData(Object instance) {
		this.instance = instance;
	}

	public Object getInfo() {
		return RefInvoker.getField(instance, ClassName, Field_info);
	}

	public Object getCompatInfo() {
		return RefInvoker.getField(instance, ClassName, Field_compatInfo);
	}

}
