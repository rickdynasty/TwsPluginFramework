package com.tws.plugin.util;

import android.content.Intent;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginIntentResolver;

/**
 * @author yongchen
 */
public class PendingIntentHelper {
	/**
	 * used before send notification
	 * 
	 * @param intent
	 * @return
	 */
	public static Intent resolvePendingIntent(Intent intent, int type) {

		if (type == PluginDescriptor.BROADCAST) {

			Intent newIntent = PluginIntentResolver.resolveReceiver(intent).get(0);
			return newIntent;

		} else if (type == PluginDescriptor.ACTIVITY) {

			PluginIntentResolver.resolveActivity(intent);
			return intent;

		} else if (type == PluginDescriptor.SERVICE) {

			PluginIntentResolver.resolveService(intent);
			return intent;

		}
		return intent;
	}

}
