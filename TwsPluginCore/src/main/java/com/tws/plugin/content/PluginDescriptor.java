package com.tws.plugin.content;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tws.plugin.bridge.TwsPluginBridgeActivity;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.ProcessUtil;
import com.tws.plugin.util.ResourceUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import qrom.component.log.QRomLog;

/**
 * <Pre>
 *
 * @author yongchen
 *         </Pre>
 */
public class PluginDescriptor implements Serializable {
    protected static final String TAG = "rick_Print:PluginDescriptor";
    private static final long serialVersionUID = -5245734825911798344L;

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

    // 插件进程info配置
    // rick_Note:注意这里可能需要另外添加一个逻辑：如果插件配置了要显示在宿主里面的exported-fragment，那么插件的进程就只能是host进程，否则这会显示不出来
    private int processIndex = ProcessUtil.PLUGIN_PROCESS_INDEX_HOST;

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

    private HashMap<String, PluginServiceInfo> serviceInfos = new HashMap<String, PluginServiceInfo>();

    /**
     * key: receiver class name value: intentfilter list
     */
    private HashMap<String, ArrayList<PluginIntentFilter>> receivers = new HashMap<String, ArrayList<PluginIntentFilter>>();

    private String installedPath;

    private String[] dependencies;

    private ArrayList<String> muliDexList;

    private ArrayList<DisplayItem> displayItems = null;

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

        final String[] values = version.split(DisplayItem.SEPARATOR_VER);
        return Integer.parseInt(values[0]);
    }

    public String getVersionName() {
        if (TextUtils.isEmpty(version))
            return "1.0";

        final String[] values = version.split(DisplayItem.SEPARATOR_VER);
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

    public int getProcessIndex() {
        return processIndex;
    }

    public void setProcessIndexByProcessName(String process) {
        QRomLog.i(TAG, "setProcessIndexByProcessName:" + process);
        //1、没有配置 2、配置了宿主的包名 3、配置了插件的包名 这三种情况会被视为跑在宿主进程中
        if (TextUtils.isEmpty(process) || process.equals(PluginLoader.getApplication().getPackageName()) || process.equals(packageName)) {
            this.processIndex = ProcessUtil.PLUGIN_PROCESS_INDEX_HOST;
        } else if (process.endsWith(":pminor")) { //除非特别指定了进程为pminor，否则不应该指定为：次要的插件进程
            this.processIndex = ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR;
        } else {
            this.processIndex = ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER;
        }
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

    public HashMap<String, PluginServiceInfo> getServiceInfos() {
        return serviceInfos;
    }

    public void setServiceInfos(HashMap<String, PluginServiceInfo> serviceInfos) {
        this.serviceInfos = serviceInfos;
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

    public ArrayList<DisplayItem> getDisplayItems() {
        return displayItems;
    }

    public void setDisplayConfigs(ArrayList<DisplayItem> displayItems) {
        this.displayItems = displayItems;
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
     * @param clsId
     * @return
     */
    public String getPluginClassNameById(String clsId) {
        String clsName = getFragments().get(clsId);

        if (clsName == null) {
            QRomLog.i(TAG, "className not found for classId:" + clsId);
        } else {
            QRomLog.i(TAG, "clsName found:" + clsName);
        }

        return clsName;
    }

    /**
     * 需要根据Id查询的只有fragment
     *
     * @param clsId
     * @return
     */
    public boolean containsFragment(String clsId) {
        if (getFragments().containsKey(clsId) && isEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * 这里当前只处理组大组件及fragment
     *
     * @return boolean is contains
     */
    public boolean containsComponent(String className, int type) {
        if (!isEnabled()) {
            return false;
        }

        switch (type) {
            case DisplayItem.TYPE_FRAGMENT:
                return getFragments().containsValue(className);
            case DisplayItem.TYPE_ACTIVITY:
                return getActivitys().containsKey(className);
            case DisplayItem.TYPE_SERVICE:
                return getServices().containsKey(className);
            case DisplayItem.TYPE_PROVIDER:
                return getProviderInfos().containsKey(className);
            case DisplayItem.TYPE_BROADCAST:
                return getReceivers().containsKey(className);
            default:
                return false;
        }
    }

    /**
     * 获取className的type类型,比如：activity、fragment、service等，没办法：不知道类型，只能遍历来get结果
     *
     * @return int type value
     */
    public int getClsNameType(String className) {
        if (!isEnabled()) {
            return DisplayItem.TYPE_UNKOWN;
        }

        if (getFragments().containsValue(className)) {
            return DisplayItem.TYPE_FRAGMENT;
        } else if (getActivitys().containsKey(className)) {
            return DisplayItem.TYPE_ACTIVITY;
        } else if (getReceivers().containsKey(className)) {
            return DisplayItem.TYPE_BROADCAST;
        } else if (getServices().containsKey(className)) {
            return DisplayItem.TYPE_SERVICE;
        } else if (getProviderInfos().containsKey(className)) {
            return DisplayItem.TYPE_PROVIDER;
        } else if (getApplicationName().equals(className) && !className.equals(Application.class.getName())) {
            return DisplayItem.TYPE_APPLICATION;
        } else {
            return DisplayItem.TYPE_UNKOWN;
        }
    }

    public List<ComponentInfo> matchPluginComponents(Intent intent, int type) {
        List<ComponentInfo> result = null;
        // 如果是通过组件进行匹配的, 这里忽略了packageName
        if (intent.getComponent() != null) {
            final String clsName = intent.getComponent().getClassName();
            if (containsComponent(clsName, type) && !TwsPluginBridgeActivity.class.getName().equals(clsName)) {
                result = new ArrayList<ComponentInfo>(1);

                int pIndex = getProcessIndex();
                String pName = ProcessUtil.getProcessNameByIndex(processIndex);
                switch (type) {
                    //目前就service存在可能和application不在一个进程
                    case DisplayItem.TYPE_SERVICE:
                        final PluginServiceInfo serviceInfo = serviceInfos.get(clsName);
                        if (null != serviceInfo) {
                            pName = serviceInfo.getProcessName();
                            pIndex = serviceInfo.getProcessIndex();
                        }
                        break;
                    default:
                        break;
                }
                result.add(new ComponentInfo(clsName, type, getPackageName(), pName, pIndex));
                return result;// 暂时不考虑不同的插件中配置了相同名称的组件的问题,先到先得
            }
        }

        // 如果是通过IntentFilter进行匹配的
        ArrayList<ComponentInfo> list = scanToMatchComponents(intent, type);
        if (list != null && 0 < list.size()) {
            switch (type) {
                case DisplayItem.TYPE_ACTIVITY:
                    result = new ArrayList<ComponentInfo>(1);
                    result.add(list.get(0));
                    return result;// 暂时不考虑多个Activity配置了相同的Intent的问题,先到先得
                case DisplayItem.TYPE_SERVICE:
                    result = new ArrayList<ComponentInfo>(1);
                    result.add(list.get(0));
                    return result;// service本身不支持多匹配,先到先得
                case DisplayItem.TYPE_BROADCAST:
                    result = new ArrayList<ComponentInfo>();
                    result.addAll(list);// 暂时不考虑去重的问题
                    return result;
                default:
                    break;
            }
        }

        return null;
    }

    private ArrayList<ComponentInfo> scanToMatchComponents(Intent intent, final int type) {
        final HashMap<String, ArrayList<PluginIntentFilter>> intentFilter;
        switch (type) {
            case DisplayItem.TYPE_ACTIVITY:
                intentFilter = getActivitys();
                break;
            case DisplayItem.TYPE_SERVICE:
                intentFilter = getServices();
                break;
            case DisplayItem.TYPE_BROADCAST:
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

                        int pIndex = getProcessIndex();
                        String pName = ProcessUtil.getProcessNameByIndex(processIndex);
                        switch (type) {
                            //目前就service存在可能和application不在一个进程
                            case DisplayItem.TYPE_SERVICE:
                                final PluginServiceInfo serviceInfo = serviceInfos.get(item.getKey());
                                if (null != serviceInfo) {
                                    pName = serviceInfo.getProcessName();
                                    pIndex = serviceInfo.getProcessIndex();
                                }
                                break;
                            default:
                                break;
                        }
                        targetComponentInfos.add(new ComponentInfo(item.getKey(), type, getPackageName(), pName, pIndex));
                        break;
                    }
                }
            }
            return targetComponentInfos;
        }
        return null;
    }
}
