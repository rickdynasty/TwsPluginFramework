package com.tws.plugin.util;

import java.util.List;

import tws.component.log.TwsLog;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;

import com.tws.plugin.core.PluginApplication;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.manager.PluginManagerProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ProcessUtil {

	private static final String TAG = "rick_Print:ProcessUtil";

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
			String processName = getCurProcessName();
			String pluginProcessName = getPluginProcessName(context);

			isHostProcess = processName.equals(pluginProcessName);
			// 这是一个潜规则，插件的进程除PluginManagerProvider的标配外，其他的都统一规定前缀："HostPackageName:plugin"+"编号";
			isPluginProcess = isHostProcess
					|| processName.startsWith(PluginApplication.getInstance().getPackageName() + ":plugin"); // 注意这里不能用PluginLoader的Application
		}
	}

	public static boolean isPluginProcess() {
		return isPluginProcess(PluginLoader.getApplication());
	}

	public static boolean isHostProcess() {
		return isHostProcess(PluginLoader.getApplication());
	}

	private static String getCurProcessName() {
	       BufferedReader mBufferedReader=null;
	       final int pid = android.os.Process.myPid();
	       TwsLog.d(TAG, "getCurProcessName pid=" + pid);
               try {
                       File file = new File("proc/" + pid + "/" + "cmdline");
                       mBufferedReader = new BufferedReader(new FileReader(file));
                       String processName = mBufferedReader.readLine().trim();
                       mBufferedReader.close();
                       return processName;
               } catch (Exception e) {
                       e.printStackTrace();

               } finally {
                       if (mBufferedReader != null) {
                               try {
                                       mBufferedReader.close();
                               } catch (IOException e) {
                                       e.printStackTrace();
                               }
                       }
               }
               return "";
        }
	
        public static String getPluginProcessName(Context context) {
		try {
			// 这里取个巧, 直接查询ContentProvider的信息中包含的processName
			// 因为Contentprovider是被配置在插件进程的.
			// 但是这个api只支持9及以上,
			ProviderInfo pinfo = context.getPackageManager().getProviderInfo(
					new ComponentName(context, PluginManagerProvider.class), 0);
			return pinfo.processName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}
}
