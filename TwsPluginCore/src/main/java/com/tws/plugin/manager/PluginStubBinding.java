package com.tws.plugin.manager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Base64;

import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.ProcessUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import qrom.component.log.QRomLog;

/**
 * 插件组件动态绑定到宿主的虚拟stub组件
 */
class PluginStubBinding {

    private static final String TAG = "rick_Print:PluginStubBinding";

    private static String PREFERENCES_NAME_HOST_PROCESS_SERVICES = "sp_host_process_serviceMapping";
    private static String KEY_HOST_PROCESS_SERVICES_MAP = "host.process.serviceMapping";

    private static String PREFERENCES_NAME_PMASTER_PROCESS_SERVICES = "sp_pmaster_process_serviceMapping";
    private static String KEY_PMASTER_PROCESS_SERVICES_MAP = "pmaster.process.serviceMapping";

    private static String PREFERENCES_NAME_PMINOR_PROCESS_SERVICES = "sp_pminor_process_serviceMapping";
    private static String KEY_PMINOR_PROCESS_SERVICES_MAP = "pminor.process.serviceMapping";

    private static String KEY_MP_SERVICE_MAP_PREFERENCES_NAME = "plugins.mp.serviceMapping";
    private static String KEY_MP_SERVICE_MAP_MAP_PREFERENCES_NAME = "plugins.mp.serviceMapping.map";

    //专门为单独进程服务申明的
    private static String buildMpDefaultAction() {
        return "com.rick.tws.plugin.MP_STUB_DEFAULT";
    }

    // ACTION是固定的，在AndroidManifest.xml里面申明就确定好了
    //receiver 一直是跑在Host进程里面的
    private static String buildHostAction() {
        return "com.rick.tws.pluginhost.STUB_DEFAULT";
    }

    private static String buildMasterAction() {
        return "com.rick.tws.pluginmaster.STUB_DEFAULT";
    }

    private static String buildMinorAction() {
        return "com.rick.tws.pluginminor.STUB_DEFAULT";
    }

    //预设Activity stub的坑位数量
    private static final int STUB_ACTIVITY_INITIAL_CAPACITY = 5;
    //预设Activity stub的坑位数量
    private static final int STUB_SERVICE_INITIAL_CAPACITY = 9;
    //预设Activity stub的坑位数量
    private static final int STUB_MP_SERVICE_ACTIVITY_INITIAL_CAPACITY = 5;
    // <stub组件，插件组件>通过这种映射来维护绑定关系
    //运行在宿主进程的stub组件
    //////////////////////////////////////////// 01 host process begin ////////////////////////////////////////////
    /**
     * key:stub Activity Name value:plugin Activity Name
     */
    private static HashMap<String, String> hProcessSTaskActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> hProcessSTopActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> hProcessSIActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static String hProcessStandardActivity = null;
    /**
     * key:stub Service Name value:plugin Service Name
     */
    private static HashMap<String, String> hProcessServices = new HashMap<String, String>(STUB_SERVICE_INITIAL_CAPACITY);
    //////////////////////////////////////////// 01 host process end ////////////////////////////////////////////

    //运行在pmaster进程的stub组件
    //////////////////////////////////////////// 02 pmaster process begin ////////////////////////////////////////////
    /**
     * key:stub Activity Name value:plugin Activity Name
     */
    private static HashMap<String, String> pMasterSTaskActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> pMasterSTopActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> pMasterSIActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static String pMasterStandardActivity = null;
    /**
     * key:stub Service Name value:plugin Service Name
     */
    private static HashMap<String, String> pMasterServices = new HashMap<String, String>(STUB_SERVICE_INITIAL_CAPACITY);
    //////////////////////////////////////////// 02 pmaster process begin ////////////////////////////////////////////

    //运行在pminor进程的stub组件
    //////////////////////////////////////////// 03 pminor process begin ////////////////////////////////////////////
    /**
     * key:stub Activity Name value:plugin Activity Name
     */
    private static HashMap<String, String> pMinorSTaskActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> pMinorSTopActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static HashMap<String, String> pMinorSIActivitys = new HashMap<String, String>(STUB_ACTIVITY_INITIAL_CAPACITY);
    private static String pMinorStandardActivity = null;
    /**
     * key:stub Service Name value:plugin Service Name
     */
    private static HashMap<String, String> pMinorServices = new HashMap<String, String>(STUB_SERVICE_INITIAL_CAPACITY);
    //////////////////////////////////////////// 03 pminor process begin ////////////////////////////////////////////

    //Activity stub绑定关系cache
    private static HashMap<String, ActivityStubInfo> bindedActivityStubCache = new HashMap<String, ActivityStubInfo>(16);
    private static HashMap<String, BindStubInfo> bindedServiceStubCache = new HashMap<String, BindStubInfo>(16);

    private static String receiver = null;
    //这个属性用于有些插件依赖第三方库，而第三方库运行了一些service指定单独进程 - 并且在运行结束后会主动回收
    private static HashMap<String, String> mpServiceMapping = new HashMap<String, String>(STUB_MP_SERVICE_ACTIVITY_INITIAL_CAPACITY);

    private static boolean isPoolInited = false;

    private static void initPool() {
        if (!ProcessUtil.isPluginProcess()) {
            throw new IllegalAccessError("此类只能在插件所在进程使用");
        }

        if (isPoolInited) {
            return;
        }

        loadHostProcessStubActivity();
        loadPluginMasterProcessStubActivity();
        loadPluginMinorProcessStubActivity();

        loadHostProcessStubService();
        loadPluginMasterProcessStubService();
        loadPluginMinorProcessStubService();

        loadMpStubService();

        loadStubReceiver();

        isPoolInited = true;
    }

    private static void loadHostProcessStubActivity() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildHostAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentActivities(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                    hProcessSTaskActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                    hProcessSTopActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                    hProcessSIActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE) {
                    hProcessStandardActivity = resolveInfo.activityInfo.name;
                }
            }
        }
    }

    private static void loadPluginMasterProcessStubActivity() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildMasterAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentActivities(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                    pMasterSTaskActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                    pMasterSTopActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                    pMasterSIActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE) {
                    pMasterStandardActivity = resolveInfo.activityInfo.name;
                }
            }
        }
    }

    private static void loadPluginMinorProcessStubActivity() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildMinorAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentActivities(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
                    pMinorSTaskActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {
                    pMinorSTopActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                    pMinorSIActivitys.put(resolveInfo.activityInfo.name, null);
                } else if (resolveInfo.activityInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE) {
                    pMinorStandardActivity = resolveInfo.activityInfo.name;
                }
            }
        }
    }

    private static synchronized void loadHostProcessStubService() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildHostAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentServices(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                hProcessServices.put(resolveInfo.serviceInfo.name, null);
            }

            HashMap<String, String> mapping = restore(ProcessUtil.PLUGIN_PROCESS_INDEX_HOST);
            boolean modifyStore = false;
            if (null != mapping) {
                Iterator<String> iter = mapping.keySet().iterator();
                String stubName, pluginServiceClassName;
                while (iter.hasNext()) {
                    stubName = iter.next();
                    if (hProcessServices.containsKey(stubName)) {
                        pluginServiceClassName = mapping.get(stubName);
                        hProcessServices.put(stubName, pluginServiceClassName);

                        //这里记录一下 插件组件的绑定关系，方便后面查询&解绑操作
                        bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, ProcessUtil.PLUGIN_PROCESS_INDEX_HOST));
                    } else {
                        modifyStore = true;
                        // 可能是版本升级做了调整,那就直接丢弃掉
                    }
                }
            }

            if (modifyStore) {
                save(hProcessServices, ProcessUtil.PLUGIN_PROCESS_INDEX_HOST);
            }
        }
    }

    private static synchronized void loadPluginMasterProcessStubService() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildMasterAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentServices(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                pMasterServices.put(resolveInfo.serviceInfo.name, null);
            }

            HashMap<String, String> mapping = restore(ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER);
            boolean modifyStore = false;
            if (null != mapping) {
                Iterator<String> iter = mapping.keySet().iterator();
                String stubName, pluginServiceClassName;
                while (iter.hasNext()) {
                    stubName = iter.next();
                    if (pMasterServices.containsKey(stubName)) {
                        pluginServiceClassName = mapping.get(stubName);
                        pMasterServices.put(stubName, pluginServiceClassName);

                        //这里记录一下 插件组件的绑定关系，方便后面查询&解绑操作
                        bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER));
                    } else {
                        // 可能是版本升级做了调整,那就直接丢弃掉
                        modifyStore = true;
                    }
                }
            }

            if (modifyStore) {
                save(pMasterServices, ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER);
            }
        }
    }

    private static synchronized void loadPluginMinorProcessStubService() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildMinorAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentServices(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                pMinorServices.put(resolveInfo.serviceInfo.name, null);
            }

            HashMap<String, String> mapping = restore(ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR);
            boolean modifyStore = false;
            if (null != mapping) {
                Iterator<String> iter = mapping.keySet().iterator();
                String stubName, pluginServiceClassName;
                while (iter.hasNext()) {
                    stubName = iter.next();
                    if (pMinorServices.containsKey(stubName)) {
                        pluginServiceClassName = mapping.get(stubName);
                        pMinorServices.put(stubName, pluginServiceClassName);

                        //这里记录一下 插件组件的绑定关系，方便后面查询&解绑操作
                        bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER));
                    } else {
                        // 可能是版本升级做了调整,那就直接丢弃掉
                        modifyStore = true;
                    }
                }
            }

            if (modifyStore) {
                save(pMinorServices, ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR);
            }
        }
    }

    private static synchronized void loadMpStubService() {
        Intent launchModeIntent = new Intent();
        launchModeIntent.setAction(buildMpDefaultAction());
        launchModeIntent.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> list = PluginLoader.getApplication().getPackageManager().queryIntentServices(launchModeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != list && 0 < list.size()) {
            for (ResolveInfo resolveInfo : list) {
                mpServiceMapping.put(resolveInfo.serviceInfo.name, null);
            }

            HashMap<String, String> mapping = restore(ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE);
            boolean modifyStore = false;
            if (null != mapping) {
                Iterator<String> iter = mapping.keySet().iterator();
                String stubName, pluginServiceClassName;
                while (iter.hasNext()) {
                    stubName = iter.next();
                    if (mpServiceMapping.containsKey(stubName)) {
                        pluginServiceClassName = mapping.get(stubName);
                        mpServiceMapping.put(stubName, pluginServiceClassName);

                        //这里记录一下 插件组件的绑定关系，方便后面查询&解绑操作
                        bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE));
                    } else {
                        // 可能是版本升级做了调整,那就直接丢弃掉
                        modifyStore = true;
                    }
                }
            }

            if (modifyStore) {
                save(mpServiceMapping, ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE);
            }
        }
    }

    private static void loadStubReceiver() {
        Intent exactStub = new Intent();
        exactStub.setAction(buildHostAction());
        exactStub.setPackage(PluginLoader.getApplication().getPackageName());

        List<ResolveInfo> resolveInfos = PluginLoader.getApplication().getPackageManager().queryBroadcastReceivers(exactStub, PackageManager.MATCH_DEFAULT_ONLY);

        if (null != resolveInfos && 0 < resolveInfos.size()) {
            receiver = resolveInfos.get(0).activityInfo.name;
        }
    }

    public static String bindStubReceiver() {
        initPool();
        return receiver;
    }

    public static synchronized String bindStubActivity(String pluginActivityClassName, int launchMode, int pIndex) {

        initPool();

        HashMap<String, String> stubActivitys = null;
        String tandardActivity = "";
        switch (pIndex) {
            case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST: {   //host进程stbu
                switch (launchMode) {
                    case ActivityInfo.LAUNCH_SINGLE_TASK:
                        stubActivitys = hProcessSTaskActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_TOP:
                        stubActivitys = hProcessSTopActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                        stubActivitys = hProcessSIActivitys;
                        break;
                    default:                                //ActivityInfo.LAUNCH_MULTIPLE
                        return hProcessStandardActivity;
                }
                tandardActivity = hProcessStandardActivity;
            }
            break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER: { //pMaster进程stbu
                switch (launchMode) {
                    case ActivityInfo.LAUNCH_SINGLE_TASK:
                        stubActivitys = pMasterSTaskActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_TOP:
                        stubActivitys = pMasterSTopActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                        stubActivitys = pMasterSIActivitys;
                        break;
                    default:                                //ActivityInfo.LAUNCH_MULTIPLE
                        return pMasterStandardActivity;
                }
                tandardActivity = pMasterStandardActivity;
            }
            break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR: {  //pMinor进程stbu
                switch (launchMode) {
                    case ActivityInfo.LAUNCH_SINGLE_TASK:
                        stubActivitys = pMinorSTaskActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_TOP:
                        stubActivitys = pMinorSTopActivitys;
                        break;
                    case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                        stubActivitys = pMinorSIActivitys;
                        break;
                    default:                                //ActivityInfo.LAUNCH_MULTIPLE
                        return pMinorStandardActivity;
                }
                tandardActivity = pMinorStandardActivity;
            }
            break;
            default:
                throw new IllegalAccessError("插件Activity组件当前只允许运行在[宿主(0)、插件master(1)、插件minor(2)]三个进程范围内，pIndex:" + pIndex + " 并不在这个范围内");
        }

        if (null != stubActivitys) {
            Iterator<Map.Entry<String, String>> itr = stubActivitys.entrySet().iterator();
            String stubName = null;

            String pluginActivityName = "";
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                pluginActivityName = entry.getValue();
                if (null == pluginActivityName) {   //如果当前stub组件没有绑定插件的组件
                    if (null == stubName) {         //[如果还没确定目标stub]将stub组件名取出并赋值给目标stbuName
                        stubName = entry.getKey();  //这里找到空闲的stubactivity以后，还需继续遍历，用来检查是否pluginActivityClassName已经绑定过了
                    }
                } else if (pluginActivityClassName.equals(pluginActivityName)) { //如果当前stub组件有绑定插件组件，被绑定的插件组件刚好和当前要处理的插件组件一致，直接返回
                    BindStubInfo bindStubInfo = bindedActivityStubCache.get(pluginActivityClassName);
                    stubName = entry.getKey();
                    if (null == bindStubInfo) {
                        //进这里是正常的，之前有绑定过，后来被解绑了就为null了
                        bindedActivityStubCache.put(pluginActivityClassName, new ActivityStubInfo(stubName, pIndex, launchMode));
                    } else if (!stubName.equals(bindStubInfo.stubName) || pIndex != bindStubInfo.pIndex) {
                        //先更新一下
                        bindedActivityStubCache.put(pluginActivityClassName, new ActivityStubInfo(stubName, pIndex, launchMode));

                        //当前的绑定结果和之前的结果不一样，可能是存在两个插件同名的组件，需要预警一下
                        QRomLog.e(TAG, "call bindStubActivity(" + pluginActivityClassName + ", " + launchMode + ", " + pIndex + ") 发现组件已经绑定了，绑定的stubInfo却是：" + bindStubInfo);
                    }

                    // 已绑定过，直接返回【上面的判断是为了校准之前的赋值】
                    return entry.getKey();
                }
            }

            // 没有绑定到StubActivity，而且还有空余的stubActivity，进行绑定
            if (null != stubName) {
                stubActivitys.put(stubName, pluginActivityClassName);

                //这里记录一下 插件组件的绑定关系，方便后面解绑操作
                bindedActivityStubCache.put(pluginActivityClassName, new ActivityStubInfo(stubName, pIndex, launchMode));

                return stubName;
            } else {
                Exception here = new Exception();
                here.fillInStackTrace();
                QRomLog.e(TAG, "预什么进程pIndex:" + pIndex + " launchMode:" + launchMode
                        + " 的Activity坑位出现了不够用的情况~~~【将用标准的activity坑位代替】", here);
            }

        }

        return tandardActivity;
    }


    public static synchronized void unBindLaunchModeStubActivity(String stubActivityName, String pluginActivityName) {
        QRomLog.i(TAG, "call unBindLaunchModeStubActivity:" + stubActivityName + " pluginActivityName is " + pluginActivityName);
        final ActivityStubInfo bindStubInfo = bindedActivityStubCache.get(pluginActivityName);
        if (null != bindStubInfo) {
            //对于standard和singleTop的launchmode，不做处理。
            if ((bindStubInfo.launchMode == ActivityInfo.LAUNCH_MULTIPLE || bindStubInfo.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP)) {
                return;
            }

            QRomLog.i(TAG, "get bindStubInfo from cache is " + bindStubInfo);
            HashMap<String, String> stubActivitys = null;
            switch (bindStubInfo.pIndex) {
                case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST: {   //host进程stbu
                    switch (bindStubInfo.launchMode) {
                        case ActivityInfo.LAUNCH_SINGLE_TASK:
                            stubActivitys = hProcessSTaskActivitys;
                            break;
                        case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                            stubActivitys = hProcessSIActivitys;
                            break;
                        default:
                            break;
                    }
                }
                break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER: { //pMaster进程stbu
                    switch (bindStubInfo.launchMode) {
                        case ActivityInfo.LAUNCH_SINGLE_TASK:
                            stubActivitys = pMasterSTaskActivitys;
                            break;
                        case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                            stubActivitys = pMasterSIActivitys;
                            break;
                        default:
                            break;
                    }
                }
                break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR: {  //pMinor进程stbu
                    switch (bindStubInfo.launchMode) {
                        case ActivityInfo.LAUNCH_SINGLE_TASK:
                            stubActivitys = pMinorSTaskActivitys;
                            break;
                        case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                            stubActivitys = pMinorSIActivitys;
                            break;
                        default:
                            break;
                    }
                }
                break;
                default:
                    break;
            }

            if (null != stubActivitys) {
                stubActivitys.put(stubActivityName, null);
                QRomLog.i(TAG, "找到绑定关系，成功解绑！");

                //处理缓存记录
                bindedActivityStubCache.put(pluginActivityName, null);

                return;
            }
        }

        QRomLog.i(TAG, "缓存失效，下面就只能通过遍历来处理解绑~");
        if (pluginActivityName.equals(hProcessSTaskActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals hProcessSTaskActivitys");
            hProcessSTaskActivitys.put(stubActivityName, null);
        } else if (pluginActivityName.equals(hProcessSIActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals hProcessSIActivitys");
            hProcessSIActivitys.put(stubActivityName, null);
        } else if (pluginActivityName.equals(pMasterSTaskActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals pMasterSTaskActivitys");
            pMasterSTaskActivitys.put(stubActivityName, null);
        } else if (pluginActivityName.equals(pMasterSIActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals pMasterSIActivitys");
            pMasterSIActivitys.put(stubActivityName, null);
        } else if (pluginActivityName.equals(pMinorSTaskActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals pMinorSTaskActivitys");
            pMinorSTaskActivitys.put(stubActivityName, null);
        } else if (pluginActivityName.equals(pMinorSIActivitys.get(stubActivityName))) {
            QRomLog.i(TAG, "equals pMinorSIActivitys");
            pMinorSIActivitys.put(stubActivityName, null);
        } else {
            QRomLog.i(TAG, "对于standard和singleTop的launchmode，不做处理。");
        }

    }

    public static synchronized String getBindedPluginServiceName(String stubServiceName) {
        initPool();

        //先从cache中获取
        Iterator<Map.Entry<String, BindStubInfo>> cacheItr = bindedServiceStubCache.entrySet().iterator();
        BindStubInfo bindStubInfo;
        while (cacheItr.hasNext()) {
            Map.Entry<String, BindStubInfo> entry = cacheItr.next();
            bindStubInfo = entry.getValue();
            if (null != bindStubInfo && bindStubInfo.stubName.equals(stubServiceName)) {
                return entry.getKey();
            }
        }

        //cache失效了就只能一个一个遍历获取
        //宿主进程坑位
        Iterator<Map.Entry<String, String>> itr = hProcessServices.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            if (entry.getKey().equals(stubServiceName)) {
                return entry.getValue();
            }
        }

        //pMaster进程坑位
        Iterator<Map.Entry<String, String>> itrMaster = pMasterServices.entrySet().iterator();
        while (itrMaster.hasNext()) {
            Map.Entry<String, String> entry = itrMaster.next();
            if (entry.getKey().equals(stubServiceName)) {
                return entry.getValue();
            }
        }

        //pMinor进程坑位
        Iterator<Map.Entry<String, String>> itrMinor = pMinorServices.entrySet().iterator();
        while (itrMinor.hasNext()) {
            Map.Entry<String, String> entry = itrMinor.next();
            if (entry.getKey().equals(stubServiceName)) {
                return entry.getValue();
            }
        }

        // 没找到尝试MP里面
        Iterator<Map.Entry<String, String>> mpItr = mpServiceMapping.entrySet().iterator();
        while (mpItr.hasNext()) {
            Map.Entry<String, String> entry = mpItr.next();
            if (entry.getKey().equals(stubServiceName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static synchronized String bindStubService(String pluginServiceClassName, int pIndex) {
        initPool();
        HashMap<String, String> stubServices = null;
        switch (pIndex) {
            case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST:
                stubServices = hProcessServices;
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER:
                stubServices = pMasterServices;
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR:
                stubServices = pMinorServices;
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE:
                stubServices = mpServiceMapping;
                break;
            default:
                throw new IllegalAccessError("插件Service组件当前只允许运行在[宿主(0)、插件master(1)、插件minor(2)、自定义(3)]四中情况范围内，pIndex:" + pIndex + " 并不在这个范围内");
        }
        Iterator<Map.Entry<String, String>> itr = stubServices.entrySet().iterator();

        String stubName = null;
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            if (entry.getValue() == null) {
                if (stubName == null) {
                    stubName = entry.getKey();
                    // 这里找到空闲的idleStubServiceName以后，还需继续遍历，用来检查是否pluginServiceClassName已经绑定过了
                }
            } else if (pluginServiceClassName.equals(entry.getValue())) {
                // 已经绑定过，直接返回
                QRomLog.i(TAG, "已经绑定过:" + entry.getKey() + " pluginServiceClassName is " + pluginServiceClassName);
                stubName = entry.getKey();

                BindStubInfo bindStubInfo = bindedServiceStubCache.get(pluginServiceClassName);

                //service没有launchMode,直接给-1就行
                if (null == bindStubInfo) {
                    //进这里是正常的，之前有绑定过，后来被解绑了就为null了
                    bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, pIndex));
                } else if (!stubName.equals(bindStubInfo.stubName) || pIndex != bindStubInfo.pIndex) {
                    //先更新一下
                    bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, pIndex));

                    //当前的绑定结果和之前的结果不一样，可能是存在两个插件同名的组件，需要预警一下
                    QRomLog.e(TAG, "call bindStubService(" + pluginServiceClassName + ", " + pIndex + ") 发现组件已经绑定了，绑定的stubInfo却是：" + bindStubInfo);
                }

                return stubName;
            }
        }

        // 没有绑定到StubService，而且还有空余的StubService，进行绑定
        if (null != stubName) {
            QRomLog.i(TAG, "添加绑定:" + stubName + " pluginServiceClassName is " + pluginServiceClassName);
            stubServices.put(stubName, pluginServiceClassName);

            //这里记录一下 插件组件的绑定关系，方便后面解绑操作
            bindedServiceStubCache.put(pluginServiceClassName, new BindStubInfo(stubName, pIndex));

            // 对serviceMapping持久化是因为如果service处于运行状态时app发生了crash，系统会自动恢复之前的service，此时插件映射信息查不到的话会再次crash
            save(stubServices, pIndex);

            return stubName;
        }

        // 绑定失败
        return null;
    }

    public static synchronized void unBindStubService(String pluginServiceName) {
        final BindStubInfo bindStubInfo = bindedServiceStubCache.get(pluginServiceName);
        if (null != bindStubInfo) {
            HashMap<String, String> stubServices = null;
            switch (bindStubInfo.pIndex) {
                case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST:
                    stubServices = hProcessServices;
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER:
                    stubServices = pMasterServices;
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR:
                    stubServices = pMinorServices;
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE:
                    stubServices = mpServiceMapping;
                    break;
            }

            if (null != stubServices) {
                stubServices.put(pluginServiceName, null);
                QRomLog.i(TAG, "unBindStubService 找到绑定关系，成功解绑！");
                save(stubServices, bindStubInfo.pIndex);

                //处理缓存记录
                bindedServiceStubCache.put(pluginServiceName, null);
                return;
            }
        }

        QRomLog.i(TAG, "unBindStubService 缓存失效，下面就只能通过遍历来处理解绑~");

        //宿主进程坑位
        Iterator<Map.Entry<String, String>> itr = hProcessServices.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            if (pluginServiceName.equals(entry.getValue())) {
                // 如果存在绑定关系，解绑
                QRomLog.i(TAG, "回收绑定 Key:" + entry.getKey() + " Value:" + entry.getValue());
                hProcessServices.put(entry.getKey(), null);
                save(hProcessServices, ProcessUtil.PLUGIN_PROCESS_INDEX_HOST);
                break;
            }
        }

        //pmaster进程坑位
        Iterator<Map.Entry<String, String>> itrMaster = pMasterServices.entrySet().iterator();
        while (itrMaster.hasNext()) {
            Map.Entry<String, String> entry = itrMaster.next();
            if (pluginServiceName.equals(entry.getValue())) {
                // 如果存在绑定关系，解绑
                QRomLog.i(TAG, "回收绑定 Key:" + entry.getKey() + " Value:" + entry.getValue());
                pMasterServices.put(entry.getKey(), null);
                save(pMasterServices, ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER);
                break;
            }
        }

        //pminor进程坑位
        Iterator<Map.Entry<String, String>> itrMinor = pMinorServices.entrySet().iterator();
        while (itrMinor.hasNext()) {
            Map.Entry<String, String> entry = itrMinor.next();
            if (pluginServiceName.equals(entry.getValue())) {
                // 如果存在绑定关系，解绑
                QRomLog.i(TAG, "回收绑定 Key:" + entry.getKey() + " Value:" + entry.getValue());
                pMinorServices.put(entry.getKey(), null);
                save(pMinorServices, ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR);
                break;
            }
        }

        //如果在mpServiceMapping中get到了绑定的servive,需要做解绑操作
        Iterator<Map.Entry<String, String>> mpItr = mpServiceMapping.entrySet().iterator();
        while (mpItr.hasNext()) {
            Map.Entry<String, String> entry = mpItr.next();
            if (pluginServiceName.equals(entry.getValue())) {
                // 如果存在绑定关系，解绑
                QRomLog.i(TAG, "回收绑定 Key:" + entry.getKey() + " Value:" + entry.getValue());
                mpServiceMapping.put(entry.getKey(), null);
                save(mpServiceMapping, ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE);
                break;
            }
        }
    }

    public static String dumpServieInfo() {
        return hProcessServices.toString() + "" + pMasterServices.toString() + "" + pMinorServices.toString() + "" + mpServiceMapping.toString();
    }

    private static boolean save(HashMap<String, String> mapping, int pIndex) {
        ObjectOutputStream objectOutputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(mapping);
            objectOutputStream.flush();

            byte[] data = byteArrayOutputStream.toByteArray();
            String list = Base64.encodeToString(data, Base64.DEFAULT);

            switch (pIndex) {
                case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST:
                    PluginLoader.getApplication()
                            .getSharedPreferences(PREFERENCES_NAME_HOST_PROCESS_SERVICES, Context.MODE_PRIVATE).edit()
                            .putString(KEY_HOST_PROCESS_SERVICES_MAP, list).apply();
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER:
                    PluginLoader.getApplication()
                            .getSharedPreferences(PREFERENCES_NAME_PMASTER_PROCESS_SERVICES, Context.MODE_PRIVATE).edit()
                            .putString(KEY_PMASTER_PROCESS_SERVICES_MAP, list).apply();
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR:
                    PluginLoader.getApplication()
                            .getSharedPreferences(PREFERENCES_NAME_PMINOR_PROCESS_SERVICES, Context.MODE_PRIVATE).edit()
                            .putString(KEY_PMINOR_PROCESS_SERVICES_MAP, list).apply();
                    break;
                case ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE:
                    PluginLoader.getApplication()
                            .getSharedPreferences(KEY_MP_SERVICE_MAP_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                            .putString(KEY_MP_SERVICE_MAP_MAP_PREFERENCES_NAME, list).apply();
                    break;
                default:
                    throw new IllegalAccessError("call save - 插件Service组件当前只允许运行在[宿主(0)、插件master(1)、插件minor(2)、自定义(3)]四中情况范围内，pIndex:" + pIndex + " 并不在这个范围内");
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != objectOutputStream) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != byteArrayOutputStream) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        QRomLog.e(TAG, "save:" + pIndex + " failed!");
        return false;
    }

    private static HashMap<String, String> restore(int pIndex) {
        String list = null;
        switch (pIndex) {
            case ProcessUtil.PLUGIN_PROCESS_INDEX_HOST:
                list = PluginLoader.getApplication()
                        .getSharedPreferences(PREFERENCES_NAME_HOST_PROCESS_SERVICES, Context.MODE_PRIVATE)
                        .getString(KEY_HOST_PROCESS_SERVICES_MAP, "");
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MASTER:
                list = PluginLoader.getApplication()
                        .getSharedPreferences(PREFERENCES_NAME_PMASTER_PROCESS_SERVICES, Context.MODE_PRIVATE)
                        .getString(KEY_PMASTER_PROCESS_SERVICES_MAP, "");
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_MINOR:
                list = PluginLoader.getApplication()
                        .getSharedPreferences(PREFERENCES_NAME_PMINOR_PROCESS_SERVICES, Context.MODE_PRIVATE)
                        .getString(KEY_PMINOR_PROCESS_SERVICES_MAP, "");
                break;
            case ProcessUtil.PLUGIN_PROCESS_INDEX_CUSTOMIZE:
                list = PluginLoader.getApplication()
                        .getSharedPreferences(KEY_MP_SERVICE_MAP_PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .getString(KEY_MP_SERVICE_MAP_MAP_PREFERENCES_NAME, "");
                break;
            default:
                throw new IllegalAccessError("call save - 插件Service组件当前只允许运行在[宿主(0)、插件master(1)、插件minor(2)、自定义(3)]四中情况范围内，pIndex:" + pIndex + " 并不在这个范围内");
        }

        Serializable object = null;
        if (!TextUtils.isEmpty(list)) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decode(list, Base64.DEFAULT));
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(byteArrayInputStream);
                object = (Serializable) objectInputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != objectInputStream) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (null != byteArrayInputStream) {
                    try {
                        byteArrayInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (null != object) {
            HashMap<String, String> mapping = (HashMap<String, String>) object;
            return mapping;
        }

        return null;
    }

    public static boolean isStub(String className, int type) {
        initPool();

        switch (type) {
            case DisplayItem.TYPE_BROADCAST: {
                return className.equals(receiver);
            }
            case DisplayItem.TYPE_ACTIVITY: {
                return className.equals(hProcessStandardActivity) || hProcessSTaskActivitys.containsKey(className) || hProcessSTopActivitys.containsKey(className) || hProcessSIActivitys.containsKey(className) ||
                        className.equals(pMasterStandardActivity) || pMasterSTaskActivitys.containsKey(className) || pMasterSTopActivitys.containsKey(className) || pMasterSIActivitys.containsKey(className) ||
                        className.equals(pMinorStandardActivity) || pMinorSTaskActivitys.containsKey(className) || pMinorSTopActivitys.containsKey(className) || pMinorSIActivitys.containsKey(className);
            }
            case DisplayItem.TYPE_SERVICE: {
                return hProcessServices.containsKey(className) || pMasterServices.containsKey(className)
                        || pMinorServices.containsKey(className) || mpServiceMapping.containsKey(className);
            }
            default:
                throw new IllegalAccessError("isStub接口当前只用于判断广播、activity、service三种哦~");
        }
    }
}
