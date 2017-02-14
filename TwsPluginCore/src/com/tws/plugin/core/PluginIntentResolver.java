package com.tws.plugin.core;

import java.util.ArrayList;

import tws.component.log.TwsLog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import com.tws.plugin.content.ComponentInfo;
import com.tws.plugin.content.PluginActivityInfo;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.content.PluginReceiverIntent;
import com.tws.plugin.core.android.HackCreateServiceData;
import com.tws.plugin.core.android.HackReceiverData;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;

public class PluginIntentResolver {
	private static final String TAG = "rick_Print:PluginIntentResolver";

	public static final String CLASS_SEPARATOR = "@";// 字符串越短,判断时效率越高
	public static final String CLASS_PREFIX_RECEIVER = "#";// 字符串越短,判断时效率越高
	public static final String CLASS_PREFIX_SERVICE = "%";// 字符串越短,判断时效率越高

	public static final String INTENT_EXTRA_PID = "extra_plugin_packagename";
	// 保存跳转的跳板
	public static final String INTENT_EXTRA_BRIDGE_RAMP = "extra_bridge_ramp";
	// 保存跳转的目的
	public static final String INTENT_EXTRA_BRIDGE_TO_PLUGIN = "extra_bridge_to_plugin";

	public static void resolveService(Intent intent) {
		ArrayList<ComponentInfo> componentInfos = PluginLoader.matchPlugin(intent, PluginDescriptor.SERVICE,
				PluginLoader.getPackageName(intent));
		if (componentInfos != null && componentInfos.size() > 0) {
			String stubServiceName = PluginManagerHelper.bindStubService(componentInfos.get(0).name,
					componentInfos.get(0).processName);
			if (stubServiceName != null) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), stubServiceName));
			}
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}
	}

	public static ArrayList<Intent> resolveReceiver(final Intent intent) {
		// 如果在插件中发现了匹配intent的receiver项目，替换掉ClassLoader
		// 不需要在这里记录目标className，className将在Intent中传递
		ArrayList<Intent> result = new ArrayList<Intent>();
		ArrayList<ComponentInfo> componentInfos = PluginLoader.matchPlugin(intent, PluginDescriptor.BROADCAST,
				PluginLoader.getPackageName(intent));
		if (componentInfos != null && componentInfos.size() > 0) {
			for (ComponentInfo info : componentInfos) {
				Intent newIntent = new Intent(intent);
				newIntent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(),
						PluginManagerHelper.bindStubReceiver()));
				// hackReceiverForClassLoader检测到这个标记后会进行替换
				newIntent.setAction(info.name + CLASS_SEPARATOR
						+ (intent.getAction() == null ? "" : intent.getAction()));
				result.add(newIntent);
			}
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}

		// fix 插件中对同一个广播同时注册了动态和静态广播的情况
		result.add(intent);

		return result;
	}

	/* package */static Class resolveReceiverForClassLoader(final Object msgObj) {
		HackReceiverData hackReceiverData = new HackReceiverData(msgObj);
		Intent intent = hackReceiverData.getIntent();
		if (intent.getComponent().getClassName().equals(PluginManagerHelper.bindStubReceiver())) {
			String action = intent.getAction();
			TwsLog.d(TAG, "action:" + action);
			if (action != null) {
				String[] targetClassName = action.split(CLASS_SEPARATOR);
				@SuppressWarnings("rawtypes")
				Class clazz = PluginLoader.loadPluginClassByName(targetClassName[0]);
				if (clazz != null) {
					intent.setExtrasClassLoader(clazz.getClassLoader());
					// 由于之前intent被修改过 这里再吧Intent还原到原始的intent
					if (targetClassName.length > 1) {
						intent.setAction(targetClassName[1]);
					} else {
						intent.setAction(null);
					}
				}
				// PluginClassLoader检测到这个特殊标记后会进行替换
				intent.setComponent(new ComponentName(intent.getComponent().getPackageName(), CLASS_PREFIX_RECEIVER
						+ targetClassName[0]));

				if (Build.VERSION.SDK_INT >= 21) {
					if (intent.getExtras() != null) {
						hackReceiverData.setIntent(new PluginReceiverIntent(intent));
					}
				}

				return clazz;
			}
		}
		return null;
	}

	/* package */static String resolveServiceForClassLoader(Object msgObj) {

		HackCreateServiceData hackCreateServiceData = new HackCreateServiceData(msgObj);
		ServiceInfo info = hackCreateServiceData.getInfo();

		if (ProcessUtil.isPluginProcess()) {

			PluginInjector.hackHostClassLoaderIfNeeded();

			// 通过映射查找
			String targetClassName = PluginManagerHelper.getBindedPluginServiceName(info.name);
			// TODO 或许可以通过这个方式来处理service
			// info.applicationInfo = XXX

			TwsLog.d(TAG, "hackServiceName=" + info.name + " packageName=" + info.packageName + " processName="
					+ info.processName + " targetClassName=" + targetClassName + " applicationInfo.packageName="
					+ info.applicationInfo.packageName);

			if (targetClassName != null) {
				info.name = CLASS_PREFIX_SERVICE + targetClassName;
			} else if (PluginManagerHelper.isStub(info.name)) {
				String dumpString = PluginManagerHelper.dumpServiceInfo();
				TwsLog.e(TAG, "hackServiceName 没有找到映射关系, 可能映射表出了异常 info.name=" + info.name + " dumpString="
						+ dumpString);

				info.name = CLASS_PREFIX_SERVICE + "null";
			} else {
				TwsLog.d(TAG, "是宿主service:" + info.name);
			}
		}

		return info.name;
	}

	public static void resolveActivity(Intent intent) {
		String packageName = PluginLoader.getPackageName(intent);
		// 如果在插件中发现Intent的匹配项，记下匹配的插件Activity的ClassName
		ArrayList<ComponentInfo> componentInfos = PluginLoader.matchPlugin(intent, PluginDescriptor.ACTIVITY,
				packageName);
		if (componentInfos != null && componentInfos.size() > 0) {
			String className = componentInfos.get(0).name;
			final String applicationPackageName = PluginLoader.getApplication().getPackageName();
			PluginDescriptor pd = (TextUtils.isEmpty(packageName) || applicationPackageName.equals(packageName)) ? PluginManagerHelper
					.getPluginDescriptorByClassName(className) : PluginManagerHelper
					.getPluginDescriptorByPluginId(packageName);

			if (pd != null) {
				packageName = pd.getPackageName();
			}

			PluginActivityInfo pluginActivityInfo = pd.getActivityInfos().get(className);

			String stubActivityName = PluginManagerHelper.bindStubActivity(className,
					Integer.parseInt(pluginActivityInfo.getLaunchMode()));

			intent.setComponent(new ComponentName(applicationPackageName, stubActivityName));
			intent.putExtra(INTENT_EXTRA_PID, packageName);
			// PluginInstrumentationWrapper检测到这个标记后会进行替换
			intent.setAction(className + CLASS_SEPARATOR + (intent.getAction() == null ? "" : intent.getAction()));
		} else {
			if (intent.getComponent() != null
					&& null != PluginManagerHelper
							.getPluginDescriptorByPluginId(intent.getComponent().getPackageName())) {
				intent.setComponent(new ComponentName(PluginLoader.getApplication().getPackageName(), intent
						.getComponent().getClassName()));
			}
		}
	}

	/* package */static void resolveActivity(Intent[] intent) {
		// 不常用。需要时再实现此方法，
	}

}
