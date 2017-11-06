package com.tws.plugin.manager;

public interface PluginCallback {

	public static final String ACTION_PLUGIN_CHANGED = "com.tws.plugin.core.action_plugin_changed";

	public static final String EXTRA_TYPE = "type";
	public static final String EXTRA_ID = "id";
	public static final String EXTRA_VERSION = "version";
	public static final String EXTRA_RESULT_CODE = "code";
	public static final String EXTRA_SRC = "src";

	public static final int TYPE_UNKNOW = 0x0000;
	public static final int TYPE_INSTALL = 0x0001;
	public static final int TYPE_REMOVE = 0x0002;
	public static final int TYPE_REMOVE_ALL = 0x0004;
	public static final int TYPE_START = 0x0008;
	public static final int TYPE_STOP = 0x0010;

	void onInstall(int result, String packageName, String version, String src);

	void onRemove(String packageName, boolean success);

	void onRemoveAll(boolean success);

	void onStart(String packageName);

	void onStop(String packageName);
}
