package com.tencent.tws.framework.utils;

import com.tencent.tws.sharelib.R;

import tws.component.log.TwsLog;
import android.app.Application;
import android.content.Context;
import android.webkit.WebView;

public class HostProxy {
	private static final String TAG = "rick_Print:HostProxy";

	private static Application sApplication = null;
	private static HomeUIProxy sHomeUIProxy = null;
	private static String HOST_PACKAGE_NAME = "com.tencent.tws.pluginhost";

	public static void setApplication(Application context) {
		sApplication = context;
	}

	public static Application getApplication() {
		if (sApplication == null) {
			throw new IllegalStateException("框架尚未初始化，请确定在当前进程中的PluginLoader.initLoader方法已执行！");
		}
		return sApplication;
	}

	public static void setHomeUIProxy(HomeUIProxy uiProxy) {
		sHomeUIProxy = uiProxy;
	}

	public static HomeUIProxy getHomeUIProxy() {
		return sHomeUIProxy;
	}

	public static int getHostApplicationThemeId() {
		return sApplication.getApplicationInfo().theme;
	}

	public static int getShareStyleId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "style", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareStyleId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareAttrId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "attr", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareAttrId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareDrawableId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "drawable", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareDrawableId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareLayoutId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "layout", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareLayoutId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareDimenId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "dimen", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareDimenId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}
	
	public static int getApplicationIconId() {
		return R.drawable.ic_launcher;
	}

	public static int getShareStringId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "string", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareStringId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareColorId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "color", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareColorId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareBoolId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "bool", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareBoolId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	public static int getShareIntegerId(String resName) {
		int id = sApplication.getResources().getIdentifier(resName, "integer", HOST_PACKAGE_NAME);
		TwsLog.d(TAG, "getShareIntegerId resName=" + resName + " id=0x" + Integer.toHexString(id));
		return id;
	}

	/**
	 * 这个方法是为了解决: 目前插件是共享一个进程的, 而Webview的全局Context是进程唯一的。
	 * 要让哪个插件能加载插件自己的Assets目录下的本地HTML, 就的将Webview的全局Context设置为哪个插件的AppContext
	 * 
	 * 但是当有多个插件在自己的Assets目录下的存在本地HTML文件时, Webview的全局Context无论设置为哪个插件的AppContext,
	 * 都会导致另外一个插件的Asest下的HTML文件加载不出来。
	 * 
	 * 因此每次切换Activity的时候都尝试将Webview的全局Context切换到当前Activity所在的AppContext
	 * 
	 * @param pluginActivity
	 */
	public static void switchWebViewContext(Context pluginActivity) {
		TwsLog.d(TAG, "尝试切换WebView Context, 不同的WebView内核, 实现方式可能不同, 本方法基于Chrome的WebView实现");
		try {
			/**
			 * webviewProvider获取过程： new WebView()
			 * ->WebViewFactory.getProvider().createWebView(this, new
			 * PrivateAccess()).init() ->loadChromiumProvider ->
			 * PathClassLoader("/system/framework/webviewchromium.jar")
			 * .forName(
			 * "com.android.webviewchromium.WebViewChromiumFactoryProvider") ->
			 * BootLoader.forName(android.webkit.WebViewClassic$Factory) ->new
			 * WebViewClassic.Factory()
			 */
			WebView wb = new WebView(pluginActivity);
			wb.loadUrl("");// 触发代理对象AndroidWebkitWebViewFactoryProvider里面的fixWebViewAsset方法
			wb.destroy();
		} catch (NullPointerException e) {
			e.printStackTrace();
			TwsLog.e(TAG, "插件Application对象尚未初始化会触发NPE，如果是异步初始化插件，应等待异步初始化完成再进入插件");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
