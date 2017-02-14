package com.tws.plugin.core.proxy.systemservice;

import java.lang.reflect.Method;

import tws.component.log.TwsLog;
import android.view.WindowManager;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.ProxyUtil;
import com.tws.plugin.manager.PluginManagerHelper;

public class AndroidViewIWindowSession extends MethodDelegate {

	private static final String TAG = "rick_Print:AndroidViewIWindowSession";

	public static Object installProxy(Object invokeResult) {
		TwsLog.d(TAG, "安装AndroidViewIWindowSessionProxy");
		Object iWindowSessionProxy = ProxyUtil.createProxy(invokeResult, new AndroidViewIWindowSession());
		TwsLog.d(TAG, "安装完成");
		return iWindowSessionProxy;
	}

	@Override
	public Object beforeInvoke(Object target, Method method, Object[] args) {
		if (args != null) {
			fixPackageName(method.getName(), args);
		}
		return super.beforeInvoke(target, method, args);
	}

	private void fixPackageName(String methodName, Object[] args) {
		for (Object object : args) {
			if (object != null && object instanceof WindowManager.LayoutParams) {

				WindowManager.LayoutParams params = ((WindowManager.LayoutParams) object);

				if (params.packageName != null
						&& !params.packageName.equals(PluginLoader.getApplication().getPackageName())) {

					// 尝试读取插件, 注意, 这个方法调用会触发ContentProvider调用
					PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByPluginId(params.packageName);
					if (pd != null) {
						TwsLog.d(TAG, "修正System api:" + methodName + " WindowManager.LayoutParams.packageName参数为宿主包名:"
								+ params.packageName);
						// 这里修正packageName会引起弹PopupWindow时发生WindowManager异常，
						// TODO 此处暂不修正，原因待查
						// params.packageName =
						// PluginLoader.getApplication().getPackageName();
					}
				}
			}
		}
	}

}
