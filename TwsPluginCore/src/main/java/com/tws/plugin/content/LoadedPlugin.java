package com.tws.plugin.content;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import dalvik.system.BaseDexClassLoader;

/**
 * @author yongchen
 * 
 */
public class LoadedPlugin {

	public final BaseDexClassLoader pluginClassLoader;
	public final Context pluginContext;
	public final Resources pluginResource;

	public final String pluginPackageName;
	public final String pluginSourceDir;

	public Application pluginApplication;

	public LoadedPlugin(String packageName, String pluginSourceDir, Context pluginContext, BaseDexClassLoader pluginClassLoader) {
		this.pluginPackageName = packageName;
		this.pluginSourceDir = pluginSourceDir;
		this.pluginContext = pluginContext;
		this.pluginClassLoader = pluginClassLoader;
		this.pluginResource = pluginContext.getResources();
	}

}
