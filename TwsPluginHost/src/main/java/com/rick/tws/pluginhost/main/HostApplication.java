package com.rick.tws.pluginhost.main;

import android.content.Context;

import com.rick.tws.framework.HostProxy;
import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.ProcessUtil;

/**
 * Created by Administrator on 2017/11/6 0006.
 */
public class HostApplication extends PluginApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        if (ProcessUtil.isPluginProcess(this)) {
            // 提前启动应用的依赖插件[DM的启动依赖登录和配对插件]
            startAppDependentPlugin();
            // 随DM启动的插件 时机调整到application的onCreate里面
            startNeedPowerbootPlugin();

            //加载内置插件
            PluginLoader.loadPlugins(this);
        }
    }

    private void startAppDependentPlugin() {
        // 宿主的启动依赖一些插件，需要提前加载好这些插件
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        HostProxy.setApplication(this);
    }
}
