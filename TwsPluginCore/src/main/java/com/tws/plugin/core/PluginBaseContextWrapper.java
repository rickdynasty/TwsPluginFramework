/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tws.plugin.core;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;

import com.tws.plugin.core.android.HackContextImpl;
import com.tws.plugin.manager.PluginManagerHelper;

import java.io.File;
import java.util.ArrayList;

import qrom.component.log.QRomLog;

public class PluginBaseContextWrapper extends ContextWrapper {

	private static final String TAG = "rick_Print_TT:PluginBaseContextWrapper";

	public PluginBaseContextWrapper(Context base) {
		super(base);
	}

	@Override
	public void sendBroadcast(Intent intent) {
		QRomLog.i(TAG, " call sendBroadcast :" + intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendBroadcast(item);
		}
	}

	@Override
	public void sendBroadcast(Intent intent, String receiverPermission) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendBroadcast(item, receiverPermission);
		}
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendOrderedBroadcast(item, receiverPermission);
		}
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
			Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendOrderedBroadcast(item, receiverPermission, resultReceiver, scheduler, initialCode, initialData,
					initialExtras);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void sendBroadcastAsUser(Intent intent, UserHandle user) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendBroadcastAsUser(item, user);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendBroadcastAsUser(item, user, receiverPermission);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission,
			BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData,
			Bundle initialExtras) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendOrderedBroadcastAsUser(item, user, receiverPermission, resultReceiver, scheduler, initialCode,
					initialData, initialExtras);
		}
	}

	@Override
	public void sendStickyBroadcast(Intent intent) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendStickyBroadcast(item);
		}
	}

	@Override
	public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler,
			int initialCode, String initialData, Bundle initialExtras) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendStickyOrderedBroadcast(item, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}

	}

	@Override
	public void removeStickyBroadcast(Intent intent) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.removeStickyBroadcast(item);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendStickyBroadcastAsUser(item, user);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver,
			Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
		QRomLog.i(TAG, intent.toString());
		ArrayList<Intent> list = PluginIntentResolver.resolveReceiver(intent);
		for (Intent item : list) {
			super.sendStickyOrderedBroadcastAsUser(item, user, resultReceiver, scheduler, initialCode, initialData,
					initialExtras);
		}
	}

	@Override
	public ComponentName startService(Intent service) {
		QRomLog.i(TAG, "call startService " + service.toString());
		PluginIntentResolver.resolveService(service);
		return super.startService(service);
	}

	@Override
	public boolean stopService(Intent name) {
		QRomLog.i(TAG, "call stopService " + name.toString());
		PluginIntentResolver.resolveService(name);
		return super.stopService(name);
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags) {
		QRomLog.i(TAG, "call bindService " + service.toString());
		PluginIntentResolver.resolveService(service);
		return super.bindService(service, conn, flags);
	}

	@Override
	public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
		QRomLog.i(TAG, "call createPackageContext packageName is " + packageName + " flags is " + flags);
		// 这个方法有2个作用
		// 1、context返回插件宿主packageName时,安装插件中的contentprovider时会用到它，
		// 被android.app.ActiviThread这个类调用。
		// 2、可以方便的创建一个插件ApplicationContext副本。用于满足一些特定的业务需要
		if (PluginManagerHelper.getPluginDescriptorByPluginId(packageName) != null) {
			return PluginCreator.getNewPluginApplicationContext(packageName);
		}
		return super.createPackageContext(packageName, flags);
	}

	private File mHostPreferencesFile = null;

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (23 < Build.VERSION.SDK_INT) {
			synchronized (PluginContextTheme.class) {
				HackContextImpl impl = new HackContextImpl(getContextImpl());
				ArrayMap<String, File> mSharedPrefsPaths = impl.getSharedPrefsPaths();

				String privateDir = PluginLoader.getApplication().getFilesDir().getParentFile().getPath();
				if (mHostPreferencesFile == null) {
					mHostPreferencesFile = new File(privateDir, "shared_prefs");
				}
				String preferencesDir = mHostPreferencesFile.getAbsolutePath();
				QRomLog.i(TAG, "preferencesDir is " + preferencesDir);
				if (mSharedPrefsPaths != null) {
					File file = mSharedPrefsPaths.get(name);
					if (file != null) {
						QRomLog.i(TAG, "file path is " + file.getAbsolutePath() + " parent is " + file.getParent());
					}
					if (file != null && !file.getParent().equals(preferencesDir)) {
						mSharedPrefsPaths.remove(name);// 置空之后再get会触发重建，则getDataDir有机会生效
					}
				}
				File preferencesDirFile = impl.getPreferencesDir();
				if (preferencesDirFile == null || !preferencesDirFile.getAbsolutePath().equals(preferencesDir)) {
					impl.setPreferencesDir(mHostPreferencesFile);
				}
			}
		}

		return super.getSharedPreferences(name, mode);
	}

	protected Object getContextImpl() {
		int dep = 0;// 这个dep限制是以防万一陷入死循环
		Context base = getBaseContext();
		while (base instanceof ContextWrapper && dep < 10) {
			base = ((ContextWrapper) base).getBaseContext();
			dep++;
		}
		if (HackContextImpl.instanceOf(base)) {
			return base;
		}
		return null;
	}

	protected SharedPreferences getSharedPreferencesEx(String name, int mode) {
		return super.getSharedPreferences(name, mode);
	}
}
