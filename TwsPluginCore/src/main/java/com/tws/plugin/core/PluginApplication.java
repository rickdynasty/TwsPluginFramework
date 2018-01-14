package com.tws.plugin.core;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Environment;
import android.text.TextUtils;

import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;

import qrom.component.log.QRomLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class PluginApplication extends Application {

    private static final String TAG = "rick_Print:PluginApplication";
    private static PluginApplication instance;

    //黑名单插件，这个用于版本升级后对旧版本的插件过滤，一般是后台下发
    private ArrayList<String> mEliminatePlugins = new ArrayList<String>(3);
    public static String EXCLUDE_PLUGIN_FILE = "/plugins/exclude_plugin.ini";

    private Configuration mSaveConfiguration = null;

    public static PluginApplication getInstance() {
        return instance;
    }

    public ArrayList<String> getEliminatePlugins() {
        return mEliminatePlugins;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
        // 这个地方之所以这样写，是因为如果是插件进程，initPluginFramework必须在applicaiotn启动时执行
        // 而如果是宿主进程，initPluginFramework可以在这里执行，也可以在需要时再在宿主的其他组件中执行，
        // 例如点击宿主的某个Activity中的button后再执行这个方法来启动插件框架。

        // 总体原则有3点：
        // 1、插件进程和宿主进程都必须有机会执行initPluginFramework
        // 2、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前，不可和插件交互
        // 3、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前启动的组件，即使在initPluginFramework都执行完毕之后，也不可和插件交互

        // 如果initPluginFramework都在进程启动时就执行，自然很轻松满足上述条件。
        if (ProcessUtil.isPluginProcess(this)) {
            QRomLog.d(TAG, "插件进程 PluginLoader.initPluginFramework");
            // 插件进程，必须在这里执行initPluginFramework
            PluginLoader.initPluginFramework(this);
            // init ServiceManager
            LocalServiceManager.init();
        } else if (ProcessUtil.isHostProcess(this)) {
            // 宿主进程，可以在这里执行，也可以选择在宿主的其他地方在需要时再启动插件框架
            QRomLog.d(TAG, "宿主进程 PluginLoader.initPluginFramework");
            PluginLoader.initPluginFramework(this);
            // init ServiceManager
            LocalServiceManager.init();
        }
    }

    /**
     * 重写这个方法是为了支持Receiver,否则会出现ClassCast错误
     */
    @Override
    public Context getBaseContext() {
        return PluginLoader.fixBaseContextForReceiver(super.getBaseContext());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册日志广播 全局生命周期 高优先级 需要放在application中
        if (mSaveConfiguration == null) {
            mSaveConfiguration = new Configuration(getResources().getConfiguration());
        }

        QRomLog.registerLogReceiver(this);
        initEliminatePlugins();
    }

    protected void startNeedPowerbootPlugin() {
        //some time on android 7.1 handle post can not working And plugin application must oncreate finish when home activity is onResume
        //so if on thread has a hidden danger.
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
        QRomLog.d(TAG, "startNeedPowerbootPlugin run");
        Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
        Iterator<PluginDescriptor> itr = plugins.iterator();
        while (itr.hasNext()) {
            final PluginDescriptor pluginDescriptor = itr.next();
            if (TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
                QRomLog.e(TAG, "My god !!! how can have such a situatio~!");
                continue;
            }

            if (mEliminatePlugins.contains(pluginDescriptor.getPackageName())) {
                QRomLog.w(TAG, "当前插件" + pluginDescriptor.getPackageName() + "已经被列入黑名单了");
                continue;
            }

            final ArrayList<DisplayItem> dis = pluginDescriptor.getDisplayItems();
            if (dis != null) {
                for (DisplayItem di : dis) {
                    QRomLog.d(TAG, "DisplayConfig dc.pos =(" + di.x + "," + di.y + ") ,di.type " + di.action_type);
                    switch (di.action_type) {
                        case DisplayItem.TYPE_APPLICATION:
                            if (null == PluginLauncher.instance().startPlugin(di.action_id)) {
                                QRomLog.e(TAG, "startPlugin:" + di.action_id + "失败!!!");
                            }
                            break;
                        case DisplayItem.TYPE_SERVICE:
                            Intent intent = new Intent();
                            intent.setClassName(getApplicationContext(), di.action_id);
                            startService(intent);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
//			}
//		}).start();
    }

    private void initEliminatePlugins() {
        String configFile = Environment.getExternalStorageDirectory().getPath() + EXCLUDE_PLUGIN_FILE;
        try {
            File file = new File(configFile);
            if (!file.exists()) {
                return;
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line) || line.startsWith("#"))
                    continue;

                if (mEliminatePlugins == null) {
                    mEliminatePlugins = new ArrayList<String>();
                }

                //插件都是apk包，因此在这里需要带上后缀
                if (!line.endsWith(".apk")) {
                    line += ".apk";
                }

                mEliminatePlugins.add(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mEliminatePlugins != null) {
            QRomLog.w(TAG + "getExceList()", "U config Eliminate the following plug-ins:");
            for (String pStr : mEliminatePlugins) {
                QRomLog.d(TAG, " " + pStr);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mSaveConfiguration.diff(newConfig) != 0 && ProcessUtil.isPluginProcess(this)) {
            mSaveConfiguration.updateFrom(newConfig);
            QRomLog.d(TAG, "更新所有插件的Config配置");
            PluginLauncher.instance().onConfigurationChanged(newConfig);
        }
    }
}
