package com.tws.plugin.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.tws.plugin.content.LoadedPlugin;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.android.HackLayoutInflater;
import com.tws.plugin.core.compat.CompatForSupportv7ViewInflater;
import com.tws.plugin.core.proxy.systemservice.AndroidAppIActivityManager;
import com.tws.plugin.core.proxy.systemservice.AndroidAppIPackageManager;
import com.tws.plugin.core.proxy.systemservice.AndroidWebkitWebViewFactoryProvider;
import com.tws.plugin.manager.InstallResult;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.FileUtil;
import com.tws.plugin.util.ProcessUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import dalvik.system.BaseDexClassLoader;
import qrom.component.log.QRomLog;

public class PluginLoader {

    private static final String TAG = "rick_Print:PluginLoader";
    private static final String PLUGIN_SHAREED_PREFERENCE_NAME = "plugins.shared.preferences";
    private static final String VERSION_CODE_KEY = "version.code";
    private static final String VERSION_NAME_KEY = "version.name";
    private static Application sApplication;
    private static boolean isLoaderInited = false;
    private static boolean isLoaderPlugins = false;
    private static final String ASSETS_PLUGS_DIR = "plugins";
    private static String sHostPackageName;

    private PluginLoader() {
    }

    public static Application getApplication() {
        if (sApplication == null) {
            throw new IllegalStateException("框架尚未初始化，请确定在当前进程中，PluginLoader.initLoader方法已执行！");
        }
        return sApplication;
    }

    /**
     * 初始化loader, 只可调用一次
     *
     * @param app
     */
    public static synchronized void initPluginFramework(Application app) {
            if (!isLoaderInited) {
                QRomLog.i(TAG, "begin init PluginFramework...");
                long startTime = System.currentTimeMillis();

                isLoaderInited = true;
                sApplication = app;
                sHostPackageName = app.getPackageName();
                QRomLog.i(TAG, "begin init PluginFramework... HostPackageName is " + sHostPackageName);

                // 这里的isPluginProcess方法需要在安装AndroidAppIActivityManager之前执行一次。
                // 原因见AndroidAppIActivityManager的getRunningAppProcesses()方法
                boolean isPluginProcess = ProcessUtil.isPluginProcess();

                // 进行PendingIntent的resolve、进程欺骗等主要是为了让插件在四大组件之外的组件等单元也具备自己的运行权限
                AndroidAppIActivityManager.installProxy();

                // Notification在适配上出现了不少问题，暂时将这个交由宿主进行，如果DM出现了独立插件在放开进行适配
                // AndroidAppINotificationManager.installProxy();

                // 这里是修正插件运行单元的info信息
                AndroidAppIPackageManager.installProxy(sApplication.getPackageManager());

                if (isPluginProcess) {
                    HackLayoutInflater.installPluginCustomViewConstructorCache();
                    CompatForSupportv7ViewInflater.installPluginCustomViewConstructorCache();
                    // 不可在主进程中同步安装，因为此时ActivityThread还没有准备好, 会导致空指针。
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            // 这个注入 主要是为了解决插件加载本地页面问题
                            AndroidWebkitWebViewFactoryProvider.installProxy();
                        }
                    });
                }

                PluginInjector.injectHandlerCallback();// 本来宿主进程是不需要注入handlecallback的，这里加上是为了对抗360安全卫士等软件，提高Instrumentation的成功率
                PluginInjector.injectInstrumentation();
                PluginInjector.injectBaseContext(sApplication);

                if (isPluginProcess) {
                    if (Build.VERSION.SDK_INT >= 14) {
                        //Application中的registerActivityLifecycleCallbacks方法，可以在回调中把整个应用打开的Activity保存在集合中、销毁的Activity重集合中删除。
                        sApplication.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                            @Override
                            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                                //if (null == mActivitys) {
                                //    return;
                                //}
                                //mActivitys.add(activity);
                            }

                            @Override
                            public void onActivityStarted(Activity activity) {
                            }

                            @Override
                            public void onActivityResumed(Activity activity) {
                            }

                            @Override
                            public void onActivityPaused(Activity activity) {
                            }

                            @Override
                            public void onActivityStopped(Activity activity) {
                            }

                            @Override
                            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                            }

                            @Override
                            public void onActivityDestroyed(Activity activity) {
                                if (null == activity/* && mActivitys.isEmpty()*/) {
                                    return;
                                }
                                //if (mActivitys.contains(activity)) {
                                //    mActivitys.remove(activity);
                                //}

                                Intent intent = activity.getIntent();
                                if (intent != null && intent.getComponent() != null) {
                                    PluginManagerHelper.unBindLaunchModeStubActivity(intent.getComponent().getClassName(), activity.getClass().getName());
                                }
                            }
                        });
                    }
                }
                QRomLog.i(TAG, "Complete Init PluginFramework Take:" + (System.currentTimeMillis() - startTime) + "ms");
            }
        }

    public static Context fixBaseContextForReceiver(Context superApplicationContext) {
        if (superApplicationContext instanceof ContextWrapper) {
            return ((ContextWrapper) superApplicationContext).getBaseContext();
        } else {
            return superApplicationContext;
        }
    }

    /**
     * 根据插件中的classId加载一个插件中的class
     *
     * @param clsId
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Class loadPluginFragmentClassById(String clsId) {
        PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByFragmentId(clsId);
        if (pluginDescriptor != null) {
            // 插件可能尚未初始化，确保使用前已经初始化
            LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

            BaseDexClassLoader pluginClassLoader = plugin.pluginClassLoader;

            String clsName = pluginDescriptor.getPluginClassNameById(clsId);
            if (clsName != null) {
                try {
                    Class pluginCls = ((ClassLoader) pluginClassLoader).loadClass(clsName);
                    QRomLog.i(TAG, "loadPluginClass for clsId:" + clsId + " clsName=" + clsName + " success");
                    return pluginCls;
                } catch (ClassNotFoundException e) {
                    QRomLog.e(TAG, "loadPluginFragmentClassById:" + clsId + " ClassNotFound:" + clsName
                            + "Exception", e);
                    QRomLog.w(TAG, "没有找到：" + clsName + " 是不是被混淆了~");
                }
            }
        }

        QRomLog.e(TAG, "loadPluginClass for clsId:" + clsId + " fail");

        return null;

    }

    @SuppressWarnings("rawtypes")
    public static Class loadPluginClassByName(String clsName) {
        PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(clsName);
        return loadPluginClassByName(pluginDescriptor, clsName);
    }

    public static Class loadPluginClassByName(PluginDescriptor pluginDescriptor, String clsName) {
        if (pluginDescriptor != null) {
            // 插件可能尚未初始化，确保使用前已经初始化
            LoadedPlugin plugin = PluginLauncher.instance().startPlugin(pluginDescriptor);

            BaseDexClassLoader pluginClassLoader = plugin.pluginClassLoader;

            try {
                Class pluginCls = ((ClassLoader) pluginClassLoader).loadClass(clsName);
                QRomLog.i(TAG, "loadPluginClass Success for clsName is " + clsName);
                return pluginCls;
            } catch (ClassNotFoundException e) {
                QRomLog.e(TAG, "ClassNotFound " + clsName, e);
            } catch (java.lang.IllegalAccessError illegalAccessError) {
                illegalAccessError.printStackTrace();
                throw new IllegalAccessError("出现这个异常最大的可能是插件dex和" + "宿主dex包含了相同的class导致冲突, "
                        + "请检查插件的编译脚本，确保排除了所有公共依赖库的jar");
            }

        }

        QRomLog.e(TAG, "loadPluginClass Fail for clsName:" + clsName
                + (pluginDescriptor == null ? "pluginDescriptor = null" : "pluginDescriptor not null"));

        return null;
    }

    /**
     * 获取当前class所在插件的Context 每个插件只有1个DefaultContext, 是当前插件中所有class公用的Context
     *
     * @param cls
     * @return
     */
    public static Context getDefaultPluginContext(@SuppressWarnings("rawtypes") Class cls) {

        Context pluginContext = null;
        PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(cls.getName());

        if (pluginDescriptor != null) {
            pluginContext = PluginLauncher.instance().getRunningPlugin(pluginDescriptor.getPackageName()).pluginContext;
            ;
        } else {
            QRomLog.e(TAG, "PluginDescriptor Not Found for " + cls.getName());
        }

        if (pluginContext == null) {
            QRomLog.e(TAG, "Context Not Found for " + cls.getName());
        }

        return pluginContext;
    }

    public static boolean isInstalled(String pluginId, String pluginVersion) {
        PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);
        if (pluginDescriptor != null) {
            QRomLog.i(
                    TAG,
                    "call isInstalled pluginId=" + pluginId + " pluginDescriptor.getVersion="
                            + pluginDescriptor.getVersion() + " pluginVersion=" + pluginVersion);
            return pluginDescriptor.getVersion().equals(pluginVersion);
        }
        return false;
    }

    //Ver1.0的接口,勿删
    public static synchronized void loadPlugins(Context app) {
        if (!isLoaderPlugins) {
            long beginTime = System.currentTimeMillis();
            // step1 判断application的版本号，通过版本号来判断是否要全部更新插件内容
            int curVersionCode = 1;
            String curVersionName = "";
            try {
                final PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(),
                        PackageManager.GET_CONFIGURATIONS);
                curVersionCode = pi.versionCode;
                curVersionName = pi.versionName;
            } catch (NameNotFoundException e) {
                QRomLog.w(TAG, "loadPlugins getPackageInfo Exception:", e);
            }

            final int saveVerCode = getVersionCode();
            final String saveVerName = getVersionName();
            QRomLog.i(TAG, "call loadPlugins !isLoaderPlugins - curVersionCode：" + curVersionCode + ", oldVersionCode:" + saveVerCode +
                    " curVersionName：" + curVersionName + " oldVersionName:" + curVersionName);

            //不管是verCode 还是 verName 变更了都认为是不同的包【旧的包可能插件会存在不谦容，因此这里的版本判断的 ！=】
            if (saveVerCode != curVersionCode || !saveVerName.equals(curVersionName)) {
                QRomLog.i(TAG, "新的程序包安装,先清理...");
                // 版本升级 清理掉之前安装的所有插件
                PluginManagerHelper.removeAll();

                // step2 加载assets/plugins下面的插件
                installAssetsPlugins();

                // save Version info
                saveVersionCode(curVersionCode);
                saveVersionName(curVersionName);
            }

            QRomLog.i(TAG, "loadPlugins 耗时：" + (System.currentTimeMillis() - beginTime) + "ms");

            isLoaderPlugins = true;
        }
    }

    // HashMap<String, String> <packageName, apk>
    public static synchronized void loadPlugins(Context app, HashMap<String, String> pluginApksInfo, boolean force) {
        long beginTime = System.currentTimeMillis();

        // step1 判断application的版本号，通过版本号来判断是否要全部更新插件内容
        int curVersionCode = 1;
        String curVersionName = "";
        try {
            final PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
            curVersionCode = pi.versionCode;
            curVersionName = pi.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        final int saveVerCode = getVersionCode();
        final String saveVerName = getVersionName();
        QRomLog.i(TAG, "loadPlugins curVersionCode is " + curVersionCode + " saveVerCode=" + saveVerCode
                + " curVersionName is " + curVersionName + " saveVerName=" + saveVerName);
        if (saveVerCode != curVersionCode || !saveVerName.equals(curVersionName) || force) {
            QRomLog.i(TAG, "首次/升级安装,先清理...");// rick_Note:这个有个问题需要确定：如果新版本里面不包含之前版本的插件包该怎么处理？？？？
            if (pluginApksInfo != null && 0 < pluginApksInfo.size()) {
                // 不在install列表里面的插件一律remove掉
                Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
                Iterator<PluginDescriptor> itr = plugins.iterator();
                while (itr.hasNext()) {
                    final PluginDescriptor pluginDescriptor = itr.next();
                    if (!pluginApksInfo.containsKey(pluginDescriptor.getPackageName())) {
                        PluginManagerHelper.remove(pluginDescriptor.getPackageName());

                        //如果存在后台下了的插件，这里直接安装更新包，并将config配置的插件包名置空
                        if(!TextUtils.isEmpty(pluginDescriptor.getUpgradeFilePath()) && InstallResult.SUCCESS == PluginManagerHelper.installPlugin(pluginDescriptor.getUpgradeFilePath())) {
                            QRomLog.i(TAG, "loadPlugins:" + 01);
                            pluginApksInfo.put(pluginDescriptor.getPackageName(),"");//pluginApksInfo对应key的记录value置为空
                        }
                    }
                }

                final Hashtable<String, String> upgradePluginsInfo = PluginManagerHelper.getUpgradePluginsInfo();
                QRomLog.i(TAG, "loadPlugins::getUpgradePluginsInfo:" + upgradePluginsInfo);
                //先判断一下插件是否存在更新
                for (String pid : pluginApksInfo.keySet()) {
                    if (upgradePluginsInfo.containsKey(pid)) {
                        if (InstallResult.SUCCESS == PluginManagerHelper.installPlugin(upgradePluginsInfo.get(pid))) {
                            QRomLog.i(TAG, "loadPlugins:" + 02);
                            pluginApksInfo.put(pid, "");
                        }
                        //将更新包置空
                        PluginManagerHelper.updateUpgradePluginPackageInfo(pid, "");
                    }
                }

                // step2 加载assets/plugins下面的插件
                installPlugins(pluginApksInfo.values());
            }

            // save Version info
            saveVersionCode(curVersionCode);
            saveVersionName(curVersionName);
        } else {
            final Hashtable<String, String> upgradePluginsInfo = PluginManagerHelper.getUpgradePluginsInfo();
            QRomLog.i(TAG, "loadPlugins::getUpgradePluginsInfo:" + upgradePluginsInfo);
            //先判断一下插件是否存在更新
            for (String pid : pluginApksInfo.keySet()) {
                if (upgradePluginsInfo.containsKey(pid)){
                    PluginManagerHelper.installPlugin(upgradePluginsInfo.get(pid));

                    //将更新包置空
                    PluginManagerHelper.updateUpgradePluginPackageInfo(pid, "");
                }
            }
        }

        QRomLog.i(TAG, "loadPlugins 耗时：" + (System.currentTimeMillis() - beginTime) + "ms");
    }

    //rick_tan  Ver1.0的接口，保留 - 勿删
    // 安装内置插件
    private static synchronized void installAssetsPlugins() {
        QRomLog.i(TAG, "installAssetsPlugins()");
        final AssetManager asset = getApplication().getAssets();
        String[] files = null;
        try {
            files = asset.list(ASSETS_PLUGS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files != null) {
            for (String apk : files) {
                if (!apk.endsWith(".apk")) {
                    QRomLog.e(TAG, "主意 - fill：" + apk + "不是apk，将放弃走插件安装流程...");
                    continue;
                }

                copyAndInstall(ASSETS_PLUGS_DIR + "/" + apk);
            }
        }
    }

    // 安装内置插件
    private static synchronized void installPlugins(Collection<String> pluginApks) {
        QRomLog.i(TAG, "installAssetsPlugins()");

        if (pluginApks == null) {
            return;
        }

        for (String apk : pluginApks) {
            QRomLog.i(TAG, "installPlugins apk = " + apk);
            if (!apk.endsWith(".apk")) {
                continue;
            }

            copyAndInstall(ASSETS_PLUGS_DIR + "/" + apk);
        }
    }

    public static int getVersionCode() {
        SharedPreferences sp = getSharedPreference();

        return sp == null ? 0 : sp.getInt(VERSION_CODE_KEY, 0);
    }

    public static String getVersionName() {
        SharedPreferences sp = getSharedPreference();

        return sp == null ? "" : sp.getString(VERSION_NAME_KEY, "");
    }

    private static synchronized void saveVersionCode(int verCode) {
        QRomLog.i(TAG, "saveVersionCode:" + verCode);
        getSharedPreference().edit().putInt(VERSION_CODE_KEY, verCode).commit();
    }

    private static synchronized void saveVersionName(String verName) {
        QRomLog.i(TAG, "saveVersionName:" + verName);
        getSharedPreference().edit().putString(VERSION_NAME_KEY, verName).commit();
    }

    private static SharedPreferences getSharedPreference() {
        SharedPreferences sp = getApplication().getSharedPreferences(PLUGIN_SHAREED_PREFERENCE_NAME,
                Build.VERSION.SDK_INT < 11 ? Context.MODE_PRIVATE : Context.MODE_PRIVATE | 0x0004);
        return sp;
    }

    public static String getHostPackageName() {
        if (TextUtils.isEmpty(sHostPackageName)) {
            sHostPackageName = getApplication().getPackageName();
        }

        return sHostPackageName;
    }

    public static boolean copyAndInstall(String name) {
        boolean isSuccess = false;
        InputStream assestInput = null;
        try {
            assestInput = getApplication().getAssets().open(name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == assestInput) {
            QRomLog.e(TAG, "读取Assets下面的：" + name + "失败，请确认该文件是否存在~");
            return isSuccess;
        }

        String dest = getApplication().getCacheDir().getAbsolutePath() + "/" + name;
        if (FileUtil.copyFile(assestInput, dest)) {
            isSuccess = InstallResult.SUCCESS == PluginManagerHelper.installPlugin(dest);
        } else {
            QRomLog.e(TAG, "抽取assets中的Apk失败");
        }

        return isSuccess;
    }

    public static String getPackageName(final Intent intent) {
        String packageName = intent.getPackage();
        if (TextUtils.isEmpty(packageName) && intent.getComponent() != null) {
            packageName = intent.getComponent().getPackageName();
        }

        return packageName;
    }
}
