package com.tws.plugin.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import qrom.component.log.QRomLog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.Instrumentation;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.text.TextUtils;

import com.tws.plugin.bridge.TwsPluginBridgeActivity;
import com.tws.plugin.content.ComponentInfo;
import com.tws.plugin.content.DisplayItem;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.android.HackContextImpl;
import com.tws.plugin.core.android.HackInstrumentation;
import com.tws.plugin.core.annotation.PluginContainer;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.core.viewfactory.PluginViewFactory;
import com.tws.plugin.manager.PluginActivityMonitor;
import com.tws.plugin.manager.PluginManagerHelper;
import com.tws.plugin.util.ProcessUtil;

/**
 * 插件Activity免注册的主要实现原理。 如有必要，可以增加被代理的方法数量。
 *
 * @author yongchen
 */
public class PluginInstrumentionWrapper extends Instrumentation {

    private static final String RELAUNCH_FLAG = "relaunch.category.";

    private static final String TAG = "PluginInstrumentionWrapper";

    private final HackInstrumentation hackInstrumentation;
    private PluginActivityMonitor monitor;

    public PluginInstrumentionWrapper(Instrumentation instrumentation) {
        this.hackInstrumentation = new HackInstrumentation(instrumentation);
        this.monitor = new PluginActivityMonitor();
    }

    /**
     * @param app
     */
    @Override
    public void callApplicationOnCreate(Application app) {
        // 此方法在application的attach之后被ActivityThread调用
        super.callApplicationOnCreate(app);

		//ContentProvider的相关操作应该放在installContentProvider之后执行,
		//而installContentProvider是ActivityThread在调用application的attach之后,onCreate之前执行
		// 因此下面的初始化操作的最佳时机是在application的oncreate之前执行
		LocalServiceManager.init();
		if (ProcessUtil.isPluginProcess()) {
			Iterator<PluginDescriptor> itr = PluginManagerHelper.getPlugins().iterator();
			while (itr.hasNext()) {
				PluginDescriptor plugin = itr.next();
				LocalServiceManager.registerService(plugin);
			}
		}
	}

    @Override
    public boolean onException(Object obj, Throwable e) {
        if (obj instanceof Activity) {
            ((Activity) obj).finish();
        } else if (obj instanceof Service) {
            ((Service) obj).stopSelf();
        }
        QRomLog.e(TAG, "记录错误日志", e);
        return super.onException(obj, e);
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        if (ProcessUtil.isPluginProcess()) {
            PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);
            if (pluginDescriptor != null) {
                return PluginLauncher.instance().getRunningPlugin(pluginDescriptor.getPackageName()).pluginApplication;
            }
        }
        return super.newApplication(cl, className, context);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {

        ClassLoader orignalCl = cl;
        String orginalClassName = className;
        String orignalIntent = intent.toString();

        if (ProcessUtil.isHostProcess() && TwsPluginBridgeActivity.class.getName().equals(className)) {
            // 第三方应用启动了TwsPluginBridgeActivity
            String packageName = PluginLoader.getPackageName(intent);
            ArrayList<ComponentInfo> componentInfos = PluginIntentResolver.matchPlugin(intent,
                    DisplayItem.TYPE_ACTIVITY, packageName);
            String pluginClassName = null;
            if (componentInfos != null && componentInfos.size() > 0) {
                pluginClassName = componentInfos.get(0).name;
            }

            if (null == pluginClassName) {
                pluginClassName = intent.getStringExtra(PluginIntentResolver.INTENT_EXTRA_BRIDGE_TO_PLUGIN);
            }

            if (pluginClassName != null) {
                PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(pluginClassName);

                if (pluginDescriptor != null) {
                    packageName = pluginDescriptor.getPackageName();
                    Class<?> cls = PluginLoader.loadPluginClassByName(pluginDescriptor, pluginClassName);
                    if (cls != null) {
                        intent.putExtra(PluginIntentResolver.INTENT_EXTRA_BRIDGE_RAMP, className);

                        className = pluginClassName;
                        cl = cls.getClassLoader();

                        intent.setExtrasClassLoader(cl);
                        intent.setAction(null);
                        // 添加一个标记符
                        intent.addCategory(RELAUNCH_FLAG + className);
                    } else {
                        throw new ClassNotFoundException("pluginClassName : " + pluginClassName, new Throwable());
                    }
                }
            }
        } else if (ProcessUtil.isPluginProcess()) {
            // 将PluginStubActivity替换成插件中的activity
            if (PluginManagerHelper.isStub(className)) {

                String action = intent.getAction();

                QRomLog.i(TAG, "newActivity action=" + action + " className=" + className);
                if (action != null && action.contains(PluginIntentResolver.CLASS_SEPARATOR)) {
                    String[] targetClassName = action.split(PluginIntentResolver.CLASS_SEPARATOR);
                    String pluginClassName = targetClassName[0];

                    final String pid = targetClassName.length > 2 ? targetClassName[2] : "";
                    PluginDescriptor pluginDescriptor = null;
                    if (!TextUtils.isEmpty(pid)) {
                        pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pid);
                    }

                    if (null == pluginDescriptor) {
                        pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(pluginClassName);
                    }

                    Class<?> cls = PluginLoader.loadPluginClassByName(pluginDescriptor, pluginClassName);
                    if (cls != null) {
                        className = pluginClassName;
                        cl = cls.getClassLoader();

                        intent.setExtrasClassLoader(cl);
                        if (targetClassName.length > 1) {
                            // 之前为了传递classNae，intent的action被修改过
                            // 这里再把Action还原到原始的Action
                            intent.setAction(targetClassName[1]);
                        } else {
                            intent.setAction(null);
                        }
                        // 添加一个标记符
                        intent.addCategory(RELAUNCH_FLAG + className);
                    } else {
                        throw new ClassNotFoundException("pluginClassName : " + pluginClassName, new Throwable());
                    }
                } else if (PluginManagerHelper.isExact(className, DisplayItem.TYPE_ACTIVITY)) {

                    // 这个逻辑是为了支持外部app唤起配置了stub_exact的插件Activity
                    PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByClassName(className);

                    Class<?> cls = PluginLoader.loadPluginClassByName(pluginDescriptor, className);
                    if (cls != null) {
                        cl = cls.getClassLoader();
                    } else {
                        throw new ClassNotFoundException("className : " + className, new Throwable());
                    }
                } else {
                    // 进入这个分支可能是因为activity重启了，比如横竖屏切换，由于上面的分支已经把Action还原到原始到Action了
                    // 这里只能通过之前添加的标记符来查找className
                    boolean found = false;
                    Set<String> category = intent.getCategories();
                    if (category != null) {
                        Iterator<String> itr = category.iterator();
                        while (itr.hasNext()) {
                            String cate = itr.next();

                            if (cate.startsWith(RELAUNCH_FLAG)) {
                                className = cate.replace(RELAUNCH_FLAG, "");

                                PluginDescriptor pluginDescriptor = PluginManagerHelper
                                        .getPluginDescriptorByClassName(className);

                                Class<?> cls = PluginLoader.loadPluginClassByName(pluginDescriptor, className);
                                cl = cls.getClassLoader();
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        throw new ClassNotFoundException(
                                "className : " + className + ", intent : " + intent.toString(), new Throwable());
                    }
                }
            } else {
                // 到这里有2中种情况
                // 1、确实是宿主Activity
                // 2、是插件Activity，但是上面的if没有识别出来（这种情况目前只发现在ActivityGroup情况下会出现，因为ActivityGroup不会触发resolveActivity方法，导致Intent没有更换）
                // 判断上述两种情况可以通过ClassLoader的类型来判断, 判断出来以后补一个resolveActivity方法
                if (cl instanceof PluginClassLoader) {
                    PluginIntentResolver.resolveActivity(intent);
                } else {
                    // Do Nothing
                }
            }
        }

        try {
            Activity activity = super.newActivity(cl, className, intent);
            if (activity instanceof PluginContainer) {
                ((PluginContainer) activity).setPluginId(intent.getStringExtra(PluginContainer.FRAGMENT_PLUGIN_ID));
            }

            return activity;
        } catch (ClassNotFoundException e) {
            // 收集状态，便于异常分析
            throw new ClassNotFoundException("  orignalCl : " + orignalCl.toString() + ", orginalClassName : "
                    + orginalClassName + ", orignalIntent : " + orignalIntent + ", currentCl : " + cl.toString()
                    + ", currentClassName : " + className + ", currentIntent : " + intent.toString() + ", process : "
                    + ProcessUtil.isPluginProcess() + ", isStubActivity : "
                    + PluginManagerHelper.isStub(orginalClassName) + ", isExact : "
                    + PluginManagerHelper.isExact(orginalClassName, DisplayItem.TYPE_ACTIVITY), e);
        }
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {

        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        PluginInjector.injectActivityContext(activity);

        Intent intent = activity.getIntent();

        if (intent != null) {
            intent.setExtrasClassLoader(activity.getClassLoader());
        }

        if (icicle != null) {
            icicle.setClassLoader(activity.getClassLoader());
        }

        if (ProcessUtil.isPluginProcess()) {
            installPluginViewFactory(activity);

            if (activity.isChild()) {
                // 修正TabActivity中的Activity的ContextImpl的packageName
                Context base = activity.getBaseContext();
                while (base instanceof ContextWrapper) {
                    base = ((ContextWrapper) base).getBaseContext();
                }
                if (HackContextImpl.instanceOf(base)) {
                    HackContextImpl impl = new HackContextImpl(base);
                    String packageName = PluginLoader.getApplication().getPackageName();
                    // String packageName1 = activity.getPackageName();
                    impl.setBasePackageName(packageName);
                    impl.setOpPackageName(packageName);
                }
            }
        }

        super.callActivityOnCreate(activity, icicle);

        monitor.onActivityCreate(activity);

    }

    private void installPluginViewFactory(Activity activity) {
        String pluginId = null;
        if (activity instanceof PluginContainer) {
            pluginId = ((PluginContainer) activity).getPluginId();
        }
        // 如果配置了插件容器注解而且指定了插件Id, 框架会自动更换activity的context,无需安装PluginViewFactory
        if (TextUtils.isEmpty(pluginId)) {
            new PluginViewFactory(activity, activity.getWindow(), new PluginViewCreator()).installViewFactory();
        }
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        monitor.onActivityDestory(activity);

        super.callActivityOnDestroy(activity);
    }

    @Override
    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(activity.getClassLoader());
        }

        super.callActivityOnRestoreInstanceState(activity, savedInstanceState);
    }

    @Override
    public void callActivityOnPostCreate(Activity activity, Bundle icicle) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        if (icicle != null) {
            icicle.setClassLoader(activity.getClassLoader());
        }

        super.callActivityOnPostCreate(activity, icicle);
    }

    @Override
    public void callActivityOnNewIntent(Activity activity, Intent intent) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        if (intent != null) {
            intent.setExtrasClassLoader(activity.getClassLoader());
        }

        super.callActivityOnNewIntent(activity, intent);
    }

    @Override
    public void callActivityOnStart(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnStart(activity);
    }

    @Override
    public void callActivityOnRestart(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnRestart(activity);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnResume(activity);
        monitor.onActivityResume(activity);
    }

    @Override
    public void callActivityOnStop(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnStop(activity);
    }

    @Override
    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);

        if (outState != null) {
            outState.setClassLoader(activity.getClassLoader());
        }

        super.callActivityOnSaveInstanceState(activity, outState);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnPause(activity);
        monitor.onActivityPause(activity);
    }

    @Override
    public void callActivityOnUserLeaving(Activity activity) {
        PluginInjector.injectInstrumetionFor360Safe(activity, this);
        super.callActivityOnUserLeaving(activity);
    }

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                            Intent intent, int requestCode, Bundle options) {

        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivity(who, contextThread, token, target, intent, requestCode, options);
    }

    public void execStartActivities(Context who, IBinder contextThread, IBinder token, Activity target,
                                    Intent[] intents, Bundle options) {

        PluginIntentResolver.resolveActivity(intents);

        hackInstrumentation.execStartActivities(who, contextThread, token, target, intents, options);
    }

    public void execStartActivitiesAsUser(Context who, IBinder contextThread, IBinder token, Activity target,
                                          Intent[] intents, Bundle options, int userId) {

        PluginIntentResolver.resolveActivity(intents);

        hackInstrumentation.execStartActivitiesAsUser(who, contextThread, token, target, intents, options, userId);
    }

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target,
                                            Intent intent, int requestCode, Bundle options) {

        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivity(who, contextThread, token, target, intent, requestCode, options);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                            Intent intent, int requestCode, Bundle options, UserHandle user) {

        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivity(who, contextThread, token, target, intent, requestCode, options,
                user);
    }

    // /////////// Android 4.0.4及以下 ///////////////

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                            Intent intent, int requestCode) {

        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivity(who, contextThread, token, target, intent, requestCode);
    }

    public void execStartActivities(Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents) {
        PluginIntentResolver.resolveActivity(intents);

        hackInstrumentation.execStartActivities(who, contextThread, token, target, intents);
    }

    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target,
                                            Intent intent, int requestCode) {

        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivity(who, contextThread, token, target, intent, requestCode);
    }

    // ///// For Android 5.1
    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread, IBinder token, Activity target,
                                                    Intent intent, int requestCode, Bundle options, int userId) {
        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivityAsCaller(who, contextThread, token, target, intent, requestCode,
                options, userId);
    }

    public void execStartActivityFromAppTask(Context who, IBinder contextThread, Object appTask, Intent intent,
                                             Bundle options) {

        PluginIntentResolver.resolveActivity(intent);

        hackInstrumentation.execStartActivityFromAppTask(who, contextThread, appTask, intent, options);
    }

    // for Android 7.0
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target,
                                            Intent intent, int requestCode, Bundle options) {

        PluginIntentResolver.resolveActivity(intent);
        if (who instanceof Activity)
            return hackInstrumentation.execStartActivity(who, contextThread, token, (Activity) who, intent,
                    requestCode, options);
        else {
            QRomLog.e(TAG, "execStartActivity who:" + who + " intent:" + intent + " String target:" + target);
            return null;
        }
    }

    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread, IBinder token, Activity target,
                                                    Intent intent, int requestCode, Bundle options, boolean ignoreTargetSecurity, int userId) {
        PluginIntentResolver.resolveActivity(intent);

        return hackInstrumentation.execStartActivityAsCaller(who, contextThread, token, target, intent, requestCode,
                options, userId);
    }
}
