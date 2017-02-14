package com.tencent.tws.framework.utils;

import tws.component.log.TwsLog;
import android.content.Context;

public class HostProxy {
	private static Context g_hostContext = null;
	private static String HOST_PACKAGE_NAME = "com.tencent.tws.pluginhost";
	private static String SHARE_LIB_PACKAGE_NAME = "com.tencent.tws.sharelib";

	public static void setHostContext(Context context) {
		g_hostContext = context;
	}

	public static Context getHostContext() {
		if (g_hostContext == null) {
			// return null;
		}

		return g_hostContext;
	}
	
	public static int getHostApplicationThemeId() {
		return g_hostContext.getApplicationInfo().theme;
	}

	public static int getShareStyleId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "style", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareStyleId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareAttrId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "attr", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareAttrId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareDrawableId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "drawable", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareDrawableId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareLayoutId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "layout", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareLayoutId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareDimenId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "dimen", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareDimenId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareStringId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "string", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareStringId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareColorId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "color", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareColorId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareBoolId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "bool", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareBoolId resName=" + resName + " id=" + id);
		return id;
	}

	public static int getShareIntegerId(String resName) {
		int id = g_hostContext.getResources().getIdentifier(resName, "integer", HOST_PACKAGE_NAME);
		TwsLog.d("rick_Print:", "getShareIntegerId resName=" + resName + " id=" + id);
		return id;
	}

}
