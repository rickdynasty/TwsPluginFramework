package com.tws.plugin.core;

import tws.component.log.TwsLog;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.tws.plugin.core.android.HackContextImpl;
import com.tws.plugin.core.android.HackService;

/**
 * 此类用于修正service的中的context
 */
public class PluginShadowService extends Service {

	private static final String TAG = "rick_Print:PluginShadowService";
	public Context mBaseContext = null;
	public Object mThread = null;
	public String mClassName = null;
	public IBinder mToken = null;
	public Application mApplication = null;
	public Object mActivityManager = null;
	public Boolean mStartCompatibility = false;

	public Service realService;

	@Override
	public void onCreate() {
		super.onCreate();

		getAttachParam();

		callServiceOnCreate();
	}

	private void getAttachParam() {
		mBaseContext = getBaseContext();
		HackService hackService = new HackService(this);
		mThread = hackService.getThread();
		mClassName = hackService.getClassName();
		mToken = hackService.getToken();
		mApplication = getApplication();
		mActivityManager = hackService.getActivityManager();
		mStartCompatibility = hackService.getStartCompatibility();
	}

	private void callServiceOnCreate() {
		String realName = mClassName;
		try {
			realName = mClassName.replace(PluginIntentResolver.CLASS_PREFIX_SERVICE, "");
			TwsLog.d(TAG, "className:" + mClassName + " target:" + realName);
			Class clazz = PluginLoader.loadPluginClassByName(realName);
			realService = (Service) clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(
					"Unable to instantiate service " + mClassName
							+ ": " + e.toString(), e);
		}

		try {
			new HackContextImpl(mBaseContext).setOuterContext(realService);
			HackService hackService = new HackService(realService);
			hackService.attach(mBaseContext, mThread, mClassName, mToken, mApplication, mActivityManager);
			hackService.setStartCompatibility(mStartCompatibility);

			//拿到创建好的service，重新 设置mBase和mApplicaiton
			PluginInjector.replacePluginServiceContext(realName, realService);

			realService.onCreate();
		} catch (Exception e) {
			throw new RuntimeException(
					"Unable to create service " + mClassName
							+ ": " + e.toString(), e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
