package com.tws.plugin.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;

import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.PluginManagerProvider;

import qrom.component.log.QRomLog;

public class ProcessUtil {
    private static final String TAG = "rick_Print:ProcessUtil";

    //插件运行所在的进程
    public static int PLUGIN_PROCESS_INDEX_HOST = 0;
    public static int PLUGIN_PROCESS_INDEX_MASTER = 1;
    public static int PLUGIN_PROCESS_INDEX_MINOR = 2;

    // 这是一个潜规则，插件的进程除PluginManagerProvider的标配外，其他的都统一规定前缀：
    private static final String PLUGIN_MULTI_PROCESS_SUFFIX = ":plugin";

    private static final String PLUGIN_MASTER_PROCESS_SUFFIX = ":pmaster";  //PluginManagerProvider 就配置在插件master进程里面
    private static final String PLUGIN_MINOR_PROCESS_SUFFIX = ":pminor";

    private static Boolean isPluginProcess = null;
    private static Boolean isHostProcess = null;

    public static String getHostProcessName() {
        return PluginLoader.getHostPackageName();
    }

    public static boolean isPluginProcess(Context context) {
        ensure(context);
        return isPluginProcess;
    }

    public static boolean isHostProcess(Context context) {
        ensure(context);
        return isHostProcess;
    }

    private static void ensure(Context context) {
        // 注意：当前宿主和插件是一个进程
        if (isPluginProcess == null) {
            final String hostProcessName = PluginApplication.getInstance().getPackageName();  //rick_Note：注意这里使用了系统默认的方式指定进程，如果自己指定了主进程的名称需要做处理
            final String pluginMasterProcessName = getPluginMasterProcessName(context);
            final String pluginMinorProcessName = getPluginMinorProcessName(context);

            String processName = getCurProcessName(context);

            isHostProcess = hostProcessName.equals(processName);
            // 这是一个潜规则，插件的进程除PluginManagerProvider的标配外，其他的都统一规定前缀："HostPackageName:plugin"+"编号";
            //rick_Note:这里是否要 ‘||’ isHostProcess，是有争议的，原则上是 存在运行在宿主中的插件才需要，不过这里做成多进程管理，宿主有需要知道哪些插件安装了，这样就需要这个操作
            isPluginProcess = isHostProcess || pluginMasterProcessName.equals(processName) || pluginMinorProcessName.equals(processName)
                    || processName.startsWith(PluginApplication.getInstance().getPackageName() + PLUGIN_MULTI_PROCESS_SUFFIX); // 注意这里不能用PluginLoader的Application
        }
    }

    public static boolean isPluginProcess() {
        return isPluginProcess(PluginLoader.getApplication());
    }

    public static boolean isHostProcess() {
        return isHostProcess(PluginLoader.getApplication());
    }

    public static String getCurProcessName(Context context) {
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

    public static String getPluginMasterProcessName(Context context) {
        try {
            // 这里取个巧, 直接查询ContentProvider的信息中包含的processName,因为Contentprovider是被配置为插件pmaster进程.但是这个api只支持9及以上,
            ProviderInfo pinfo = context.getPackageManager().getProviderInfo(new ComponentName(context, PluginManagerProvider.class), 0);
            return pinfo.processName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //:pmaster
        return PluginApplication.getInstance().getPackageName() + PLUGIN_MASTER_PROCESS_SUFFIX;
    }

    private static String getPluginMinorProcessName(Context context) {
        return PluginApplication.getInstance().getPackageName() + PLUGIN_MINOR_PROCESS_SUFFIX;
    }
}
