package com.tws.plugin.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Window;

import com.tws.plugin.bridge.TwsPluginBridgeActivity;
import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginActivityInfo;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.content.PluginProviderInfo;
import com.tws.plugin.core.android.HackActivity;
import com.tws.plugin.core.android.HackActivityThread;
import com.tws.plugin.core.android.HackApplication;
import com.tws.plugin.core.android.HackContextImpl;
import com.tws.plugin.core.android.HackContextThemeWrapper;
import com.tws.plugin.core.android.HackContextWrapper;
import com.tws.plugin.core.android.HackLayoutInflater;
import com.tws.plugin.core.android.HackLoadedApk;
import com.tws.plugin.core.android.HackService;
import com.tws.plugin.core.android.HackWindow;
import com.tws.plugin.core.android.TwsActivityInterface;
import com.tws.plugin.core.annotation.PluginContainer;
import com.tws.plugin.core.compat.CompatForSupportv7_23_2;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;
import com.tws.plugin.util.ResourceUtil;

public class PluginInjector {

	private static final String TAG = "rick_Print:PluginInjector";

	/**
	 * 替换宿主程序Application对象的mBase是为了修改它的几个StartActivity、
	 * StartService和SendBroadcast方法
	 */
	static void injectBaseContext(Context context) {
		TwsLog.d(TAG, "替换宿主程序Application对象的mBase");
		HackContextWrapper wrapper = new HackContextWrapper(context);
		wrapper.setBase(new PluginBaseContextWrapper(wrapper.getBase()));
	}

	/**
	 * 注入Instrumentation主要是为了支持Activity
	 */
	static void injectInstrumentation() {
		// 给Instrumentation添加一层代理，用来实现隐藏api的调用
		TwsLog.d(TAG, "替换宿主程序Intstrumentation");
		HackActivityThread.wrapInstrumentation();
	}

	static void injectHandlerCallback() {
		TwsLog.d(TAG, "向宿主程序消息循环插入回调器");
		HackActivityThread.wrapHandler();
	}

	public static void installContentProviders(Context context, Context pluginContext,
			Collection<PluginProviderInfo> pluginProviderInfos) {
		List<ProviderInfo> providers = new ArrayList<ProviderInfo>();
		for (PluginProviderInfo pluginProviderInfo : pluginProviderInfos) {
			ProviderInfo p = new ProviderInfo();
			// name做上标记，表示是来自插件，方便classloader进行判断
			p.name = pluginProviderInfo.getName();
			p.authority = pluginProviderInfo.getAuthority();
			p.applicationInfo = new ApplicationInfo(context.getApplicationInfo());
			p.applicationInfo.packageName = pluginContext.getPackageName();
			p.exported = pluginProviderInfo.isExported();
			p.packageName = context.getApplicationInfo().packageName;
			providers.add(p);
		}

		if (providers.size() > 0) {
			TwsLog.d(TAG,
					"为插件:" + pluginContext.getPackageName() + " 安装ContentProvider size=" + pluginProviderInfos.size());
			// pluginContext.getPackageName().equals(applicationInfo.packageName)
			// == true
			// 安装的时候使用的是插件的Context, 所有无需对Classloader进行映射处理
			HackActivityThread.get().installContentProviders(pluginContext, providers);
		}
	}

	static void injectInstrumetionFor360Safe(Activity activity, Instrumentation pluginInstrumentation) {
		// 检查mInstrumention是否已经替换成功。
		// 之所以要检查，是因为如果手机上安装了360手机卫士等app，它们可能会劫持用户app的ActivityThread对象，
		// 导致在PluginApplication的onCreate方法里面替换mInstrumention可能会失败
		// 所以这里再做一次检查
		HackActivity hackActivity = new HackActivity(activity);
		Instrumentation instrumention = hackActivity.getInstrumentation();
		if (!(instrumention instanceof PluginInstrumentionWrapper)) {
			// 说明被360还原了，这里再次尝试替换
			hackActivity.setInstrumentation(pluginInstrumentation);
		}
	}

	static void injectActivityContext(final Activity activity) {
		TwsLog.d(TAG, "injectActivityContext");
		String pluginId = null;
		boolean isStubActivity = false;

		if (ProcessUtil.isPluginProcess()) {
			// 如果是打开插件中的activity,
			final Intent intent = activity.getIntent();
			isStubActivity = PluginManagerHelper.isStub(intent.getComponent().getClassName());
			if (!isStubActivity) {
				final String rampActivityName = intent.getStringExtra(PluginIntentResolver.INTENT_EXTRA_BRIDGE_RAMP);
				isStubActivity = TwsPluginBridgeActivity.class.getName().equals(rampActivityName);
			}

			if (activity instanceof PluginContainer) {
				pluginId = ((PluginContainer) activity).getPluginId();
			}
		}

		HackActivity hackActivity = new HackActivity(activity);

		if (isStubActivity || !TextUtils.isEmpty(pluginId)) {

			PluginDescriptor pd = null;
			LoadedPlugin plugin = null;

			if (isStubActivity) {
				// 是打开插件中的activity
				pd = PluginManagerHelper.getPluginDescriptorByClassName(activity.getClass().getName());
				plugin = PluginLauncher.instance().startPlugin(pd);
				// 获取插件Application对象
				Application pluginApp = plugin.pluginApplication;
				// 重设mApplication
				hackActivity.setApplication(pluginApp);
			} else {
				// 是打开的用来显示插件组件的宿主activity
				if (!TextUtils.isEmpty(pluginId)) {
					// 进入这里表示指定了这个宿主Activity "只显示" 某个插件的组件
					// 因此直接将这个Activity的Context也替换成插件的Context
					pd = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);
					plugin = PluginLauncher.instance().getRunningPlugin(pluginId);
				} else {
					// do nothing
					// 进入这里表示这个宿主可能要同时显示来自多个不同插件的组件,
					// 也就没办法将Context替换成之中某一个插件的context,
					// 剩下的交给PluginViewFactory去处理
					return;
				}
			}

			PluginActivityInfo pluginActivityInfo = pd.getActivityInfos().get(activity.getClass().getName());

			ActivityInfo activityInfo = hackActivity.getActivityInfo();
			final boolean isTwsActivity = (activity instanceof TwsActivityInterface);
			int pluginAppTheme = getPluginTheme(activityInfo, pluginActivityInfo, pd, isTwsActivity);

			TwsLog.d(TAG, "Theme 0x" + Integer.toHexString(pluginAppTheme) + " activity:"
					+ activity.getClass().getName());

			// 在activityoncreate之前去完成attachBaseContext的事情
			Context pluginContext = PluginLoader.getNewPluginComponentContext(plugin.pluginContext,
					activity.getBaseContext(), pluginAppTheme);

			resetActivityContext(pluginContext, activity, pluginAppTheme);

			resetWindowConfig(pluginContext, pd, activity, activityInfo, pluginActivityInfo);

			activity.setTitle(activity.getClass().getName());

		} else {
			// 如果是打开宿主程序的activity，注入一个无害的Context，用来在宿主程序中startService和sendBroadcast时检查打开的对象是否是插件中的对象
			// 插入Context
			Context mainContext = new PluginBaseContextWrapper(activity.getBaseContext());
			hackActivity.setBase(null);
			hackActivity.attachBaseContext(mainContext);
		}
	}

	static void resetActivityContext(final Context pluginContext, final Activity activity, final int pluginAppTheme) {
		if (pluginContext == null) {
			return;
		}

		// 重设BaseContext
		HackContextThemeWrapper hackContextThemeWrapper = new HackContextThemeWrapper(activity);
		hackContextThemeWrapper.setBase(null);
		hackContextThemeWrapper.attachBaseContext(pluginContext);

		// 由于在attach的时候Resource已经被初始化了，所以需要重置Resource
		hackContextThemeWrapper.setResources(null);

		CompatForSupportv7_23_2.fixResource(pluginContext, activity);

		// 重设theme
		if (pluginAppTheme != 0) {
			hackContextThemeWrapper.setTheme(null);
			activity.setTheme(pluginAppTheme);
		}
		// 重设theme
		((PluginContextTheme) pluginContext).mTheme = null;
		pluginContext.setTheme(pluginAppTheme);

		Window window = activity.getWindow();

		HackWindow hackWindow = new HackWindow(window);
		// 重设mContext
		hackWindow.setContext(pluginContext);

		// 重设mWindowStyle
		hackWindow.setWindowStyle(null);
		// 让WindowStyle构建出来
		window.getWindowStyle();
		// 注意这里重设回context,是为了解决MEIZU等奇葩机型对系统statusBar的定制
		hackWindow.setContext(activity);

		// 重设LayoutInflater
		TwsLog.d(TAG, activity.getWindow().getClass().getName());
		// 注意：这里getWindow().getClass().getName() 不一定是android.view.Window
		// 如miui下返回MIUI window
		hackWindow.setLayoutInflater(window.getClass().getName(), LayoutInflater.from(activity));

		// 如果api>=11,还要重设factory2
		if (Build.VERSION.SDK_INT >= 11) {
			new HackLayoutInflater(window.getLayoutInflater()).setPrivateFactory(activity);
		}
	}

	static void resetWindowConfig(final Context pluginContext, final PluginDescriptor pd, final Activity activity,
			final ActivityInfo activityInfo, final PluginActivityInfo pluginActivityInfo) {

		if (pluginActivityInfo != null) {

			// 如果PluginContextTheme的getPackageName返回了插件包名,需要在这里对attribute修正
			activity.getWindow().getAttributes().packageName = PluginLoader.getApplication().getPackageName();

			if (null != pluginActivityInfo.getWindowSoftInputMode()) {
				activity.getWindow().setSoftInputMode(
						Integer.parseInt(pluginActivityInfo.getWindowSoftInputMode().replace("0x", ""), 16));
			}
			if (Build.VERSION.SDK_INT >= 14) {
				if (null != pluginActivityInfo.getUiOptions()) {
					activity.getWindow().setUiOptions(
							Integer.parseInt(pluginActivityInfo.getUiOptions().replace("0x", ""), 16));
				}
			}
			if (null != pluginActivityInfo.getScreenOrientation()) {
				int orientation = Integer.parseInt(pluginActivityInfo.getScreenOrientation());
				// noinspection ResourceType
				if (orientation != activityInfo.screenOrientation && !activity.isChild()) {
					// noinspection ResourceType
					activity.setRequestedOrientation(orientation);
				}
			}
			if (Build.VERSION.SDK_INT >= 18 && !activity.isChild()) {
				Boolean isImmersive = ResourceUtil.getBoolean(pluginActivityInfo.getImmersive(), pluginContext);
				if (isImmersive != null) {
					activity.setImmersive(isImmersive);
				}
			}

			final String claName = activity.getClass().getName();
			TwsLog.d(TAG, claName + " immersive is " + pluginActivityInfo.getImmersive());
			TwsLog.d(TAG, claName + " screenOrientation is " + pluginActivityInfo.getScreenOrientation());
			TwsLog.d(TAG, claName + " launchMode is " + pluginActivityInfo.getLaunchMode());
			TwsLog.d(TAG, claName + " windowSoftInputMode is " + pluginActivityInfo.getWindowSoftInputMode());
			TwsLog.d(TAG, claName + " uiOptions is " + pluginActivityInfo.getUiOptions());
		}

		// 如果是独立插件，由于没有合并资源，这里还需要替换掉 mActivityInfo，
		// 避免activity试图通过ActivityInfo中的资源id来读取资源时失败
		activityInfo.icon = pd.getApplicationIcon();
		activityInfo.logo = pd.getApplicationLogo();
		if (Build.VERSION.SDK_INT >= 19) {
			activity.getWindow().setIcon(activityInfo.icon);
			activity.getWindow().setLogo(activityInfo.logo);
		}
	}

	/* package */static void replaceReceiverContext(Context baseContext, Context newBase) {

		if (HackContextImpl.instanceOf(baseContext)) {
			ContextWrapper receiverRestrictedContext = new HackContextImpl(baseContext).getReceiverRestrictedContext();
			new HackContextWrapper(receiverRestrictedContext).setBase(newBase);
		}
	}

	// 这里是因为在多进程情况下，杀死插件进程，自动恢复service时有个bug导致一个service同时存在多个service实例
	// 这里做个遍历保护
	// break;
	/* package */static void replacePluginServiceContext(String serviceName) {
		Map<IBinder, Service> services = HackActivityThread.get().getServices();
		if (services != null) {
			Iterator<Service> itr = services.values().iterator();
			while (itr.hasNext()) {
				Service service = itr.next();
				if (service != null && service.getClass().getName().equals(serviceName)) {

					replacePluginServiceContext(serviceName, service);
				}

			}
		}
	}

	/* package */static void replacePluginServiceContext(String servieName, Service service) {
		PluginDescriptor pd = PluginManagerHelper.getPluginDescriptorByClassName(servieName);

		LoadedPlugin plugin = PluginLauncher.instance().getRunningPlugin(pd.getPackageName());

		HackService hackService = new HackService(service);
		hackService.setBase(PluginLoader.getNewPluginComponentContext(plugin.pluginContext, service.getBaseContext(),
				pd.getApplicationTheme()));
		hackService.setApplication(plugin.pluginApplication);
		hackService.setClassName(PluginManagerHelper.bindStubService(service.getClass().getName(), pd
				.getServiceProcessInfos().get(service.getClass().getName())));
	}

	/* package */static void replaceHostServiceContext(String serviceName) {
		Map<IBinder, Service> services = HackActivityThread.get().getServices();
		if (services != null) {
			Iterator<Service> itr = services.values().iterator();
			while (itr.hasNext()) {
				Service service = itr.next();
				if (service != null && service.getClass().getName().equals(serviceName)) {
					PluginInjector.injectBaseContext(service);
					break;
				}

			}
		}
	}

	/**
	 * 主题的选择顺序为 先选择插件Activity配置的主题，再选择插件Application配置的主题，
	 * 如果是非独立插件，再选择宿主Activity主题 如果是独立插件，再选择系统默认主题
	 * 
	 * @param activityInfo
	 * @param pluginActivityInfo
	 * @param pd
	 * @return
	 */
	private static int getPluginTheme(ActivityInfo activityInfo, PluginActivityInfo pluginActivityInfo,
			PluginDescriptor pd, boolean isTwsActivity) {
		int pluginAppTheme = pd.getApplicationTheme();
		if (isTwsActivity || pluginAppTheme == 0) {
			pluginAppTheme = PluginLoader.getApplication().getApplicationInfo().theme;
		}

		if (pluginAppTheme == 0 && pd.isStandalone()) {
			pluginAppTheme = android.R.style.Theme_Holo_Light;
		}

		return pluginAppTheme;
	}

	/**
	 * 如果插件中不包含service、receiver，是不需要替换classloader的
	 */
	public static void hackHostClassLoaderIfNeeded() {
		HackApplication hackApplication = new HackApplication(PluginLoader.getApplication());
		Object mLoadedApk = hackApplication.getLoadedApk();
		if (mLoadedApk == null) {
			// 重试一次
			mLoadedApk = hackApplication.getLoadedApk();
		}
		if (mLoadedApk == null) {
			// 换个方式再试一次
			mLoadedApk = HackActivityThread.getLoadedApk();
		}
		if (mLoadedApk != null) {
			HackLoadedApk hackLoadedApk = new HackLoadedApk(mLoadedApk);
			ClassLoader originalLoader = hackLoadedApk.getClassLoader();
			if (!(originalLoader instanceof HostClassLoader)) {
				HostClassLoader newLoader = new HostClassLoader("", PluginLoader.getApplication().getCacheDir()
						.getAbsolutePath(), PluginLoader.getApplication().getCacheDir().getAbsolutePath(),
						originalLoader);
				hackLoadedApk.setClassLoader(newLoader);
			}
		} else {
			TwsLog.w(TAG, "What!!Why?");
		}
	}
}
