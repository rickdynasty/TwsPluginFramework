package com.tws.plugin.core.android;

import java.util.List;

import android.content.pm.ResolveInfo;
import android.os.Build;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackParceledListSlice {

	private static final String ClassName = "android.content.pm.ParceledListSlice";

	private static final String Method_getList = "getList";

	private Object instance;

	public HackParceledListSlice(Object instance) {
		this.instance = instance;
	}

	public Object getList() {
		return RefInvoker.invokeMethod(instance, ClassName, Method_getList, (Class[]) null, (Object[]) null);
	}

	public static Object newParecledListSlice(List<ResolveInfo> itemList) {
		if (Build.VERSION.SDK_INT >= 21) {
			return RefInvoker.newInstance(ClassName, new Class[] { List.class }, new Object[] { itemList });
		} else {
			return null;
		}
	}
}
