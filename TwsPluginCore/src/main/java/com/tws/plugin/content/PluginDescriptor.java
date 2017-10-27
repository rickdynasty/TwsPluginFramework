package com.tws.plugin.content;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import qrom.component.log.QRomLog;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tws.plugin.bridge.TwsPluginBridgeActivity;
import com.tws.plugin.util.ResourceUtil;

/**
 * <Pre>
 * @author yongchen
 * </Pre>
 */
public class PluginDescriptor implements Serializable {
	protected static final String TAG = "rick_Print:PluginDescriptor";
	private static final long serialVersionUID = -7545734825911798344L;

	public static final int UNKOWN = 0;
	public static final int BROADCAST = 1;
	public static final int ACTIVITY = 2;
	public static final int SERVICE = 4;
	public static final int PROVIDER = 6;
	public static final int FRAGMENT = 8;
	public static final int FUNCTION = 9;
	public static final int APPLICATION = 10;

	private String packageName;

	private String platformBuildVersionCode;

	private String platformBuildVersionName;

	private String minSdkVersion;

	private String targetSdkVersion;

	private String version;

	private String description;

	private boolean isStandalone;

	private boolean isEnabled;

	private String applicationName;

	private int applicationIcon;

	private int applicationLogo;

	private int applicationTheme;

	/**
	 * 定义在插件Manifest中的meta-data标签
	 */
	private transient Bundle metaData;

	private HashMap<String, PluginProviderInfo> providerInfos = new HashMap<String, PluginProviderInfo>();

	/**
	 * key: fragment id, value: fragment class
	 */
	private HashMap<String, String> fragments = new HashMap<String, String>();

	/**
	 * key: localservice id, value: localservice class
	 */
	private HashMap<String, String> functions = new HashMap<String, String>();

	/**
	 * key: activity class name value: intentfilter list
	 */
	private HashMap<String, ArrayList<PluginIntentFilter>> activitys = new HashMap<String, ArrayList<PluginIntentFilter>>();

	/**
	 * key: activity class name value: activity info in Manifest
	 */
	private HashMap<String, PluginActivityInfo> activityInfos = new HashMap<String, PluginActivityInfo>();

	/**
	 * key: service class name value: intentfilter list
	 */
	private HashMap<String, ArrayList<PluginIntentFilter>> services = new HashMap<String, ArrayList<PluginIntentFilter>>();

	private HashMap<String, String> serviceProcessInfos = new HashMap<String, String>();

	/**
	 * key: receiver class name value: intentfilter list
	 */
	private HashMap<String, ArrayList<PluginIntentFilter>> receivers = new HashMap<String, ArrayList<PluginIntentFilter>>();

	private String installedPath;

	private String[] dependencies;

	private ArrayList<String> muliDexList;

	private ArrayList<DisplayConfig> displayConfigs = null;

	// =============getter and setter======================

	// ////////////////////////////////////定制化配置//////////////////////////////////////
	// 个是可定制化的配置 1、依赖app 2、依赖插件
	public ArrayList<String> dependOns = null;
	// ////////////////////////////////////定制化配置//////////////////////////////////////

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPlatformBuildVersionCode() {
		return platformBuildVersionCode;
	}

	public void setPlatformBuildVersionCode(String platformBuildVersionCode) {
		this.platformBuildVersionCode = platformBuildVersionCode;
	}

	public String getPlatformBuildVersionName() {
		return platformBuildVersionName;
	}

	public void setPlatformBuildVersionName(String platformBuildVersionName) {
		this.platformBuildVersionName = platformBuildVersionName;
	}

	public String getMinSdkVersion() {
		return minSdkVersion;
	}

	public void setMinSdkVersion(String minSdkVersion) {
		this.minSdkVersion = minSdkVersion;
	}

	public String getTargetSdkVersion() {
		return targetSdkVersion;
	}

	public void setTargetSdkVersion(String targetSdkVersion) {
		this.targetSdkVersion = targetSdkVersion;
	}

	public String getVersion() {
		return version;
	}

	public int getVersionCode() {
		if (TextUtils.isEmpty(version))
			return 1;

		final String[] values = version.split(DisplayConfig.SEPARATOR_VER);
		return Integer.parseInt(values[0]);
	}
	
	public String getVersionName() {
		if (TextUtils.isEmpty(version))
			return "1.0";

		final String[] values = version.split(DisplayConfig.SEPARATOR_VER);
		if (values.length < 2)
			return "1.0";

		return values[1];
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getApplicationIcon() {
		return applicationIcon;
	}

	public void setApplicationIcon(int icon) {
		this.applicationIcon = icon;
	}

	public int getApplicationLogo() {
		return applicationLogo;
	}

	public void setApplicationLogo(int logo) {
		this.applicationLogo = logo;
	}

	public int getApplicationTheme() {
		return applicationTheme;
	}

	public void setApplicationTheme(int theme) {
		this.applicationTheme = theme;
	}

	public Bundle getMetaData() {
		if (metaData == null) {
			if (installedPath != null) {
				metaData = ResourceUtil.getApplicationMetaData(installedPath);
				if (metaData == null) {
					metaData = new Bundle();
				}
			}
		}
		return metaData;
	}

	public HashMap<String, String> getFragments() {
		return fragments;
	}

	public void setfragments(HashMap<String, String> fragments) {
		this.fragments = fragments;
	}

	public HashMap<String, String> getFunctions() {
		return functions;
	}

	public void setFunctions(HashMap<String, String> functions) {
		this.functions = functions;
	}

	public HashMap<String, ArrayList<PluginIntentFilter>> getReceivers() {
		return receivers;
	}

	public void setReceivers(HashMap<String, ArrayList<PluginIntentFilter>> receivers) {
		this.receivers = receivers;
	}

	public HashMap<String, ArrayList<PluginIntentFilter>> getActivitys() {
		return activitys;
	}

	public void setActivitys(HashMap<String, ArrayList<PluginIntentFilter>> activitys) {
		this.activitys = activitys;
	}

	public HashMap<String, PluginActivityInfo> getActivityInfos() {
		return activityInfos;
	}

	public void setActivityInfos(HashMap<String, PluginActivityInfo> activityInfos) {
		this.activityInfos = activityInfos;
	}

	public HashMap<String, String> getServiceProcessInfos() {
		return serviceProcessInfos;
	}

	public void setServiceProcessInfos(HashMap<String, String> serviceProcessInfos) {
		this.serviceProcessInfos = serviceProcessInfos;
	}

	public HashMap<String, ArrayList<PluginIntentFilter>> getServices() {
		return services;
	}

	public void setServices(HashMap<String, ArrayList<PluginIntentFilter>> services) {
		this.services = services;
	}

	public String getInstalledPath() {
		return installedPath;
	}

	public void setInstalledPath(String installedPath) {
		this.installedPath = installedPath;
	}

	public String[] getDependencies() {
		return dependencies;
	}

	public void setDependencies(String[] dependencies) {
		this.dependencies = dependencies;
	}

	public ArrayList<DisplayConfig> getDisplayConfigs() {
		return displayConfigs;
	}

	public void setDisplayConfigs(ArrayList<DisplayConfig> displayConfigs) {
		this.displayConfigs = displayConfigs;
	}

	public List<String> getMuliDexList() {
		return muliDexList;
	}

	public void setMuliDexList(ArrayList<String> muliDexList) {
		this.muliDexList = muliDexList;
	}
	
	public ArrayList<String> getDependOns() {
		return dependOns;
	}

	public void setDependOns(ArrayList<String> dependOns) {
		this.dependOns = dependOns;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isStandalone() {
		return isStandalone;
	}

	public void setStandalone(boolean isStandalone) {
		this.isStandalone = isStandalone;
	}

	public HashMap<String, PluginProviderInfo> getProviderInfos() {
		return providerInfos;
	}

	public void setProviderInfos(HashMap<String, PluginProviderInfo> providerInfos) {
		this.providerInfos = providerInfos;
	}

	/**
	 * 需要根据id查询的只有fragment
	 * 
	 * @param clazzId
	 * @return
	 */
	public String getPluginClassNameById(String clazzId) {
		String clazzName = getFragments().get(clazzId);

		if (clazzName == null) {
			QRomLog.d(TAG, "clazzName not found for classId:" + clazzId);
		} else {
			QRomLog.d(TAG, "clazzName found:" + clazzName);
		}

		return clazzName;
	}

	/**
	 * 需要根据Id查询的只有fragment
	 * 
	 * @param clazzId
	 * @return
	 */
	public boolean containsFragment(String clazzId) {
		if (getFragments().containsKey(clazzId) && isEnabled()) {
			return true;
		}
		return false;
	}

	/**
	 * 根据className查询
	 * 
	 * @param clazzName
	 * @return
	 */
	public int matcheName(String clazzName) {
		if (getFragments().containsValue(clazzName) && isEnabled()) {
			return FRAGMENT;
		} else if (getActivitys().containsKey(clazzName) && isEnabled()) {
			return ACTIVITY;
		} else if (getReceivers().containsKey(clazzName) && isEnabled()) {
			return BROADCAST;
		} else if (getServices().containsKey(clazzName) && isEnabled()) {
			return SERVICE;
		} else if (getProviderInfos().containsKey(clazzName) && isEnabled()) {
			return PROVIDER;
		} else if (getApplicationName().equals(clazzName) && !clazzName.equals(Application.class.getName())
				&& isEnabled()) {
			return APPLICATION;
		}

		return UNKOWN;
	}

	/**
	 * 获取class的类型： activity
	 * 
	 * @return
	 */
	public int getType(String clazzName) {
		if (getFragments().containsValue(clazzName) && isEnabled()) {
			return FRAGMENT;
		} else if (getActivitys().containsKey(clazzName) && isEnabled()) {
			return ACTIVITY;
		} else if (getReceivers().containsKey(clazzName) && isEnabled()) {
			return BROADCAST;
		} else if (getServices().containsKey(clazzName) && isEnabled()) {
			return SERVICE;
		} else if (getProviderInfos().containsKey(clazzName) && isEnabled()) {
			return PROVIDER;
		}
		return UNKOWN;
	}

	public List<ComponentInfo> matchPlugin(Intent intent, int type) {
		List<ComponentInfo> result = null;
		String clazzName = null;
		// 如果是通过组件进行匹配的, 这里忽略了packageName
		if (intent.getComponent() != null && type == matcheName(intent.getComponent().getClassName())
				&& !TwsPluginBridgeActivity.class.getName().equals(intent.getComponent().getClassName())) {
			clazzName = intent.getComponent().getClassName();
			result = new ArrayList<ComponentInfo>(1);
			//当前暂时就service支持配置多进程
			String process = (type == SERVICE ? serviceProcessInfos.get(clazzName) : null);
			result.add(new ComponentInfo(clazzName, type, getPackageName(), process));
			return result;// 暂时不考虑不同的插件中配置了相同名称的组件的问题,先到先得
		}

		// 如果是通过IntentFilter进行匹配的
		ArrayList<ComponentInfo> list = findClassNameByIntent(intent, type);
		if (list != null && list.size() > 0) {
			switch (type) {
			case PluginDescriptor.ACTIVITY:
				result = new ArrayList<ComponentInfo>(1);
				result.add(list.get(0));
				return result;// 暂时不考虑多个Activity配置了相同的Intent的问题,先到先得
			case PluginDescriptor.SERVICE:
				result = new ArrayList<ComponentInfo>(1);
				result.add(list.get(0));
				return result;// service本身不支持多匹配,先到先得
			case PluginDescriptor.BROADCAST:
				result = new ArrayList<ComponentInfo>();
				result.addAll(list);// 暂时不考虑去重的问题
				return result;
			default:
				break;
			}
		}

		return null;
	}

	private ArrayList<ComponentInfo> findClassNameByIntent(Intent intent, final int type) {
		final HashMap<String, ArrayList<PluginIntentFilter>> intentFilter;
		switch (type) {
		case ACTIVITY:
			intentFilter = getActivitys();
			break;
		case SERVICE:
			intentFilter = getServices();
			break;
		case BROADCAST:
			intentFilter = getReceivers();
			break;
		default:
			intentFilter = null;
			break;
		}

		if (intentFilter != null) {
			ArrayList<ComponentInfo> targetComponentInfos = null;

			Iterator<Map.Entry<String, ArrayList<PluginIntentFilter>>> entry = intentFilter.entrySet().iterator();
			while (entry.hasNext()) {
				Map.Entry<String, ArrayList<PluginIntentFilter>> item = entry.next();
				Iterator<PluginIntentFilter> values = item.getValue().iterator();
				while (values.hasNext()) {
					PluginIntentFilter filter = values.next();
					int result = filter.match(intent.getAction(), intent.getType(), intent.getScheme(),
							intent.getData(), intent.getCategories());

					if (result != PluginIntentFilter.NO_MATCH_ACTION && result != PluginIntentFilter.NO_MATCH_CATEGORY
							&& result != PluginIntentFilter.NO_MATCH_DATA && result != PluginIntentFilter.NO_MATCH_TYPE
							&& result != PluginIntentFilter.NO_MATCH_RESULT) {
						if (targetComponentInfos == null) {
							targetComponentInfos = new ArrayList<ComponentInfo>();
						}
						
						//当前暂时就service支持配置多进程
						String process = (type == SERVICE ? serviceProcessInfos.get(item.getKey()) : null);
						targetComponentInfos.add(new ComponentInfo(item.getKey(), type, getPackageName(), process));
						break;
					}
				}
			}
			return targetComponentInfos;
		}
		return null;
	}
}
