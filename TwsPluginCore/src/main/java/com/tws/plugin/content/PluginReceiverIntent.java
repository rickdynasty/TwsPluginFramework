package com.tws.plugin.content;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * @author yongchen
 */
@SuppressLint("ParcelCreator")
public class PluginReceiverIntent extends Intent {

	public PluginReceiverIntent(Intent intent) {
		super(intent);
	}

	@Override
	public void setExtrasClassLoader(ClassLoader loader) {
		if (11 < Build.VERSION.SDK_INT) {
			Bundle extra = getExtras();
			if (extra != null) {
				loader = extra.getClassLoader();
			}
		}
		super.setExtrasClassLoader(loader);
	}
}