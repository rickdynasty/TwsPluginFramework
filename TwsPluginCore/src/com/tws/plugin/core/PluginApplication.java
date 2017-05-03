package com.tws.plugin.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tws.component.log.TwsLog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Environment;
import android.text.TextUtils;

import com.tws.plugin.content.DisplayConfig;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;

public class PluginApplication extends Application {

	private static final String TAG = "rick_Print:PluginApplication";
	private static PluginApplication instance;
	private ArrayList<String> mEliminatePlugins = new ArrayList<String>();

	public static String EXCLUDE_PLUGIN_FILE = "/plugins/exclude_plugin.ini";
	public static String PLUGIN_BLACKLIST_FILE = "/plugins/plugin_blacklist.ini";

	private Configuration mSaveConfiguration = null;

	public static PluginApplication getInstance() {
		return instance;
	}

	public ArrayList<String> getEliminatePlugins() {
		return mEliminatePlugins;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		instance = this;
		// 这个地方之所以这样写，是因为如果是插件进程，initPluginFramework必须在applicaiotn启动时执行
		// 而如果是宿主进程，initPluginFramework可以在这里执行，也可以在需要时再在宿主的其他组件中执行，
		// 例如点击宿主的某个Activity中的button后再执行这个方法来启动插件框架。

		// 总体原则有3点：
		// 1、插件进程和宿主进程都必须有机会执行initPluginFramework
		// 2、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前，不可和插件交互
		// 3、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前启动的组件，即使在initPluginFramework都执行完毕之后，也不可和插件交互

		// 如果initPluginFramework都在进程启动时就执行，自然很轻松满足上述条件。
		if (ProcessUtil.isPluginProcess(this)) {
			TwsLog.d(TAG, "插件进程 PluginLoader.initPluginFramework");
			// 插件进程，必须在这里执行initPluginFramework
			PluginLoader.initPluginFramework(this);
			// init ServiceManager
			LocalServiceManager.init();
		} else if (ProcessUtil.isHostProcess(this)) {
			// 宿主进程，可以在这里执行，也可以选择在宿主的其他地方在需要时再启动插件框架
			TwsLog.d(TAG, "宿主进程 PluginLoader.initPluginFramework");
			PluginLoader.initPluginFramework(this);
			// init ServiceManager
			LocalServiceManager.init();
		}
	}

	/**
	 * 重写这个方法是为了支持Receiver,否则会出现ClassCast错误
	 */
	@Override
	public Context getBaseContext() {
		return PluginLoader.fixBaseContextForReceiver(super.getBaseContext());
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// 注册日志广播 全局生命周期 高优先级 需要放在application中
		if (mSaveConfiguration == null) {
			mSaveConfiguration = new Configuration(getResources().getConfiguration());
		}

		TwsLog.registerLogReceiver(this);
		initEliminatePlugins();
		if (ProcessUtil.isPluginProcess(this)) {
			PluginLoader.loadPlugins(this);
		}
	}

	protected void startNeedPowerbootPlugin() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
				Iterator<PluginDescriptor> itr = plugins.iterator();
				while (itr.hasNext()) {
					final PluginDescriptor pluginDescriptor = itr.next();
					if (TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
						TwsLog.e(TAG, "My god !!! how can have such a situatio~!");
						continue;
					}

					if (mEliminatePlugins.contains(pluginDescriptor.getPackageName())) {
						TwsLog.w(TAG, "当前插件" + pluginDescriptor.getPackageName() + "已经被列入黑名单了");
						continue;
					}

					final ArrayList<DisplayConfig> dcs = pluginDescriptor.getDisplayConfigs();
					if (dcs != null) {
						for (DisplayConfig dc : dcs) {
							if (dc.pos == DisplayConfig.DISPLAY_AT_OTHER_POS) {
								switch (dc.contentType) {
								case DisplayConfig.TYPE_SERVICE:
									Intent intent = new Intent();
									intent.setClassName(getApplicationContext(), dc.content);
									startService(intent);
									break;
								case DisplayConfig.TYPE_PACKAGENAEM:
									if (null == PluginLauncher.instance().startPlugin(dc.content)) {
										TwsLog.e(TAG, "startPlugin:" + dc.content + "失败!!!");
									}
									break;
								default:
									break;
								}
							}
						}
					}
				}
			}
		}).start();
	}

	private void initEliminatePlugins() {
		String configFile = Environment.getExternalStorageDirectory().getPath() + EXCLUDE_PLUGIN_FILE;
		try {
			File file = new File(configFile);
			if (!file.exists()) {
				return;
			}

			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = "";
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (TextUtils.isEmpty(line) || line.startsWith("#"))
					continue;

				if (mEliminatePlugins == null) {
					mEliminatePlugins = new ArrayList<String>();
				}

				mEliminatePlugins.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mEliminatePlugins != null) {
			TwsLog.w(TAG + "getExceList()", "U config Eliminate the following plug-ins:");
			for (String pStr : mEliminatePlugins) {
				TwsLog.d(TAG, " " + pStr);
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mSaveConfiguration.diff(newConfig) != 0 && ProcessUtil.isPluginProcess(this)) {
			mSaveConfiguration.updateFrom(newConfig);
			TwsLog.d(TAG, "更新所有插件的Config配置");
			PluginLauncher.instance().onConfigurationChanged(newConfig);
		}
	}
}
