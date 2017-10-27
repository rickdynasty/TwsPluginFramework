package com.tws.plugin.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TwsPluginBridgeService extends Service {

	private static final String TAG = "TwsPluginBridgeService";

	@Override
	public IBinder onBind(Intent intent) {
		Log.e(TAG, "绑定桥接Service失败了哦！！！");
		return null;
	}

}
