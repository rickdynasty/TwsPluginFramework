package com.tws.plugin.core.annotation;

public interface PluginContainer {

	public static final String FRAGMENT_PLUGIN_ID = "PluginDispatcher.fragment.PluginId";

	public void setPluginId(String id);

	public String getPluginId();
}
