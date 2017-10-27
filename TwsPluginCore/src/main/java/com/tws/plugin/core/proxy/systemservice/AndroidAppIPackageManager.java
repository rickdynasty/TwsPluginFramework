package com.tws.plugin.core.proxy.systemservice;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qrom.component.log.QRomLog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import com.tws.plugin.content.ComponentInfo;
import com.tws.plugin.content.PluginActivityInfo;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.content.PluginProviderInfo;
import com.tws.plugin.core.PluginIntentResolver;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.android.HackActivityThread;
import com.tws.plugin.core.android.HackApplicationPackageManager;
import com.tws.plugin.core.android.HackParceledListSlice;
import com.tws.plugin.core.proxy.MethodDelegate;
import com.tws.plugin.core.proxy.MethodProxy;
import com.tws.plugin.core.proxy.ProxyUtil;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.manager.PluginManagerProvider;
import com.tws.plugin.util.ResourceUtil;

/**
 * @author yongchen
 */
public class AndroidAppIPackageManager extends MethodProxy {

	private static final String TAG = "rick_Print:AndroidAppIPackageManager";

	static {
		sMethods.put("getInstalledPackages", new getInstalledPackages());
		sMethods.put("getPackageInfo", new getPackageInfo());
		sMethods.put("getApplicationInfo", new getApplicationInfo());
		sMethods.put("getActivityInfo", new getActivityInfo());
		sMethods.put("getReceiverInfo", new getReceiverInfo());
		sMethods.put("getServiceInfo", new getServiceInfo());
		sMethods.put("getProviderInfo", new getProviderInfo());
		sMethods.put("queryIntentActivities", new queryIntentActivities());
		sMethods.put("queryIntentServices", new queryIntentServices());
		sMethods.put("resolveIntent", new resolveIntent());
		sMethods.put("resolveService", new resolveService());
		sMethods.put("getComponentEnabledSetting", new getComponentEnabledSetting());
	}

	public static void installProxy(PackageManager manager) {
		QRomLog.d(TAG, "安装PackageManagerProxy");
		Object androidAppIPackageManagerStubProxy = HackActivityThread.getPackageManager();
		Object androidAppIPackageManagerStubProxyProxy = ProxyUtil.createProxy(androidAppIPackageManagerStubProxy,
				new AndroidAppIPackageManager());
		HackActivityThread.setPackageManager(androidAppIPackageManagerStubProxyProxy);
		HackApplicationPackageManager hackApplicationPackageManager = new HackApplicationPackageManager(manager);
		hackApplicationPackageManager.setPM(androidAppIPackageManagerStubProxyProxy);
		QRomLog.d(TAG, "安装完成");
	}

	public static class getPackageInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			String packageName = (String) args[0];
			QRomLog.d(TAG, "beforeInvoke method:" + method.getName() + " packageName:" + packageName);
			if (!packageName.equals(PluginLoader.getApplication().getPackageName())) {
				PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
				if (pluginDescriptor != null) {
					PackageInfo packageInfo = PluginLoader.getApplication().getPackageManager()
							.getPackageArchiveInfo(pluginDescriptor.getInstalledPath(), (Integer) args[1]);
					if (packageInfo.applicationInfo != null) {
						packageInfo.applicationInfo.sourceDir = pluginDescriptor.getInstalledPath();
						packageInfo.applicationInfo.publicSourceDir = pluginDescriptor.getInstalledPath();
					}
					return packageInfo;
				}
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	public static class getInstalledPackages extends MethodDelegate {

		@Override
		public Object afterInvoke(Object target, Method method, Object[] args, Object beforeResult, Object invokeResult) {
			QRomLog.d(TAG, "afterInvoke method:" + method.getName());

			if (Build.VERSION.SDK_INT >= 18) {// android4.3
				Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
				if (plugins != null) {
					List<PackageInfo> resultList = (List<PackageInfo>) new HackParceledListSlice(invokeResult)
							.getList();
					if (resultList != null) {
						for (PluginDescriptor pluginDescriptor : plugins) {
							PackageInfo packageInfo = PluginLoader.getApplication().getPackageManager()
									.getPackageArchiveInfo(pluginDescriptor.getInstalledPath(), (Integer) args[0]);
							if (packageInfo.applicationInfo != null) {
								packageInfo.applicationInfo.sourceDir = pluginDescriptor.getInstalledPath();
								packageInfo.applicationInfo.publicSourceDir = pluginDescriptor.getInstalledPath();
							}
							resultList.add(packageInfo);
						}
					}
				}
			} else {
			}
			return invokeResult;
		}
	}

	public static class queryIntentActivities extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "beforeInvoke method:" + method.getName());
			final String packageName = PluginLoader.getPackageName((Intent) args[0]);
			ArrayList<ComponentInfo> componentInfos = PluginIntentResolver.matchPlugin((Intent) args[0],
					PluginDescriptor.ACTIVITY, packageName);
			if (componentInfos != null && componentInfos.size() > 0) {
				PluginDescriptor pluginDescriptor = PluginLoader.getApplication().getPackageName().equals(packageName) ? PluginManagerHelper
						.getPluginDescriptorByClassName(componentInfos.get(0).name) : PluginManagerHelper
						.getPluginDescriptorByPluginId(packageName);

				if (pluginDescriptor != null) {
					if (Build.VERSION.SDK_INT <= 23) {
						List<ResolveInfo> result = new ArrayList<ResolveInfo>();
						ResolveInfo info = new ResolveInfo();
						result.add(info);
						info.activityInfo = getActivityInfo(pluginDescriptor, componentInfos.get(0).name);
						return result;
					} else {
						// 高于7.0的版本应当返回的类型是 android.content.pm.ParceledListSlice
						ArrayList<ResolveInfo> resultList = new ArrayList<ResolveInfo>();
						ResolveInfo info = new ResolveInfo();
						resultList.add(info);
						info.activityInfo = getActivityInfo(pluginDescriptor, componentInfos.get(0).name);
						Object parceledListSlice = HackParceledListSlice.newParecledListSlice(resultList);
						return parceledListSlice;
					}
				}
			}
			return super.beforeInvoke(target, method, args);
		}

	}

	public static class getApplicationInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			String packageName = (String) args[0];
			QRomLog.d(TAG, "beforeInvoke method:" + method.getName() + " packageName:" + packageName);
			if (!packageName.equals(PluginLoader.getApplication().getPackageName())) {
				PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
				if (pluginDescriptor != null) {
					return getApplicationInfo(pluginDescriptor);
				}
			} else {
				QRomLog.w(TAG, "注意：使用了宿主包名：" + packageName);
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	public static class getActivityInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "getActivityInfo beforeInvoke method:" + method.getName());
			String className = ((ComponentName) args[0]).getClassName();
			PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);
			if (pluginDescriptor != null) {
				return getActivityInfo(pluginDescriptor, className);
			}
			return super.beforeInvoke(target, method, args);
		}

	}

	public static class getReceiverInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "getReceiverInfo beforeInvoke method:" + method.getName());
			String className = ((ComponentName) args[0]).getClassName();
			PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);
			if (pluginDescriptor != null) {
				return getActivityInfo(pluginDescriptor, className);
			}

			return super.beforeInvoke(target, method, args);
		}

	}

	public static class getServiceInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "getServiceInfo beforeInvoke method:" + method.getName());
			String className = ((ComponentName) args[0]).getClassName();
			PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);
			if (pluginDescriptor != null) {
				return getServiceInfo(pluginDescriptor, className);
			}

			return super.beforeInvoke(target, method, args);
		}
	}

	public static class getProviderInfo extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "getProviderInfo beforeInvoke method:" + method.getName());
			String className = ((ComponentName) args[0]).getClassName();
			if (!className.equals(PluginManagerProvider.class.getName())) {
				PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);
				if (pluginDescriptor != null) {
					PluginProviderInfo info = pluginDescriptor.getProviderInfos().get(className);
					ProviderInfo providerInfo = new ProviderInfo();
					providerInfo.name = info.getName();
					providerInfo.packageName = getPackageName(pluginDescriptor);
					providerInfo.icon = pluginDescriptor.getApplicationIcon();
					providerInfo.metaData = pluginDescriptor.getMetaData();
					providerInfo.enabled = true;
					providerInfo.exported = info.isExported();
					providerInfo.applicationInfo = getApplicationInfo(pluginDescriptor);
					providerInfo.authority = info.getAuthority();
					return providerInfo;
				}
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	public static class queryIntentServices extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "queryIntentServices beforeInvoke method:" + method.getName());
			final String packageName = PluginLoader.getPackageName((Intent) args[0]);
			ArrayList<ComponentInfo> componentInfos = PluginIntentResolver.matchPlugin((Intent) args[0],
					PluginDescriptor.SERVICE, packageName);
			if (componentInfos != null && componentInfos.size() > 0) {
				PluginDescriptor pluginDescriptor = PluginLoader.getApplication().getPackageName().equals(packageName) ? PluginManagerHelper
						.getPluginDescriptorByClassName(componentInfos.get(0).name) : PluginManagerHelper
						.getPluginDescriptorByPluginId(packageName);

				if (pluginDescriptor != null) {
					if (Build.VERSION.SDK_INT <= 23) {
						List<ResolveInfo> result = new ArrayList<ResolveInfo>();
						ResolveInfo info = new ResolveInfo();
						result.add(info);
						info.serviceInfo = getServiceInfo(pluginDescriptor, componentInfos.get(0).name);
						return result;
					} else {
						// 高于7.0的版本应当返回的类型是 android.content.pm.ParceledListSlice
						ArrayList<ResolveInfo> resultList = new ArrayList<ResolveInfo>();
						ResolveInfo info = new ResolveInfo();
						resultList.add(info);
						info.serviceInfo = getServiceInfo(pluginDescriptor, componentInfos.get(0).name);
						Object parceledListSlice = HackParceledListSlice.newParecledListSlice(resultList);
						return parceledListSlice;
					}
				}
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	// ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags,
	// int userId);
	public static class resolveIntent extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "resolveIntent beforeInvoke method:" + method.getName());
			final String packageName = PluginLoader.getPackageName((Intent) args[0]);
			ArrayList<ComponentInfo> componentInfos = PluginIntentResolver.matchPlugin((Intent) args[0],
					PluginDescriptor.ACTIVITY, packageName);
			if (componentInfos != null && componentInfos.size() > 0) {
				PluginDescriptor pluginDescriptor = PluginLoader.getApplication().getPackageName().equals(packageName) ? PluginManagerHelper
						.getPluginDescriptorByClassName(componentInfos.get(0).name) : PluginManagerHelper
						.getPluginDescriptorByPluginId(packageName);

				if (pluginDescriptor != null) {
					ResolveInfo info = new ResolveInfo();
					info.activityInfo = getActivityInfo(pluginDescriptor, componentInfos.get(0).name);
					return info;
				}
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	public static class resolveService extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			QRomLog.d(TAG, "resolveService beforeInvoke method:" + method.getName());
			final String packageName = PluginLoader.getPackageName((Intent) args[0]);
			ArrayList<ComponentInfo> componentInfos = PluginIntentResolver.matchPlugin((Intent) args[0],
					PluginDescriptor.SERVICE, packageName);
			if (componentInfos != null && componentInfos.size() > 0) {
				PluginDescriptor pluginDescriptor = PluginLoader.getApplication().getPackageName().equals(packageName) ? PluginManagerHelper
						.getPluginDescriptorByClassName(componentInfos.get(0).name) : PluginManagerHelper
						.getPluginDescriptorByPluginId(packageName);

				if (pluginDescriptor != null) {
					ResolveInfo info = new ResolveInfo();
					info.serviceInfo = getServiceInfo(pluginDescriptor, componentInfos.get(0).name);
					return info;
				}
			}
			return super.beforeInvoke(target, method, args);
		}
	}

	public static class getComponentEnabledSetting extends MethodDelegate {
		@Override
		public Object beforeInvoke(Object target, Method method, Object[] args) {
			Object arg0 = args[0];
			if (arg0 instanceof ComponentName) {
				ComponentName mComponentName = ((ComponentName) args[0]);

				QRomLog.d(TAG, "getComponentEnabledSetting beforeInvoke method:" + method.getName() + " PackageName:"
						+ mComponentName.getPackageName() + " ClassName:" + mComponentName.getClassName());

				if ("com.htc.android.htcsetupwizard".equalsIgnoreCase(mComponentName.getPackageName())) {
					return PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
				}
			} else {
				QRomLog.d(TAG, "getComponentEnabledSetting beforeInvoke method:" + method.getName() + " arg0 is " + arg0);
			}

			return super.beforeInvoke(target, method, args);
		}
	}

	private static ApplicationInfo getApplicationInfo(PluginDescriptor pluginDescriptor) {
		ApplicationInfo info = new ApplicationInfo();
		info.packageName = getPackageName(pluginDescriptor);
		info.metaData = pluginDescriptor.getMetaData();
		info.name = pluginDescriptor.getApplicationName();
		info.className = pluginDescriptor.getApplicationName();
		info.enabled = true;
		info.processName = null;// 需要时再添加
		info.sourceDir = pluginDescriptor.getInstalledPath();
		info.dataDir = new File(pluginDescriptor.getInstalledPath()).getParent();
		// info.uid == Process.myUid();
		info.publicSourceDir = pluginDescriptor.getInstalledPath();
		info.taskAffinity = null;// 需要时再加上
		info.theme = pluginDescriptor.getApplicationTheme();
		info.flags = info.flags | ApplicationInfo.FLAG_HAS_CODE;
		// 需要时再添加
		// info.nativeLibraryDir = new
		// File(pluginDescriptor.getInstalledPath()).getParentFile().getAbsolutePath()
		// + "/lib";
		String targetSdkVersion = pluginDescriptor.getTargetSdkVersion();
		if (!TextUtils.isEmpty(targetSdkVersion)) {
			info.targetSdkVersion = Integer.valueOf(targetSdkVersion);
		} else {
			info.targetSdkVersion = PluginLoader.getApplication().getApplicationInfo().targetSdkVersion;
		}
		return info;
	}

	private static ActivityInfo getActivityInfo(PluginDescriptor pluginDescriptor, String className) {
		ActivityInfo activityInfo = new ActivityInfo();
		activityInfo.name = className;
		activityInfo.packageName = getPackageName(pluginDescriptor);
		activityInfo.icon = pluginDescriptor.getApplicationIcon();
		activityInfo.metaData = pluginDescriptor.getMetaData();
		activityInfo.enabled = true;
		activityInfo.exported = false;
		activityInfo.applicationInfo = getApplicationInfo(pluginDescriptor);
		activityInfo.taskAffinity = null;// 需要时再加上
		// activityInfo.targetActivity =

		if (pluginDescriptor.getType(className) == PluginDescriptor.ACTIVITY) {
			PluginActivityInfo detail = pluginDescriptor.getActivityInfos().get(className);
			activityInfo.launchMode = Integer.valueOf(detail.getLaunchMode());
			activityInfo.theme = ResourceUtil.getResourceId(detail.getTheme());
			if (detail.getUiOptions() != null) {
				activityInfo.uiOptions = Integer.parseInt(detail.getUiOptions().replace("0x", ""), 16);
			}
			activityInfo.configChanges = detail.getConfigChanges();
		}
		return activityInfo;
	}

	private static ServiceInfo getServiceInfo(PluginDescriptor pluginDescriptor, String className) {
		ServiceInfo serviceInfo = new ServiceInfo();
		serviceInfo.name = className;
		serviceInfo.packageName = getPackageName(pluginDescriptor);
		serviceInfo.icon = pluginDescriptor.getApplicationIcon();
		serviceInfo.metaData = pluginDescriptor.getMetaData();
		serviceInfo.enabled = true;
		serviceInfo.exported = false;
		// 加上插件中配置进程名称后缀
		String process = pluginDescriptor.getServiceProcessInfos().get(className);
		if (process == null) {
			serviceInfo.processName = PluginLoader.getApplication().getPackageName();
		} else if (process.startsWith(":")) {
			serviceInfo.processName = PluginLoader.getApplication().getPackageName() + process;
		} else {
			serviceInfo.processName = process;
		}
		serviceInfo.applicationInfo = getApplicationInfo(pluginDescriptor);
		return serviceInfo;
	}

	private static String getPackageName(PluginDescriptor pluginDescriptor) {
		// 这里要使用插件包名
		return pluginDescriptor.getPackageName();
	}

}
