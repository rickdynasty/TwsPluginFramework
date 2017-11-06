package com.tws.plugin.util;

import android.content.Intent;

import com.tws.plugin.content.DisplayItem;
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

		if (type == DisplayItem.TYPE_BROADCAST) {

			Intent newIntent = PluginIntentResolver.resolveReceiver(intent).get(0);
			return newIntent;

		} else if (type == DisplayItem.TYPE_ACTIVITY) {

			PluginIntentResolver.resolveActivity(intent);
			return intent;

		} else if (type == DisplayItem.TYPE_SERVICE) {

			PluginIntentResolver.resolveService(intent);
			return intent;

		}
		return intent;
	}

}
