package com.tws.plugin.core.compat;

import tws.component.log.TwsLog;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.SparseArray;

import com.tws.plugin.core.android.HackAssetManager;
import com.tws.plugin.core.android.HackWebViewFactory;

public class CompatForWebViewFactoryApi21 {

	private static final String TAG = "rick_Print:CompatForWebViewFactoryApi21";

	public static void addWebViewAssets(AssetManager assetsManager) {
		if (Build.VERSION.SDK_INT >= 21) {
			PackageInfo packageInfo = HackWebViewFactory.getLoadedPackageInfo();
			if (packageInfo != null) {
				HackAssetManager hackAssetManager = new HackAssetManager(assetsManager);
				SparseArray<String> packageIdentifiers = hackAssetManager.getAssignedPackageIdentifiers();
				if (!isAdded(packageIdentifiers, packageInfo.packageName)) {
					TwsLog.i(TAG, "Loaded WebView Package : " + packageInfo.packageName + " version "
							+ packageInfo.versionName + " (code " + packageInfo.versionCode + ")"
							+ packageInfo.applicationInfo.sourceDir);
					hackAssetManager.addAssetPath(packageInfo.applicationInfo.sourceDir);
				}
			}
		}
	}

	private static boolean isAdded(SparseArray<String> packageIdentifiers, String packageName) {
		if (packageIdentifiers != null) {
			for (int i = 0; i < packageIdentifiers.size(); i++) {
				final String name = packageIdentifiers.valueAt(i);
				if (packageName.equals(name)) {
					return true;
				}
			}
		}
		return false;
	}
}
