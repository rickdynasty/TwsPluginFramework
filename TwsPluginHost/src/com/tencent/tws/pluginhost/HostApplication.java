package com.tencent.tws.pluginhost;

import android.content.Context;

import com.tencent.tws.framework.utils.HostProxy;
import com.tencent.tws.pluginhost.ui.view.Hotseat;
import com.tws.plugin.core.PluginApplication;

public class HostApplication extends PluginApplication {
	private String mFouceTabClassId = Hotseat.MY_WATCH_FRAGMENT;// STORE_FRAGMENT;//

	@Override
	public void onCreate() {
		super.onCreate();
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
