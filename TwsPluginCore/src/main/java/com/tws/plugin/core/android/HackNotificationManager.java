package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackNotificationManager {

	private static final String ClassName = "android.app.NotificationManager";

	private static final String Method_getService = "getService";

	private static final String Field_sService = "sService";

	public static Object getService() {
		return RefInvoker.invokeMethod(null, ClassName, Method_getService, (Class[]) null, (Object[]) null);
	}

	public static void setService(Object serv) {
		RefInvoker.setField(null, ClassName, Field_sService, serv);
	}
}
