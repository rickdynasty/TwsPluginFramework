package com.tws.plugin.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.android.HackLayoutInflater;
import com.tws.plugin.core.compat.CompatForSupportv7ViewInflater;
import com.tws.plugin.core.proxy.systemservice.AndroidAppIActivityManager;
import com.tws.plugin.core.proxy.systemservice.AndroidAppIPackageManager;
import com.tws.plugin.core.proxy.systemservice.AndroidWebkitWebViewFactoryProvider;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.ProcessUtil;

import dalvik.system.DexClassLoader;

public class PluginLoader {

	private static final String TAG = "rick_Print:PluginLoader";
	private static final String PLUGIN_SHAREED_PREFERENCE_NAME = "plugins.shared.preferences";
	private static final String VERSION_CODE_KEY = "version.code";
	private static Application sApplication;
	private static boolean isLoaderInited = false;
	private static boolean isLoaderPlugins = false;
	private static final String ASSETS_PLUGS_DIR = "plugins";
	private static String sHostPackageName;

	private PluginLoader() {
	}

	public static Application getApplication() {
		if (sApplication == null) {
			throw new IllegalStateException("框架尚未初始化，请确定在当前进程中，PluginLoader.initLoader方法已执行！");
		}
		return sApplication;
	}

	/**
	 * 初始化loader, 只可调用一次
	 * 
	 * @param app
	 */
	public static synchronized void initPluginFramework(Application app) {
		if (!isLoaderInited) {
			long startTime = System.currentTimeMillis();

			isLoaderInited = true;
			sApplication = app;
			sHostPackageName = app.getPackageName();
			TwsLog.d(TAG, "begin init PluginFramework... HostPackageName is " + sHostPackageName);

			// 这里的isPluginProcess方法需要在安装AndroidAppIActivityManager之前执行一次。
			// 原因见AndroidAppIActivityManager的getRunningAppProcesses()方法
			boolean isPluginProcess = ProcessUtil.isPluginProcess();

			// 进行PendingIntent的resolve、进程欺骗等主要是为了让插件在四大组件之外的组件等单元也具备自己的运行权限
			AndroidAppIActivityManager.installProxy();

			// Notification在适配上出现了不少问题，暂时将这个交由宿主进行，如果DM出现了独立插件在放开进行适配
			// AndroidAppINotificationManager.installProxy();

			// 这里是修正插件运行单元的info信息
			AndroidAppIPackageManager.installProxy(sApplication.getPackageManager());

			if (isPluginProcess) {
				HackLayoutInflater.installPluginCustomViewConstructorCache();
				CompatForSupportv7ViewInflater.installPluginCustomViewConstructorCache();
				// 不可在主进程中同步安装，因为此时ActivityThread还没有准备好, 会导致空指针。
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						// 这个注入 主要是为了解决插件加载本地页面问题
						AndroidWebkitWebViewFactoryProvider.installProxy();
					}
				});
			}

			PluginInjector.injectHandlerCallback();// 本来宿主进程是不需要注入handlecallback的，这里加上是为了对抗360安全卫士等软件，提高Instrumentation的成功率
			PluginInjector.injectInstrumentation();
			PluginInjector.injectBaseContext(sApplication);

			if (isPluginProcess) {
				if (Build.VERSION.SDK_INT >= 14) {
					sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
						@Override
						public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
						}

						@Override
						public void onActivityStarted(Activity activity) {
						}

						@Override
						public void onActivityResumed(Activity activity) {
						}

						@Override
						public void onActivityPaused(Activity activity) {
						}

						@Override
						public void onActivityStopped(Activity activity) {
						}

						@Override
						public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
						}

						@Override
						public void onActivityDestroyed(Activity activity) {
							Intent intent = activity.getIntent();
							if (intent != null && intent.getComponent() != null) {
								PluginManagerHelper.unBindLaunchModeStubActivity(intent.getComponent().getClassName(),
										activity.getClass().getName());
							}
						}
					});
				}
			}
			TwsLog.d(TAG, "Complete Init PluginFramework Take:" + (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	public static Context fixBaseContextForReceiver(Context superApplicationContext) {
		if (superApplicationContext instanceof ContextWrapper) {
			return ((ContextWrapper) superApplicationContext).getBaseContext();
		} else {
			return superApplicationContext;
		}
	}

	/**
	 * 根据插件中的classId加载一个插件中的class
	 * 
	 * @param clazzId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Class loadPluginFragmentClassById(String clazzId) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByFragmentId(clazzId);
		if (pluginDescriptor != null) {
			// 插件可能尚未初始化，确保使用前已经初始化
			LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

			DexClassLoader pluginClassLoader = plugin.pluginClassLoader;

			String clazzName = pluginDescriptor.getPluginClassNameById(clazzId);
			if (clazzName != null) {
				try {
					Class pluginClazz = ((ClassLoader) pluginClassLoader).loadClass(clazzName);
					TwsLog.d(TAG, "loadPluginClass for clazzId:" + clazzId + " clazzName=" + clazzName + " success");
					return pluginClazz;
				} catch (ClassNotFoundException e) {
					TwsLog.e(TAG, "loadPluginFragmentClassById:" + clazzId + " ClassNotFound:" + clazzName
							+ "Exception", e);
					TwsLog.w(TAG, "没有找到：" + clazzName + " 是不是被混淆了~");
				}
			}
		}

		TwsLog.e(TAG, "loadPluginClass for clazzId:" + clazzId + " fail");

		return null;

	}

	@SuppressWarnings("rawtypes")
	public static Class loadPluginClassByName(String clazzName) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(clazzName);
		return loadPluginClassByName(pluginDescriptor, clazzName);
	}

	public static Class loadPluginClassByName(PluginDescriptor pluginDescriptor, String clazzName) {
		if (pluginDescriptor != null) {
			// 插件可能尚未初始化，确保使用前已经初始化
			LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

			DexClassLoader pluginClassLoader = plugin.pluginClassLoader;

			try {
				Class pluginClazz = ((ClassLoader) pluginClassLoader).loadClass(clazzName);
				TwsLog.d(TAG, "loadPluginClass Success for clazzName is " + clazzName);
				return pluginClazz;
			} catch (ClassNotFoundException e) {
				TwsLog.e(TAG, "ClassNotFound " + clazzName, e);
			} catch (java.lang.IllegalAccessError illegalAccessError) {
				illegalAccessError.printStackTrace();
				throw new IllegalAccessError("出现这个异常最大的可能是插件dex和" + "宿主dex包含了相同的class导致冲突, "
						+ "请检查插件的编译脚本，确保排除了所有公共依赖库的jar");
			}

		}

		TwsLog.e(TAG, "loadPluginClass Fail for clazzName:" + clazzName
				+ (pluginDescriptor == null ? "pluginDescriptor = null" : "pluginDescriptor not null"));

		return null;

	}

	/**
	 * 获取当前class所在插件的Context 每个插件只有1个DefaultContext, 是当前插件中所有class公用的Context
	 * 
	 * @param clazz
	 * @return
	 */
	public static Context getDefaultPluginContext(@SuppressWarnings("rawtypes") Class clazz) {

		Context pluginContext = null;
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(clazz.getName());

		if (pluginDescriptor != null) {
			pluginContext = PluginLauncher.instance().getRunningPlugin(pluginDescriptor.getPackageName()).pluginContext;
			;
		} else {
			TwsLog.e(TAG, "PluginDescriptor Not Found for " + clazz.getName());
		}

		if (pluginContext == null) {
			TwsLog.e(TAG, "Context Not Found for " + clazz.getName());
		}

		return pluginContext;
	}

	/**
	 * 根据当前插件的默认Context, 为当前插件的组件创建一个单独的context
	 * 
	 * @param pluginContext
	 * @param base
	 *            由系统创建的Context。 其实际类型应该是ContextImpl
	 * @return
	 */
	/* package */static Context getNewPluginComponentContext(Context pluginContext, Context base, int theme) {
		PluginContextTheme newContext = null;
		if (pluginContext != null) {
			newContext = (PluginContextTheme) PluginCreator.createPluginContext(
					((PluginContextTheme) pluginContext).getPluginDescriptor(), base, pluginContext.getResources(),
					(DexClassLoader) pluginContext.getClassLoader());

			newContext.setPluginApplication((Application) ((PluginContextTheme) pluginContext).getApplicationContext());

			newContext.setTheme(theme);
		}
		return newContext;
	}

	public static Context getNewPluginApplicationContext(String pluginId) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);

		// 插件可能尚未初始化，确保使用前已经初始化
		LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

		if (plugin != null) {
			PluginContextTheme newContext = (PluginContextTheme) PluginCreator.createPluginContext(
					((PluginContextTheme) plugin.pluginContext).getPluginDescriptor(), sApplication.getBaseContext(),
					plugin.pluginResource, plugin.pluginClassLoader);

			newContext.setPluginApplication(plugin.pluginApplication);

			newContext.setTheme(pluginDescriptor.getApplicationTheme());

			return newContext;
		}

		return null;
	}

	public static boolean isInstalled(String pluginId, String pluginVersion) {
		PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);
		if (pluginDescriptor != null) {
			TwsLog.d(
					TAG,
					"call isInstalled pluginId=" + pluginId + " pluginDescriptor.getVersion="
							+ pluginDescriptor.getVersion() + " pluginVersion=" + pluginVersion);
			return pluginDescriptor.getVersion().equals(pluginVersion);
		}
		return false;
	}

	public static synchronized void loadPlugins(Context app) {
		if (!isLoaderPlugins) {
			long beginTime = System.currentTimeMillis();
			// step1 判断application的版本号，通过版本号来判断是否要全部更新插件内容
			int currentVersionCode = 1;
			try {
				final PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(),
						PackageManager.GET_CONFIGURATIONS);
				currentVersionCode = pi.versionCode;
			} catch (NameNotFoundException e) {
				TwsLog.w(TAG, "loadPlugins getPackageInfo Exception:", e);
			}

			final int oldVersion = getVersionCode();
			TwsLog.d(TAG, "call loadPlugins - oldVersion is " + oldVersion + ", newVersion is " + currentVersionCode);
			if (oldVersion != currentVersionCode) {
				TwsLog.d(TAG, "首次/升级安装,先清理...");// rick_Note:这个有个问题需要确定：如果新版本里面不包含之前版本的插件包该怎么处理？？？？
				// 版本升级 清理掉之前安装的所有插件
				PluginManagerHelper.removeAll();
				saveVersionCode(currentVersionCode);

				// step2 加载assets/plugins下面的插件
				installAssetsPlugins();
			}

			TwsLog.d(TAG, "loadPlugins 耗时：" + (System.currentTimeMillis() - beginTime) + "ms");

			isLoaderPlugins = true;
		} else {
			Exception here = new Exception();
			here.fillInStackTrace();
			TwsLog.w(TAG, "===仅用于查看调用栈[非异常]===has loadPlugins", here);
		}
	}

	// 安装内置插件
	private static synchronized void installAssetsPlugins() {
		TwsLog.d(TAG, "installAssetsPlugins()");
		// 加载插件黑名单
		ArrayList<String> blacklist = null;
		String configFile = Environment.getExternalStorageDirectory().getPath()
				+ PluginApplication.PLUGIN_BLACKLIST_FILE;
		try {
			File file = new File(configFile);
			if (file.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = "";
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (TextUtils.isEmpty(line) || line.startsWith("#"))
						continue;

					if (blacklist == null) {
						blacklist = new ArrayList<String>();
					}

					blacklist.add(line);
				}
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		final AssetManager asset = getApplication().getAssets();
		String[] files = null;
		try {
			files = asset.list(ASSETS_PLUGS_DIR);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (files != null) {
			for (String apk : files) {
				if (!apk.endsWith(".apk")) {
					TwsLog.e(TAG, "主意 - fill：" + apk + "不是apk，将放弃走插件安装流程...");
					continue;
				}

				if (blacklist != null && blacklist.contains(apk)) {
					TwsLog.d(TAG, "插件：" + apk + "在黑名单中，continue~");
					continue;
				}

				copyAndInstall(ASSETS_PLUGS_DIR + "/" + apk);
			}
		}
	}

	private static int getVersionCode() {
		SharedPreferences sp = getSharedPreference();

		return sp == null ? 0 : sp.getInt(VERSION_CODE_KEY, 0);
	}

	private static synchronized void saveVersionCode(int verCode) {
		TwsLog.d(TAG, "saveVersionCode:" + verCode);
		getSharedPreference().edit().putInt(VERSION_CODE_KEY, verCode).commit();
	}

	private static SharedPreferences getSharedPreference() {
		SharedPreferences sp = getApplication().getSharedPreferences(PLUGIN_SHAREED_PREFERENCE_NAME,
				Build.VERSION.SDK_INT < 11 ? Context.MODE_PRIVATE : Context.MODE_PRIVATE | 0x0004);
		return sp;
	}

	public static String getHostPackageName() {
		if (TextUtils.isEmpty(sHostPackageName)) {
			sHostPackageName = getApplication().getPackageName();
		}

		return sHostPackageName;
	}

	public static void copyAndInstall(String name) {
		try {
			Log.d(TAG, "copyAndInstall:" + name);
			InputStream assestInput = getApplication().getAssets().open(name);
			File file = getApplication().getExternalFilesDir(null);

			String dest = (file == null ? ("/storage/emulated/0/Android/data/" + getHostPackageName() + "/files")
					: file.getAbsolutePath() + "/" + name);

			if (FileUtil.copyFile(assestInput, dest)) {
				PluginManagerHelper.installPlugin(dest);
			} else {
				assestInput = getApplication().getAssets().open(name);
				file = getApplication().getCacheDir();
				if (file == null) {
					Toast.makeText(getApplication(), "抽取assets中的Apk：" + name + " 02  因getCacheDir()==null失败",
							Toast.LENGTH_SHORT).show();
					return;
				}

				dest = file.getAbsolutePath() + "/" + name;
				if (FileUtil.copyFile(assestInput, dest)) {
					PluginManagerHelper.installPlugin(dest);
				} else {
					Toast.makeText(getApplication(), "抽取assets中的Apk失败" + dest, Toast.LENGTH_SHORT).show();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getApplication(), "安装失败", Toast.LENGTH_SHORT).show();
		}
	}

	public static String getPackageName(final Intent intent) {
		String packageName = intent.getPackage();
		if (TextUtils.isEmpty(packageName) && intent.getComponent() != null) {
			packageName = intent.getComponent().getPackageName();
		}

		return packageName;
	}
}
