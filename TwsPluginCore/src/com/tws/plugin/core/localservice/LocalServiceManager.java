package com.tws.plugin.core.localservice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tws.component.log.TwsLog;

import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLauncher;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.servicemanager.ServiceManager;
import com.tws.plugin.servicemanager.local.ServicePool;

/**
 * @author yongchen
 */
public class LocalServiceManager {

	protected static final String TAG = "rick_Print:LocalServiceManager";
	static boolean isSupport = false;

	static {
		try {
			Class ServiceManager = Class.forName("com.tws.plugin.servicemanager.ServiceManager");
			isSupport = ServiceManager != null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		if (!isSupport) {
			return;
		}
		ServiceManager.init(PluginLoader.getApplication());
	}

	public static void registerService(PluginDescriptor plugin) {
		if (!isSupport) {
			return;
		}
		HashMap<String, String> localServices = plugin.getFunctions();
		if (localServices != null) {
			Iterator<Map.Entry<String, String>> serv = localServices.entrySet().iterator();
			while (serv.hasNext()) {
				Map.Entry<String, String> entry = serv.next();
				LocalServiceManager.registerService(plugin.getPackageName(), entry.getKey(), entry.getValue());
			}
		}
	}

	public static void registerService(final String pluginId, final String serviceName, final String serviceClass) {
		if (!isSupport) {
			return;
		}
		ServiceManager.publishService(serviceName, new ServicePool.ClassProvider() {
			@Override
			public Object getServiceInstance() {

				// 插件可能尚未初始化，确保使用前已经初始化
				LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginId);
				if (plugin != null) {
					try {
						return plugin.pluginClassLoader.loadClass(serviceClass.split("\\|")[0]).newInstance();
					} catch (ClassNotFoundException e) {
						TwsLog.e(TAG, "获取服务失败", e);
					} catch (InstantiationException e) {
						TwsLog.e(TAG, "获取服务失败", e);
					} catch (IllegalAccessException e) {
						TwsLog.e(TAG, "获取服务失败", e);
					}
				} else {
					TwsLog.e(TAG, "未找到插件:" + pluginId);
				}
				return null;
			}

			@Override
			public String getInterfaceName() {
				return serviceClass.split("\\|")[1];
			}
		});
	}

	public static Object getService(String name) {
		if (!isSupport) {
			return null;
		}
		return ServiceManager.getService(name);
	}

	public static void unRegistService(PluginDescriptor plugin) {
		if (!isSupport) {
			return;
		}
		HashMap<String, String> localServices = plugin.getFunctions();
		if (localServices != null) {
			Iterator<Map.Entry<String, String>> serv = localServices.entrySet().iterator();
			while (serv.hasNext()) {
				Map.Entry<String, String> entry = serv.next();
				ServiceManager.unPublishService(entry.getKey());
			}
		}
	}

	public static void unRegistAll() {
        if (!isSupport) {
            return;
        }
		ServiceManager.unPublishAllService();
	}

}
