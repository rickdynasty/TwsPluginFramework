package com.tws.plugin.core.proxy.systemservice;

import java.lang.reflect.Method;

import tws.component.log.TwsLog;
import android.view.WindowManager;

import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.ProxyUtil;

public class AndroidViewWindowManager extends MethodDelegate {

	private static final String TAG = "rick_Print:AndroidViewWindowManager";

	public static WindowManager installProxy(Object invokeResult) {
		TwsLog.d(TAG, "安装AndroidViewWindowManagerProxy");
		WindowManager windowManager = (WindowManager) ProxyUtil.createProxy(invokeResult,
				new AndroidViewWindowManager());
		TwsLog.d(TAG, "安装完成");
		return windowManager;
	}

	@Override
	public Object beforeInvoke(Object target, Method method, Object[] args) {
		if (args != null) {
			fixPackageName(method.getName(), args);
		}
		return super.beforeInvoke(target, method, args);
	}

	private void fixPackageName(String methodName, Object[] args) {
		if (methodName.equals("addView") || methodName.equals("updateViewLayout")) {
			for (Object object : args) {
				if (object instanceof WindowManager.LayoutParams) {
					TwsLog.d(TAG, "修正WindowManager " + methodName + "方法参数中的packageName:"
							+ ((WindowManager.LayoutParams) object).packageName);
					((WindowManager.LayoutParams) object).packageName = PluginLoader.getApplication().getPackageName();
				}
			}
		}
	}

}
