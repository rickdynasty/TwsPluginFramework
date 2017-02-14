package com.tws.plugin.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import tws.component.log.TwsLog;
import android.content.ContentResolver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;

/**
 * @author yongchen
 * 
 */
public class PluginManagerHelper {
	private static final String TAG = "rick_Print:PluginManagerHelper";

	// 加个客户端进程的缓存，减少跨进程调用
	private static final HashMap<String, PluginDescriptor> localCache = new HashMap<String, PluginDescriptor>();
	private static final HashMap<String, Drawable> resCache = new HashMap<String, Drawable>();

	public static void addPluginIcon(String resName, Drawable drawable) {
		if (TextUtils.isEmpty(resName))
			return;

		resCache.put(resName, drawable);
	}

	public static Drawable getPluginIcon(String resName) {
		if (TextUtils.isEmpty(resName))
			return null;

		return resCache.get(resName);
	}

	public static PluginDescriptor getPluginDescriptorByClassName(String clazzName) {

		PluginDescriptor pluginDescriptor = localCache.get(clazzName);

		if (pluginDescriptor == null) {
			Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_QUERY_BY_CLASS_NAME,
					clazzName, null);
			if (bundle != null) {
				pluginDescriptor = (PluginDescriptor) bundle
						.getSerializable(PluginManagerProvider.QUERY_BY_CLASS_NAME_RESULT);
				localCache.put(clazzName, pluginDescriptor);
			}
		}

		return pluginDescriptor;
	}

	@SuppressWarnings("unchecked")
	public static Collection<PluginDescriptor> getPlugins() {
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_QUERY_ALL, null, null);

		Collection<PluginDescriptor> list = null;
		if (bundle != null) {
			list = (Collection<PluginDescriptor>) bundle.getSerializable(PluginManagerProvider.QUERY_ALL_RESULT);
		}
		// 防止NPE
		if (list == null) {
			list = new ArrayList<PluginDescriptor>();
		}
		return list;
	}

	public static PluginDescriptor getPluginDescriptorByPluginId(String pluginId) {
		if (TextUtils.isEmpty(pluginId))
			return null;

		if (pluginId.startsWith("com.android.")) {
			// 之所以有这判断, 是因为可能BinderProxyDelegate
			// 或者AndroidAppIPackageManager
			// 或者PluginBaseContextWrapper.createPackageContext
			// 中拦截了由系统发起的查询操作, 被拦截之后转到了这里
			// 所有在这做个快速判断.
			return null;
		}

		PluginDescriptor pluginDescriptor = localCache.get(pluginId);

		if (pluginDescriptor == null) {
			Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_QUERY_BY_ID, pluginId,
					null);
			if (bundle != null) {
				pluginDescriptor = (PluginDescriptor) bundle.getSerializable(PluginManagerProvider.QUERY_BY_ID_RESULT);
				localCache.put(pluginId, pluginDescriptor);
			}
		} else {
			TwsLog.d(TAG, "取本端缓存:" + pluginDescriptor.getInstalledPath());
		}

		return pluginDescriptor;
	}

	public static int installPlugin(String srcFile) {
		clearLocalCache();
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_INSTALL, srcFile, null);

		int result = 7;// install-Fail
		if (bundle != null) {
			result = bundle.getInt(PluginManagerProvider.INSTALL_RESULT);
		}
		switch (result) {
		case InstallResult.SRC_FILE_NOT_FOUND:
			Toast.makeText(PluginLoader.getApplication(), "插件不存在", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.COPY_FILE_FAIL:
			Toast.makeText(PluginLoader.getApplication(), "复制插件文件失败", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.SIGNATURES_INVALIDATE:
			Toast.makeText(PluginLoader.getApplication(), "插件签名验证失败", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.VERIFY_SIGNATURES_FAIL:
			Toast.makeText(PluginLoader.getApplication(), "插件证书和宿主证书不一致", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.PARSE_MANIFEST_FAIL:
			Toast.makeText(PluginLoader.getApplication(), "解析插件Manifest文件失败", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.FAIL_BECAUSE_HAS_LOADED:
			Toast.makeText(PluginLoader.getApplication(), "旧版插件已经加载， 且新版插件和旧版插件版本相同，拒绝安装", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.INSTALL_FAIL:
			Toast.makeText(PluginLoader.getApplication(), "安装插件失败", Toast.LENGTH_LONG).show();
			break;
		case InstallResult.MIN_API_NOT_SUPPORTED:
			Toast.makeText(PluginLoader.getApplication(), "当前系统版本过低, 不支持此插件", Toast.LENGTH_LONG).show();
			break;
		default:// InstallResult.SUCCESS:
			// Toast.makeText(PluginLoader.getApplication(), "安装成功",
			// Toast.LENGTH_SHORT).show();
			break;
		}

		return result;
	}

	public static synchronized void remove(String pluginId) {
		clearLocalCache();
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_REMOVE, pluginId, null);
		boolean success = bundle == null ? false : bundle.getBoolean(PluginManagerProvider.REMOVE_RESULT);
		TwsLog.d(TAG, "卸载：" + pluginId + (success ? "成功" : "失败"));
	}

	/**
	 * 清除列表并不能清除已经加载到内存当中的class,因为class一旦加载后后无法卸载
	 */
	public static synchronized void removeAll() {
		clearLocalCache();
		/* Bundle bundle = */call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_REMOVE_ALL, null, null);
		// boolean success = bundle == null ? false :
		// bundle.getBoolean(PluginManagerProvider.REMOVE_RESULT);
		// Toast.makeText(PluginLoader.getApplication(), success ? "卸载成功" :
		// "卸载失败", Toast.LENGTH_LONG).show();
	}

	public static void clearLocalCache() {
		localCache.clear();
	}

	public static PluginDescriptor getPluginDescriptorByFragmentId(String clazzId) {

		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_QUERY_BY_FRAGMENT_ID,
				clazzId, null);
		if (bundle != null) {
			return (PluginDescriptor) bundle.getSerializable(PluginManagerProvider.QUERY_BY_FRAGMENT_ID_RESULT);
		}
		return null;
	}

	public static String bindStubReceiver() {
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_BIND_RECEIVER, null, null);
		if (bundle != null) {
			return bundle.getString(PluginManagerProvider.BIND_RECEIVER_RESULT);
		}
		return null;
	}

	public static String bindStubActivity(String pluginActivityClassName, int launchMode) {
		Bundle arg = new Bundle();
		arg.putInt("launchMode", launchMode);
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_BIND_ACTIVITY,
				pluginActivityClassName, arg);
		if (bundle != null) {
			return bundle.getString(PluginManagerProvider.BIND_ACTIVITY_RESULT);
		}
		return null;
	}

	public static boolean isExact(String name, int type) {
		Bundle arg = new Bundle();
		arg.putInt("type", type);
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_IS_EXACT, name, arg);
		if (bundle != null) {
			return bundle.getBoolean(PluginManagerProvider.IS_EXACT_RESULT);
		}
		return false;
	}

	public static void unBindLaunchModeStubActivity(String activityName, String className) {
		Bundle arg = new Bundle();
		arg.putString("className", className);
		call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_UNBIND_ACTIVITY, activityName, arg);
	}

	public static String getBindedPluginServiceName(String stubServiceName) {
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_GET_BINDED_SERVICE,
				stubServiceName, null);
		if (bundle != null) {
			return bundle.getString(PluginManagerProvider.GET_BINDED_SERVICE_RESULT);
		}
		return null;
	}

	public static String bindStubService(String pluginServiceClassName, String process) {
		Bundle extras = null;
		if(!TextUtils.isEmpty(process)){
			extras = new Bundle();
			extras.putString(PluginManagerProvider.EXTRAS_BUNDLE_PROCESS, process);
		}
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_BIND_SERVICE,
				pluginServiceClassName, extras);
		if (bundle != null) {
			return bundle.getString(PluginManagerProvider.BIND_SERVICE_RESULT);
		}
		return null;
	}

	public static void unBindStubService(String pluginServiceName) {
		call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_UNBIND_SERVICE, pluginServiceName, null);
	}

	public static boolean isStub(String className) {
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_IS_STUB, className, null);
		if (bundle != null) {
			return bundle.getBoolean(PluginManagerProvider.IS_STUB_RESULT);
		}
		return false;
	}

	public static String dumpServiceInfo() {
		Bundle bundle = call(PluginManagerProvider.buildUri(), PluginManagerProvider.ACTION_DUMP_SERVICE_INFO, null,
				null);
		if (bundle != null) {
			return bundle.getString(PluginManagerProvider.DUMP_SERVICE_INFO_RESULT);
		}
		return null;
	}

	public static Bundle call(Uri uri, String method, String arg, Bundle extras) {
		ContentResolver resolver = PluginLoader.getApplication().getContentResolver();

		try {
			return resolver.call(uri, method, arg, extras);
		} catch (Exception e) {
			TwsLog.e(TAG, "call uri fail - uri=" + uri + " method=" + method + " arg=" + arg + " extras=" + extras, e);
		}
		return null;
	}
}
