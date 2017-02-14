package com.tws.plugin.manager;

import android.content.Intent;

import com.tws.plugin.core.PluginLoader;

/**
 * @author yongchen
 */
public class PluginCallbackImpl implements PluginCallback {	

	@Override
	public void onInstall(int result, String packageName, String version, String src) {
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(EXTRA_TYPE, TYPE_INSTALL);
		intent.putExtra(EXTRA_ID, packageName);
		intent.putExtra(EXTRA_VERSION, version);
		intent.putExtra(EXTRA_RESULT_CODE, result);
		intent.putExtra(EXTRA_SRC, src);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	@Override
	public void onRemove(String packageName, boolean success) {
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(EXTRA_TYPE, TYPE_REMOVE);
		intent.putExtra(EXTRA_ID, packageName);
		intent.putExtra(EXTRA_RESULT_CODE, success ? 0 : 7);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	@Override
	public void onRemoveAll(boolean success) {
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(EXTRA_TYPE, TYPE_REMOVE_ALL);
		intent.putExtra(EXTRA_RESULT_CODE, success ? 0 : 7);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	// 未使用
	@Override
	public void onStart(String packageName) {
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(EXTRA_TYPE, TYPE_START);
		intent.putExtra(EXTRA_ID, packageName);
		PluginLoader.getApplication().sendBroadcast(intent);
	}

	// 未使用
	@Override
	public void onStop(String packageName) {
		Intent intent = new Intent(ACTION_PLUGIN_CHANGED);
		intent.putExtra(EXTRA_TYPE, TYPE_STOP);
		intent.putExtra(EXTRA_ID, packageName);
		PluginLoader.getApplication().sendBroadcast(intent);
	}
}
