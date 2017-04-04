package com.tws.plugin.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TwsPluginBridgeReceiver extends BroadcastReceiver {

	private static final String TAG = "TwsPluginBridgeReceiver";

	@Override
	public void onReceive(Context context, Intent initIntent) {
		Log.e(TAG, "绑定桥接BroadcastReceiver失败了哦！！！");
	}

}
