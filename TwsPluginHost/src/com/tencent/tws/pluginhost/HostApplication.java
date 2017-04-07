package com.tencent.tws.pluginhost;

import android.content.Context;

import com.tencent.tws.framework.HostProxy;
import com.tencent.tws.pluginhost.ui.view.Hotseat;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.util.ProcessUtil;

public class HostApplication extends PluginApplication {
	private String mFouceTabClassId = Hotseat.HOST_HOME_FRAGMENT;// STORE_FRAGMENT;//

	@Override
	public void onCreate() {
		super.onCreate();
		final boolean isPluginPro = ProcessUtil.isPluginProcess(this);

		if (isPluginPro) {
			// 提前启动应用的依赖插件[DM的启动依赖登录和配对插件]
			startAppDependentPlugin();
			// 随DM启动的插件 时机调整到application的onCreate里面
			startNeedPowerbootPlugin();
		}
	}

	private void startAppDependentPlugin() {
		// 宿主的启动依赖一些插件，需要提前加载好这些插件
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		HostProxy.setApplication(this);
	}

	public String getFouceTabClassId() {
		return mFouceTabClassId;
	}

	public void setFouceTabClassId(String classId) {
		mFouceTabClassId = classId;
	}
}
