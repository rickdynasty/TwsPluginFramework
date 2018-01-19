package com.tws.plugin.core;

import android.text.TextUtils;

import com.tws.plugin.bridge.TwsPluginBridgeReceiver;
import com.tws.plugin.bridge.TwsPluginBridgeService;
import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.manager.PluginManagerHelper;

import java.util.HashMap;

import dalvik.system.PathClassLoader;
import qrom.component.log.QRomLog;

/**
 * 为了支持Receiver和Service，增加此类。
 *
 * @author yongchen
 */
public class HostClassLoader extends PathClassLoader {

    private static final String TAG = "rick_Print:HostClassLoader";
    private static HashMap<String, String> sPluginInHostAMF_ServiceMap = new HashMap<String, String>();

    static {
        sPluginInHostAMF_ServiceMap.clear();
//		sPluginInHostAMF_ServiceMap.put("com.pacewear.tws.demo.service.PaceService", "com.example.plugindemo");
        sPluginInHostAMF_ServiceMap.put("com.pacewear.tws.wallet.service.PaceApduService", "com.pacewear.tws.phoneside.wallet");
    }

    public HostClassLoader(String dexPath, String libraryPath, ClassLoader parent) {
        super(dexPath, libraryPath, parent);
    }

    @Override
    public String findLibrary(String name) {
        QRomLog.i(TAG, "findLibrary:" + name);
        return super.findLibrary(name);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {

        // Just for Receiver and Service
        QRomLog.i(TAG, "loadClass className:" + className + " resolve=" + resolve);

        if (className.startsWith(PluginIntentResolver.CLASS_PREFIX_SERVICE)) {

            QRomLog.i(TAG, "className is " + className + " PluginShadowService is " + PluginShadowService.class.getName());

            // 这里返回PluginShadowService是因为service的构造函数以及onCreate函数
            // 2个函数在ActivityThread的同一个函数中被调用,框架没机会在构造器执行之后,oncreate执行之前,
            // 插入一段代码, 注入context.
            // 因此这里返回一个fake的service, 在fake service的oncreate方法里面手动调用构造器和oncreate
            // 这里返回了这个Service以后,
            // 由于在框架中hook了ActivityManager的serviceDoneExecuting方法,
            // 在serviceDoneExecuting这个方法里面, 会将这个service再还原成插件的servcie对象
            if (className.equals(PluginIntentResolver.CLASS_PREFIX_SERVICE + "null")) {
                QRomLog.e(TAG, "到了这里说明出bug了,这里做个容错处理, 避免出现classnotfound");
                return TwsPluginBridgeService.class;
            }

            return PluginShadowService.class;

        } else if (className.startsWith(PluginIntentResolver.CLASS_PREFIX_RECEIVER)) {

            String realName = className.replace(PluginIntentResolver.CLASS_PREFIX_RECEIVER, "");

            Class<?> clazz = PluginLoader.loadPluginClassByName(realName);
            if (clazz != null) {
                QRomLog.i(TAG, "className is " + className + " target is " + realName + (clazz == null ? " null" : " found"));
                return clazz;
            } else {
                QRomLog.e(TAG, "到了这里说明出bug了,这里做个容错处理, 避免出现classnotfound");
                return TwsPluginBridgeReceiver.class;
            }
        } else {
            final String pluginId = sPluginInHostAMF_ServiceMap.get(className);
            if (!TextUtils.isEmpty(pluginId)) {
                PluginDescriptor pluginDescriptor = PluginManagerHelper.getPluginDescriptorByPluginId(pluginId);
                if (pluginDescriptor != null) {
                    Class<?> clazz = PluginLoader.loadPluginClassByName(pluginDescriptor, className);
                    if (clazz != null) {
                        QRomLog.i(TAG, "className is " + className + " target is in plugin:" + className + (clazz == null ? " null" : " found"));
                        return clazz;
                    }
                }

                QRomLog.e(TAG, "到了这里说明出bug了,这里做个容错处理, 避免出现classnotfound");
                return TwsPluginBridgeService.class;
            }
        }

        return super.loadClass(className, resolve);
    }
}
