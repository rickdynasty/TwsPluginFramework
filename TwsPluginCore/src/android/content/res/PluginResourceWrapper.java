package android.content.res;

import java.lang.reflect.Field;
import java.util.HashSet;

import tws.component.log.TwsLog;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.util.ResourceUtil;

/**
 * 根据不同的rom，可能需要重写更多的方法，目前发现的几个机型的问题暂时只需要重写下面2个方法。
 * 
 * @author yongchen
 * 
 */
public class PluginResourceWrapper extends Resources {

	private static final String TAG = "rick_Print:PluginResourceWrapper";

	private HashSet<Integer> idCaches = new HashSet<Integer>(5);

	private PluginDescriptor mPluginDescriptor;

	public PluginResourceWrapper(AssetManager assets, DisplayMetrics metrics, Configuration config,
			PluginDescriptor pluginDescriptor) {
		super(assets, metrics, config);
		this.mPluginDescriptor = pluginDescriptor;
	}

	@Override
	public String getResourcePackageName(int resid) throws NotFoundException {
		if (idCaches.contains(resid)) {
			return PluginLoader.getApplication().getPackageName();
		}
		try {
			return super.getResourcePackageName(resid);
		} catch (NotFoundException e) {
			TwsLog.e(TAG, "NotFoundException Try Following resid=" + Integer.toHexString(resid));

			// 就目前测试的情况来看，只有Coolpad、vivo、oppo等手机会在上面抛异常，走到这里来，
			// 华为、三星、小米等手机不会到这里来。
			if (ResourceUtil.isMainResId(resid)) {
				idCaches.add(resid);
				return PluginLoader.getApplication().getPackageName();
			}
			TwsLog.e(TAG, "NotFoundException Try Following resid=" + Integer.toHexString(resid), e);
			throw new NotFoundException("Unable to find resource ID #0x" + Integer.toHexString(resid));
		}
	}

	@Override
	public String getResourceName(int resid) throws NotFoundException {
		try {
			return super.getResourceName(resid);
		} catch (NotFoundException e) {
			TwsLog.e(TAG, "NotFoundException Try Following");

			// 就目前测试的情况来看，只有Coolpad、vivo、oppo等手机会在上面抛异常，走到这里来，
			// 华为、三星、小米等手机不会到这里来。
			if (ResourceUtil.isMainResId(resid)) {
				return PluginLoader.getApplication().getResources().getResourceName(resid);
			}
			throw new NotFoundException("Unable to find resource ID #0x" + Integer.toHexString(resid));
		}
	}

	@Override
	public String getResourceEntryName(int resid) {
		try {
			return super.getResourceEntryName(resid);
		} catch (NotFoundException e) {
			// vivo
			if (ResourceUtil.isMainResId(resid)) {
				return PluginLoader.getApplication().getResources().getResourceEntryName(resid);
			}
			TwsLog.e(TAG, "NotFoundException Try Following");
			throw new NotFoundException("Unable to find resource ID #0x" + Integer.toHexString(resid));
		}
	}

	@Override
	public String getResourceTypeName(int resid) {
		try {
			return super.getResourceTypeName(resid);
		} catch (NotFoundException e) {
			// vivo
			if (ResourceUtil.isMainResId(resid)) {
				return PluginLoader.getApplication().getResources().getResourceTypeName(resid);
			}
			TwsLog.e(TAG, "NotFoundException Try Following");
			throw new NotFoundException("Unable to find resource ID #0x" + Integer.toHexString(resid));
		}
	}

	/**
	 * 重写这个方法主要是为了解决非独立插件可以反查宿主资源id的问题
	 */
	@Override
	public int getIdentifier(String name, String defType, String defPackage) {

		if (TextUtils.isDigitsOnly(name)) {
			return super.getIdentifier(name, defType, defPackage);
		}

		// 传了packageName，而且不是宿主的packageName， 则直接返回
		if (!TextUtils.isEmpty(defPackage) && !PluginLoader.getApplication().getPackageName().equals(defPackage)) {
			return super.getIdentifier(name, defType, defPackage);
		}

		// package:type/entry
		// 第一段 “package:“ 第二段 ”type/“ 第三段 “entry”
		String packageName = null;
		String type = null;
		String entry = null;

		String[] pte = name.split(":");
		String[] te;
		if (pte.length == 2) {
			packageName = pte[0];
			te = pte[1].split("/");
		} else {
			te = pte[0].split("/");
		}

		if (te.length == 2) {
			type = te[0];
			entry = te[1];
		} else {
			entry = te[0];
		}

		if (packageName == null) {
			packageName = defPackage;
		}

		if (type == null) {
			type = defType;
		}

		// 传了宿主的packageName
		if (PluginLoader.getApplication().getPackageName().equals(packageName)) {
			if (mPluginDescriptor.isStandalone()) {
				// 如果是独立插件, 取不到宿主资源, 这里强制切换到插件
				packageName = mPluginDescriptor.getPackageName();
			} else {
				// 判断是否在真的在宿主中
				Class rClass = null;
				try {
					String className = packageName + ".R$" + type;
					rClass = this.getClass().getClassLoader().loadClass(className);
					Field field = rClass.getDeclaredField(entry);
					if (field == null) {
						// 不在宿主中，切换到插件
						packageName = mPluginDescriptor.getPackageName();
					} else {
						// 在宿主中, 通过宿主的Context获取
						return PluginLoader.getApplication().getResources().getIdentifier(entry, type, packageName);
					}
				} catch (Exception e) {
					// 不在宿主中，切换到插件
					packageName = mPluginDescriptor.getPackageName();
				}
			}
		}
		return super.getIdentifier(entry, type, packageName);
	}

	@Override
	@Deprecated
	public Drawable getDrawable(int id) throws NotFoundException {
		try {
			return super.getDrawable(id);
		} catch (Exception e) {
			TwsLog.e(TAG, "NotFoundException Try Following");

			// 7.1 Nexus 6p会跑到这里来
			if (ResourceUtil.isMainResId(id)) {
				return PluginLoader.getApplication().getResources().getDrawable(id);
			}
			throw new NotFoundException("Unable to find Drawable resource ID #0x" + Integer.toHexString(id));
		}
	}

	@Override
	public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
		try {
			return super.getDrawable(id, theme);
		} catch (Exception e) {
			TwsLog.e(TAG, "NotFoundException Try Following");

			// 7.1 Nexus 6p会跑到这里来
			if (ResourceUtil.isMainResId(id)) {
				return PluginLoader.getApplication().getResources().getDrawable(id, theme);
			}
			throw new NotFoundException("Unable to find Drawable resource ID #0x" + Integer.toHexString(id));
		}
	}

	@Override
	public int getColor(int id) throws NotFoundException {
		try {
			return super.getColor(id);
		} catch (Exception e) {
			TwsLog.e(TAG, "NotFoundException Try Following");

			// 7.1 Nexus 6p会跑到这里来
			if (ResourceUtil.isMainResId(id)) {
				return PluginLoader.getApplication().getResources().getColor(id);
			}
			throw new NotFoundException("Unable to find Color resource ID #0x" + Integer.toHexString(id));
		}
	}

	@Override
	public ColorStateList getColorStateList(int id) throws NotFoundException {
		try {
			return super.getColorStateList(id);
		} catch (Exception e) {
			TwsLog.e(TAG, "NotFoundException Try Following");

			// 7.1 Nexus 6p会跑到这里来
			if (ResourceUtil.isMainResId(id)) {
				return PluginLoader.getApplication().getResources().getColorStateList(id);
			}
			throw new NotFoundException("Unable to find Color resource ID #0x" + Integer.toHexString(id));
		}
	}
}
