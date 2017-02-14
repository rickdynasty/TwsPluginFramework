package com.tws.plugin.core.android;

import android.content.pm.PackageInfo;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackWebViewFactory {

	private static final String ClassName = "android.webkit.WebViewFactory";

	private static final String Field_sProviderInstance = "sProviderInstance";

	private static final String Method_getProvider = "getProvider";
	private static final String Method_getLoadedPackageInfo = "getLoadedPackageInfo";

	public static Object getProvider() {
		return RefInvoker.invokeMethod(null, ClassName, Method_getProvider, (Class[]) null, (Object[]) null);
	}

	public static void setProviderInstance(Object provider) {
		RefInvoker.setField(null, ClassName, Field_sProviderInstance, provider);
	}

	public static PackageInfo getLoadedPackageInfo() {
		return (PackageInfo) RefInvoker.invokeMethod(null, ClassName, Method_getLoadedPackageInfo, (Class[]) null,
				(Object[]) null);
	}
}
