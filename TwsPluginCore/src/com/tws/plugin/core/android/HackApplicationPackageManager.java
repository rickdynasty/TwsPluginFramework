package com.tws.plugin.core.android;

import com.tws.plugin.util.RefInvoker;

/**
 * Created by yongchen on 16/10/30.
 */

public class HackApplicationPackageManager {
	private static final String ClassName = "android.app.ApplicationPackageManager";

	private static final String Field_mPM = "mPM";

	private Object instance;

	public HackApplicationPackageManager(Object instance) {
		this.instance = instance;
	}

	public void setPM(Object pm) {
		RefInvoker.setField(instance, "android.app.ApplicationPackageManager", "mPM", pm);
	}
}
