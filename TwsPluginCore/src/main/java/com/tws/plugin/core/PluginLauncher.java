package com.tws.plugin.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.BaseDexClassLoader;
import qrom.component.log.QRomLog;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.android.HackActivityThread;
import com.tws.plugin.core.android.HackApplication;
import com.tws.plugin.core.android.HackSupportV4LocalboarcastManager;
import com.tws.plugin.core.compat.CompatForWebViewFactoryApi21;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.manager.PluginActivityMonitor;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;

/**
 * <Pre>
 * @author yongchen
 * </Pre>
 * 
 */
public class PluginLauncher implements Serializable {

	private static final String TAG = "rick_Print:PluginLauncher";

	private static PluginLauncher runtime;

	private ConcurrentHashMap<String, LoadedPlugin> loadedPluginMap = new ConcurrentHashMap<String, LoadedPlugin>();
	private ConcurrentHashMap<String, BaseDexClassLoader> plulginClassLoaderMap = new ConcurrentHashMap<String, BaseDexClassLoader>();

	private PluginLauncher() {
		if (!ProcessUtil.isPluginProcess()) {
			throw new IllegalAccessError("本类仅在插件进程使用");
		}
	}

	public static PluginLauncher instance() {
		if (runtime == null) {
			synchronized (PluginLauncher.class) {
				if (runtime == null) {
					runtime = new PluginLauncher();
				}
			}
		}
		return runtime;
	}

	public LoadedPlugin getRunningPlugin(String packageName) {
		return loadedPluginMap.get(packageName);
	}

	public LoadedPlugin startPlugin(String packageName) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(packageName);
		if (pluginDescriptor != null) {
			return startPlugin(pluginDescriptor);
		}
		return null;
	}

	public synchronized LoadedPlugin startPlugin(PluginDescriptor pluginDescriptor) {
		LoadedPlugin plugin = loadedPluginMap.get(pluginDescriptor.getPackageName());
		if (plugin == null) {
			long beginTime = System.currentTimeMillis();
			QRomLog.i(TAG, "正在初始化插: " + pluginDescriptor.getPackageName()
					+ ": Resources, DexClassLoader, Context, Application");
			QRomLog.i(
					TAG,
					"插件信息 Ver:" + pluginDescriptor.getVersion() + " InstalledPath="
							+ pluginDescriptor.getInstalledPath());

			Resources pluginRes = PluginCreator.createPluginResource(
					PluginLoader.getApplication().getApplicationInfo().sourceDir, PluginLoader.getApplication()
							.getResources(), pluginDescriptor);

			if (pluginRes == null) {
				QRomLog.e(TAG, "初始化插件失败");
			}

			long ct_Res_end = System.currentTimeMillis();
			QRomLog.i(TAG, "初始化插件资源 耗时：" + (ct_Res_end - beginTime));

			// plulginClassLoaderMap
			BaseDexClassLoader pluginClassLoader = plulginClassLoaderMap.get(pluginDescriptor.getInstalledPath());
			if (null == pluginClassLoader) {
				QRomLog.i(TAG, "createPluginClassLoader for plugin:" + pluginDescriptor.getInstalledPath());
				pluginClassLoader = PluginCreator.createPluginClassLoader(pluginDescriptor.getInstalledPath(),
						pluginDescriptor.isStandalone(), pluginDescriptor.getDependencies(),
						pluginDescriptor.getMuliDexList());
				plulginClassLoaderMap.put(pluginDescriptor.getInstalledPath(), pluginClassLoader);
			}

			long ct_Dex_end = System.currentTimeMillis();
			QRomLog.i(TAG, "初始化插件DexClassLoader 耗时：" + (ct_Dex_end - ct_Res_end));

			PluginContextTheme pluginContext = (PluginContextTheme) PluginCreator.createPluginContext(pluginDescriptor,
					PluginLoader.getApplication().getBaseContext(), pluginRes, pluginClassLoader);

			// 插件Context默认主题设置为插件application主题
			pluginContext.setTheme(pluginDescriptor.getApplicationTheme());

			long ct_Theme_end = System.currentTimeMillis();
			QRomLog.i(TAG, "初始化插件Theme 耗时：" + (ct_Theme_end - ct_Dex_end));

			plugin = new LoadedPlugin(pluginDescriptor.getPackageName(), pluginDescriptor.getInstalledPath(),
					pluginContext, pluginClassLoader);

			loadedPluginMap.put(pluginDescriptor.getPackageName(), plugin);

			if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
				QRomLog.i(TAG, "当前执行插件初始化的线程是主线程，开始初始化插件Application");
				initApplication(pluginContext, pluginClassLoader, pluginRes, pluginDescriptor, plugin);
			} else {
				QRomLog.i(TAG, "当前执行插件初始化的线程不是主线程，异步通知主线程初始化插件Application:" + Thread.currentThread().getId()
						+ " name is " + Thread.currentThread().getName());
				final LoadedPlugin finalLoadedPlugin = plugin;
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						if (finalLoadedPlugin.pluginApplication == null) {
							PluginLauncher.instance().initApplication(finalLoadedPlugin.pluginContext,
									finalLoadedPlugin.pluginClassLoader,
									finalLoadedPlugin.pluginContext.getResources(),
									((PluginContextTheme) finalLoadedPlugin.pluginContext).getPluginDescriptor(),
									finalLoadedPlugin);
						}
					}
				});
			}

			long ct_end = System.currentTimeMillis();
			QRomLog.i(TAG, "startPlugin  耗时：" + (ct_end - beginTime));
		} else {
			// LogUtil.d("IS RUNNING", packageName);
		}

		return plugin;
	}

	public void initApplication(Context pluginContext, BaseDexClassLoader pluginClassLoader, Resources pluginRes,
			PluginDescriptor pluginDescriptor, LoadedPlugin plugin) {

		QRomLog.i(TAG, "开始初始化插件:" + pluginDescriptor.getPackageName() + " " + pluginDescriptor.getApplicationName());

		long t13 = System.currentTimeMillis();

		Application pluginApplication = callPluginApplicationOnCreate(pluginContext, pluginClassLoader,
				pluginDescriptor);

		plugin.pluginApplication = pluginApplication;// 这里之所以不放在LoadedPlugin的构造器里面，是因为contentprovider在安装时loadclass，造成死循环

		long t3 = System.currentTimeMillis();
		QRomLog.i(TAG, "初始化插件 " + pluginDescriptor.getPackageName() + " " + pluginDescriptor.getApplicationName()
				+ ",  耗时：" + (t3 - t13));

		try {
			HackActivityThread.installPackageInfo(PluginLoader.getApplication(), pluginDescriptor.getPackageName(),
					pluginDescriptor, pluginClassLoader, pluginRes, pluginApplication);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// 解决插件中webview加载html时<input type=date />控件出错的问题，兼容性待验证
		CompatForWebViewFactoryApi21.addWebViewAssets(plugin.pluginApplication.getAssets());

		QRomLog.i(TAG, "初始化插件" + pluginDescriptor.getPackageName() + "完成");
	}

	private Application callPluginApplicationOnCreate(Context pluginContext, BaseDexClassLoader classLoader,
			PluginDescriptor pluginDescriptor) {

		Application application = null;

		try {
			QRomLog.i(TAG, "创建插件Application:" + pluginDescriptor.getApplicationName());

			// 为了支持插件中使用multidex
			((PluginContextTheme) pluginContext).setCrackPackageManager(true);

			application = Instrumentation.newApplication(classLoader.loadClass(pluginDescriptor.getApplicationName()),
					pluginContext);

			// 为了支持插件中使用multidex
			((PluginContextTheme) pluginContext).setCrackPackageManager(false);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// 安装ContentProvider, 在插件Application对象构造以后，oncreate调用之前
		PluginInjector.installContentProviders(PluginLoader.getApplication(), application, pluginDescriptor
				.getProviderInfos().values());

		// 执行onCreate
		if (application != null) {

			((PluginContextTheme) pluginContext).setPluginApplication(application);

			// 先拿到宿主的crashHandler
			Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();

			application.onCreate();

			// 再还原宿主的crashHandler，这里之所以需要还原CrashHandler，
			// 是因为如果插件中自己设置了自己的crashHandler（通常是在oncreate中），
			// 会导致当前进程的主线程的handler被意外修改。
			// 如果有多个插件都有设置自己的crashHandler，也会导致混乱
			// 所以这里直接屏蔽掉插件的crashHandler
			// TODO 或许也可以做成消息链进行分发？
			Thread.setDefaultUncaughtExceptionHandler(old);

			if (Build.VERSION.SDK_INT >= 14) {
				// ActivityLifecycleCallbacks
				// 的回调实际是由Activity内部在自己的声明周期函数内主动调用application的注册的callback触发的
				// 由于我们把插件Activity内部的application成员变量替换调用了
				// 会导致不会触发宿主中注册的ActivityLifecycleCallbacks
				// 那么我们在这里给插件的Application对象注册一个callback
				// bridge。将插件的call发给宿主的call，
				// 从而使得宿主application中注册的callback能监听到插件Activity的声明周期
				application.registerActivityLifecycleCallbacks(new LifecycleCallbackBridge(PluginLoader
						.getApplication()));
			} else {
				// 对于小于14的版本，影响是，StubActivity的绑定关系不能被回收，
				// 意味着宿主配置的非Stand的StubActivity的个数不能小于插件中对应的类型的个数的总数，否则可能会出现找不到映射的StubActivity
			}

		}

		return application;
	}

	public void stopPlugin(String packageName, PluginDescriptor pluginDescriptor) {
		final LoadedPlugin plugin = getRunningPlugin(packageName);

		if (plugin == null) {
			QRomLog.i(TAG, "插件未运行:" + packageName);
			return;
		}

		// 退出LocalService
		QRomLog.i(TAG, "退出LocalService");
		LocalServiceManager.unRegistService(pluginDescriptor);
		// TODO 还要通知宿主进程退出localService，不过不通知其实本身也不会坏影响。

		// 退出Activity
		QRomLog.i(TAG, "退出Activity");
		PluginLoader.getApplication().sendBroadcast(
				new Intent(plugin.pluginPackageName + PluginActivityMonitor.ACTION_UN_INSTALL_PLUGIN));

		// 退出 LocalBroadcastManager
		QRomLog.i(TAG, "退出LocalBroadcastManager");
		Object mInstance = HackSupportV4LocalboarcastManager.getInstance();
		if (mInstance != null) {
			HackSupportV4LocalboarcastManager hackSupportV4LocalboarcastManager = new HackSupportV4LocalboarcastManager(
					mInstance);
			HashMap<BroadcastReceiver, ArrayList<IntentFilter>> mReceivers = hackSupportV4LocalboarcastManager
					.getReceivers();
			if (mReceivers != null) {
				Iterator<BroadcastReceiver> ir = mReceivers.keySet().iterator();
				while (ir.hasNext()) {
					BroadcastReceiver item = ir.next();
					if (item.getClass().getClassLoader() == plugin.pluginClassLoader) {
						hackSupportV4LocalboarcastManager.unregisterReceiver(item);
					}
				}
			}
		}

		// 退出Service
		// bindservie启动的service应该不需要处理，退出activity的时候会unbind
		Map<IBinder, Service> map = HackActivityThread.get().getServices();
		if (map != null) {
			Collection<Service> list = map.values();
			for (Service s : list) {
				if (s.getClass().getClassLoader() == plugin.pluginClassLoader) {
					s.stopSelf();
				}
			}
		}

		// 退出Application
		if (null != plugin.pluginApplication) {
			plugin.pluginApplication.onTerminate();
		}
				
		// 退出webview
		QRomLog.i(TAG, "还原WebView Context");
//		new Handler(Looper.getMainLooper()).post(new Runnable() {
//			@Override
//			public void run() {
//				// 退出BroadcastReceiver
//				// 广播一般有个注册方式
//				// 1、activity、service注册
//				// 这种方式，在上一步Activitiy、service退出时会自然退出，所以不用处理
//				// 2、application注册
//				// 这里需要处理这种方式注册的广播，这种方式注册的广播会被PluginContextTheme对象记录下来
//				QRomLog.i(TAG, "退出BroadcastReceiver");
//				((PluginContextTheme) plugin.pluginApplication.getBaseContext()).unregisterAllReceiver();
//			}
//		});

		// 退出AssetManager
		// pluginDescriptor.getPluginContext().getResources().getAssets().close();

		// 退出ContentProvider
		// TODO ContentProvider如何退出？
		// ActivityThread.releaseProvider(IContentProvider provider, boolean
		// stable)

		// 退出fragment
		// 即退出由FragmentManager保存的Fragment
		// TODO fragment如何退出？

		loadedPluginMap.remove(packageName);
	}

	public boolean isRunning(String packageName) {
		return loadedPluginMap.get(packageName) != null;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	static class LifecycleCallbackBridge implements ActivityLifecycleCallbacks {

		private HackApplication hackPluginApplication;

		public LifecycleCallbackBridge(Application pluginApplication) {
			this.hackPluginApplication = new HackApplication(pluginApplication);
		}

		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
			hackPluginApplication.dispatchActivityCreated(activity, savedInstanceState);
		}

		@Override
		public void onActivityStarted(Activity activity) {
			hackPluginApplication.dispatchActivityStarted(activity);
		}

		@Override
		public void onActivityResumed(Activity activity) {
			hackPluginApplication.dispatchActivityResumed(activity);
		}

		@Override
		public void onActivityPaused(Activity activity) {
			hackPluginApplication.dispatchActivityPaused(activity);
		}

		@Override
		public void onActivityStopped(Activity activity) {
			hackPluginApplication.dispatchActivityStopped(activity);
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
			hackPluginApplication.dispatchActivitySaveInstanceState(activity, outState);
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
			hackPluginApplication.dispatchActivityDestroyed(activity);
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		for (Map.Entry<String, LoadedPlugin> e : loadedPluginMap.entrySet()) {
			final LoadedPlugin lp = e.getValue();
			if (lp != null && 0 != lp.pluginResource.getConfiguration().diff(newConfig)) {
				lp.pluginResource.updateConfiguration(newConfig, lp.pluginResource.getDisplayMetrics());
			}
		}
	}
}
