package com.tws.plugin.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.text.TextUtils;

import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.PluginManagerProvider;

import qrom.component.log.QRomLog;

public class ProcessUtil {
    private static final String TAG = "rick_Print:ProcessUtil";

    //插件运行所在的进程
    public static final int PLUGIN_PROCESS_INDEX_HOST = 0;
    public static final int PLUGIN_PROCESS_INDEX_MASTER = 1;
    public static final int PLUGIN_PROCESS_INDEX_MINOR = 2;
    public static final int PLUGIN_PROCESS_INDEX_CUSTOMIZE = 3; //部分插件或者插件的第三方库需要单独配置组件进程

    // 这是一个潜规则，插件的进程除PluginManagerProvider的标配外，其他的都统一规定前缀：
    private static final String PLUGIN_MULTI_PROCESS_SUFFIX = ":plugin";

    private static final String PLUGIN_MASTER_PROCESS_SUFFIX = ":pmaster";  //PluginManagerProvider 就配置在插件master进程里面
    private static final String PLUGIN_MINOR_PROCESS_SUFFIX = ":pminor";

    private static Boolean isPluginProcess = null;
    private static Boolean isHostProcess = null;
    private static String hostProcessName = null;
    private static String masterProcessName = null;
    private static String minorProcessName = null;

    public static void initProcessName(Context context) {
        if (TextUtils.isEmpty(hostProcessName)) {

            //宿主进程名
            if (TextUtils.isEmpty(hostProcessName)) {
                hostProcessName = PluginApplication.getInstance().getPackageName();
            }

            try {
                // 先查询ContentProvider的信息中包含的processName,因为Contentprovider是运行在pmaster进程.
                ProviderInfo pinfo = context.getPackageManager().getProviderInfo(new ComponentName(context, PluginManagerProvider.class), 0);
                masterProcessName = pinfo.processName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            //上面如果获取异常，才使用包名作为宿主进程名【这样可以排除宿主指定了进程名的问题】
            if (TextUtils.isEmpty(masterProcessName)) {
                //注意这里需要和AndroidManifest.xml保持一致
                masterProcessName = hostProcessName + PLUGIN_MASTER_PROCESS_SUFFIX;
            }

            minorProcessName = hostProcessName + PLUGIN_MINOR_PROCESS_SUFFIX;
        }
    }

    public static boolean isHostProcess() {
        return isHostProcess(PluginLoader.getApplication());
    }

    public static boolean isHostProcess(Context context) {
        ensure(context);
        return isHostProcess;
    }

    public static boolean isPluginProcess() {
        return isPluginProcess(PluginLoader.getApplication());
    }

    public static boolean isPluginProcess(Context context) {
        ensure(context);
        return isPluginProcess;
    }

    private static void ensure(Context context) {
        // 注意：当前宿主和插件是一个进程
        if (isPluginProcess == null) {
            String processName = getCurProcessName(context);
            initProcessName(context);

            isHostProcess = hostProcessName.equals(processName);
            // 这是一个潜规则，插件的进程命名是有严格规范的，统一是主进程+一个后缀组成，而且必须由AndroidManifest.xml配合一起
            //rick_Note:这里是否要 ‘||’ isHostProcess，是有争议的，原则上是 存在运行在宿主中的插件才需要，不过这里做成多进程管理，宿主有需要知道哪些插件安装了，这样就需要这个操作
            isPluginProcess = isHostProcess || masterProcessName.equals(processName) || minorProcessName.equals(processName)
                    || processName.startsWith(hostProcessName + PLUGIN_MULTI_PROCESS_SUFFIX);
        }
    }

    public static String getProcessNameByIndex(int processIndex) {
        switch (processIndex) {
            case PLUGIN_PROCESS_INDEX_HOST:
                return hostProcessName;
            case PLUGIN_PROCESS_INDEX_MASTER:
                return masterProcessName;
            case PLUGIN_PROCESS_INDEX_MINOR:
                return minorProcessName;
            default: //自定义进程 有组件自己申明指定
                return null;
        }
    }

    private static String getCurProcessName(Context context) {
        final int pid = android.os.Process.myPid();
        QRomLog.i(TAG, "getCurProcessName pid=" + pid);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }

        return "";
    }

    public static String getHostProcessName() {
        return hostProcessName;
    }
}
