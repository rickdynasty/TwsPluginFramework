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
    private Configuration mSaveConfiguration = null;

    public static PluginApplication getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
        // 总体原则有3点：
        // 1、插件进程和宿主进程都必须有机会执行initPluginFramework
        // 2、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前，不可和插件交互
        // 3、在插件进程和宿主进程的initPluginFramework方法都执行完毕之前启动的组件，即使在initPluginFramework都执行完毕之后，也不可和插件交互
        // 如果initPluginFramework都在进程启动时就执行，自然很轻松满足上述条件。
        if (ProcessUtil.isPluginProcess(this)) {
            QRomLog.i(TAG, "插件进程 PluginLoader.initPluginFramework");
            // 插件进程，必须在这里执行initPluginFramework
            initPluginFramework(this);
        } else if (ProcessUtil.isHostProcess(this)) {
            QRomLog.i(TAG, "宿主进程 PluginLoader.initPluginFramework");
            initPluginFramework(this);
        }
    }

    private void initPluginFramework(Application app) {
        //init Plugin Framework
        PluginLoader.initPluginFramework(app);
        // init ServiceManager
        LocalServiceManager.init();
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
    }

    protected void startNeedPowerbootPlugin() {
        //some time on android 7.1 handle post can not working And plugin application must oncreate finish when home activity is onResume
        //so if on thread has a hidden danger.
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
        QRomLog.i(TAG, "startNeedPowerbootPlugin run");
        Collection<PluginDescriptor> plugins = PluginManagerHelper.getPlugins();
        Iterator<PluginDescriptor> itr = plugins.iterator();
        while (itr.hasNext()) {
            final PluginDescriptor pluginDescriptor = itr.next();
            if (TextUtils.isEmpty(pluginDescriptor.getPackageName())) {
                QRomLog.e(TAG, "My god !!! how can have such a situatio~!");
                continue;
            }

            final ArrayList<DisplayItem> dis = pluginDescriptor.getDisplayItems();
            if (dis != null) {
                for (DisplayItem di : dis) {
                    QRomLog.i(TAG, "DisplayConfig dc.pos =(" + di.x + "," + di.y + ") ,di.type " + di.action_type);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mSaveConfiguration.diff(newConfig) != 0 && ProcessUtil.isPluginProcess(this)) {
            mSaveConfiguration.updateFrom(newConfig);
            QRomLog.i(TAG, "更新所有插件的Config配置");
            PluginLauncher.instance().onConfigurationChanged(newConfig);
        }
    }
}
