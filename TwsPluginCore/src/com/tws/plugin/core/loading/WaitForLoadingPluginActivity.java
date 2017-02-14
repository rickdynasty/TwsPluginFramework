package com.tws.plugin.core.loading;

import tws.component.log.TwsLog;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;

/**
 * 这个页面要求尽可能的简单
 */

public class WaitForLoadingPluginActivity extends Activity {
	private static final String TAG = "rick_Print:WaitForLoadingPluginActivity";

	private PluginDescriptor pluginDescriptor;
	private Handler handler;
	private long loadingAt = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// 是否需要全屏取决于上个页面是否为全屏,
		// 目的是和上个页面保持一致, 否则被透视的页面会发生移动
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);

		int resId = PluginLoader.getLoadingResId();
		TwsLog.d(TAG, "WaitForLoadingPluginActivity ContentView Id = " + resId);

		if (resId != 0) {
			setContentView(resId);
		}
		handler = new Handler();
		loadingAt = System.currentTimeMillis();

	}

	@Override
	protected void onResume() {
		super.onResume();
		TwsLog.d(TAG, "WaitForLoadingPluginActivity Shown");
		if (pluginDescriptor != null && !PluginLauncher.instance().isRunning(pluginDescriptor.getPackageName())) {
			new Thread(new Runnable() {
				@Override
				public void run() {

					PluginLauncher.instance().startPlugin(pluginDescriptor);

					long remainTime = (loadingAt + PluginLoader.getMinLoadingTime()) - System.currentTimeMillis();

					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							LoadedPlugin loadedPlugin = PluginLauncher.instance().getRunningPlugin(
									pluginDescriptor.getPackageName());
							if (loadedPlugin != null && loadedPlugin.pluginApplication != null) {
								TwsLog.d(TAG, "WaitForLoadingPluginActivity open target");
								startActivity(getIntent());
								finish();
							} else {
								TwsLog.d(TAG, "WTF! :" + pluginDescriptor + " :" + loadedPlugin);
								finish();
							}
						}
					}, remainTime);
				}
			}).start();
		} else {
			TwsLog.d(TAG, "WTF!:" + pluginDescriptor);
			finish();
		}
	}

	public void setTargetPlugin(PluginDescriptor pluginDescriptor) {
		this.pluginDescriptor = pluginDescriptor;
	}
}
