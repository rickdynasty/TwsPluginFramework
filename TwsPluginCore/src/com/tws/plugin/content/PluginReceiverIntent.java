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

	public PluginReceiverIntent(Intent o) {
		super(o);
	}

	@Override
	public void setExtrasClassLoader(ClassLoader loader) {
		if (Build.VERSION.SDK_INT > 11) {
			Bundle extra = getExtras();
			if (extra != null) {
				loader = extra.getClassLoader();
			}
		}
		super.setExtrasClassLoader(loader);
	}
}