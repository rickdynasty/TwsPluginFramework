package com.tws.plugin.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import qrom.component.log.QRomLog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.view.LayoutInflater;

import com.tws.plugin.content.PluginDescriptor;
import com.tws.plugin.core.android.HackContextImpl;
import com.tws.plugin.core.android.HackResources;
import com.tws.plugin.core.compat.CompatForSharedPreferencesImpl;
import com.tws.plugin.core.localservice.LocalServiceManager;
import com.tws.plugin.core.multidex.PluginMultiDexHelper;
import com.tws.plugin.util.ProcessUtil;

/**
 * 注意：意外覆写父类方法可能会抛出LingageError 也就是说如果要在这个类里添加非override的public方法的话要小心了。
 */
public class PluginContextTheme extends PluginBaseContextWrapper {
	private static final String TAG = "rick_Print_TT:PluginContextTheme";
	private int mThemeResource;
	Resources.Theme mTheme;
	private LayoutInflater mInflater;
	private ApplicationInfo mApplicationInfo;
	Resources mResources;
	private final ClassLoader mClassLoader;
	private Application mPluginApplication;
	protected final PluginDescriptor mPluginDescriptor;

	private ArrayList<BroadcastReceiver> receivers = new ArrayList<BroadcastReceiver>();

	private boolean crackPackageManager = false;

	public PluginContextTheme(PluginDescriptor pluginDescriptor, Context base, Resources resources,
			ClassLoader classLoader) {
		super(base);
		mPluginDescriptor = pluginDescriptor;
		mResources = resources;
		mClassLoader = classLoader;

		if (!ProcessUtil.isPluginProcess()) {
			throw new IllegalAccessError("本类仅在插件进程使用");
		}
	}

	public void setPluginApplication(Application pluginApplication) {
		this.mPluginApplication = pluginApplication;
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);
	}

	@Override
	public ClassLoader getClassLoader() {
		return mClassLoader;
	}

	@Override
	public AssetManager getAssets() {
		return mResources.getAssets();
	}

	@Override
	public Resources getResources() {
		return mResources;
	}

	/**
	 * 传0表示使用系统默认主题，最终的现实样式和客户端程序的minSdk应该有关系。 即系统针对不同的minSdk设置了不同的默认主题样式
	 * 传非0的话表示传过来什么主题就显示什么主题
	 */
	@Override
	public void setTheme(int resid) {
		mThemeResource = resid;
		initializeTheme();
	}

	@Override
	public Resources.Theme getTheme() {
		if (mTheme != null) {
			return mTheme;
		}

		Integer result = HackResources.selectDefaultTheme(mThemeResource,
				getBaseContext().getApplicationInfo().targetSdkVersion);
		if (result != null) {
			mThemeResource = result;
		}

		initializeTheme();

		return mTheme;
	}

	@Override
	public Object getSystemService(String name) {
		if (LAYOUT_INFLATER_SERVICE.equals(name)) {
			if (mInflater == null) {
				mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
			}
			return mInflater;
		}

		Object service = getBaseContext().getSystemService(name);

		if (service == null) {
			service = LocalServiceManager.getService(name);
		}

		return service;
	}

	private void initializeTheme() {
		final boolean first = mTheme == null;
		if (first) {
			mTheme = getResources().newTheme();
			Resources.Theme theme = getBaseContext().getTheme();
			if (theme != null) {
				mTheme.setTo(theme);
			}
		}
		mTheme.applyStyle(mThemeResource, true);
	}

	@Override
	public String getPackageName() {
		// packagemanager、activitymanager、wifi、window、inputservice、toast等等系统服务会获取上下文的packageName去查询信息，
		// 插件自身的packageName对系统而言是不合法的，这里必须用宿主的packageName
		return PluginLoader.getApplication().getPackageName();
	}

	// @hide
	public String getBasePackageName() {
		// ViewRootImpl中会调用这个方法, 这是个hide方法.
		return PluginLoader.getApplication().getPackageName();
	}

	// //@hide
	public String getOpPackageName() {
		return PluginLoader.getApplication().getPackageName();
	}

	@Override
	public Context getApplicationContext() {
		return mPluginApplication;
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		// 这里的ApplicationInfo是从LoadedApk中取出来的
		// 由于目前插件之间是共用1个插件进程。LoadedApk只有1个，而ApplicationInfo每个插件都有一个，
		// 所以不能通过直接修改loadedApk中的内容来修正这个方法的返回值，而是将修正的过程放在Context中去做，
		// 避免多个插件之间造成干扰
		if (mApplicationInfo == null) {
			try {
				mApplicationInfo = getPackageManager().getApplicationInfo(mPluginDescriptor.getPackageName(), 0);
				// 这里修正packageManager中hook时设置的插件packageName
				mApplicationInfo.packageName = getPackageName();
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		return mApplicationInfo;
	}

	@Override
	public String getPackageCodePath() {
		return mPluginDescriptor.getInstalledPath();
	}

	@Override
	public String getPackageResourcePath() {
		return mPluginDescriptor.getInstalledPath();
	}

	public PluginDescriptor getPluginDescriptor() {
		return mPluginDescriptor;
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		if (receiver != null) {
			receivers.add(receiver);
		}
		return super.registerReceiver(receiver, filter);
	}

	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		if (receivers.contains(receiver)) {
			super.unregisterReceiver(receiver);
			receivers.remove(receiver);
		}
	}

	public void unregisterAllReceiver() {
		for (BroadcastReceiver br : receivers) {
			if (br != null) {
				super.unregisterReceiver(br);
			}
		}
		receivers.clear();
	}

	@Override
	public PackageManager getPackageManager() {
		if (crackPackageManager) {
			// 欺骗MultDexInstaller， 使MultDexInstaller能得到正确的插件信息
			return PluginMultiDexHelper.fixPackageManagerForMultDexInstaller(mPluginDescriptor.getPackageName(),
					super.getPackageManager());
		}
		return super.getPackageManager();
	}

	public void setCrackPackageManager(boolean crackPackageManager) {
		this.crackPackageManager = crackPackageManager;
	}

	/**
	 * 隔离插件间的SharedPreferences
	 * 
	 * @param name
	 * @param mode
	 * @return
	 */
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (Build.VERSION.SDK_INT > 23) {
			synchronized (PluginContextTheme.class) {
				HackContextImpl impl = new HackContextImpl(getContextImpl());

				ArrayMap<String, File> mSharedPrefsPaths = impl.getSharedPrefsPaths();
				File sharedPrefsFile = new File(getPluginRootDir(), "shared_prefs");
				String sharedPrefsPath = sharedPrefsFile.getAbsolutePath();
				QRomLog.w(TAG, "getSharedPreferences sharedPrefsFile is " + sharedPrefsPath);
				if (mSharedPrefsPaths != null) {
					File file = mSharedPrefsPaths.get(name);

					// log
					if (file != null) {
						QRomLog.w(TAG, "mSharedPrefsPaths.get:" + name + " is " + file.getAbsolutePath());
					}

					if (file != null && !file.getParent().equals(sharedPrefsPath)) {
						QRomLog.w(TAG, "mSharedPrefsPaths.remove:" + name);
						mSharedPrefsPaths.remove(name);// 置空之后再get会触发重建，则getDataDir有机会生效
					}
				}

				File mPreferencesDir = impl.getPreferencesDir();
				if (mPreferencesDir == null || !mPreferencesDir.getAbsolutePath().equals(sharedPrefsPath)) {
					QRomLog.w(TAG, "impl.setPreferencesDir(sharedPrefsFile)");
					impl.setPreferencesDir(sharedPrefsFile);
				}
			}

			return getSharedPreferencesEx(name, mode);
		}

		// 这里之所以需要追加前缀是因为ContextImpl类中的全局静态缓存sSharedPrefs
		if (!name.startsWith(mPluginDescriptor.getPackageName() + "_")) {
			name = mPluginDescriptor.getPackageName() + "_" + name;
		}

		// 4.4以上版本缓存是延迟初始化的，这里增加这句调用是为了确保已经初始化，防止反射为空
		PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		Object cache = HackContextImpl.getSharedPrefs();
		if (Build.VERSION.SDK_INT >= 19 && cache instanceof ArrayMap) {
			synchronized (PluginContextTheme.class) {
				ArrayMap<String, ArrayMap<String, Object>> sSharedPrefs = (ArrayMap<String, ArrayMap<String, Object>>) cache;
				final String packageName = getPackageName();
				ArrayMap<String, Object> packagePrefs = sSharedPrefs.get(packageName);
				if (packagePrefs == null) {
					packagePrefs = new ArrayMap<String, Object>();
					sSharedPrefs.put(packageName, packagePrefs);
				}

				Object sp = packagePrefs.get(name);
				if (sp == null) {
					packagePrefs.put(name, CompatForSharedPreferencesImpl.newSharedPreferencesImpl(
							getSharedPrefsFile(name), mode, getPackageName()));
				}
			}
		} else if (cache instanceof HashMap) {
			HashMap<String, Object> sSharedPrefs = (HashMap<String, Object>) cache;
			Object sp = sSharedPrefs.get(name);
			if (sp == null) {
				sSharedPrefs.put(name, CompatForSharedPreferencesImpl.newSharedPreferencesImpl(
						getSharedPrefsFile(name), mode, getPackageName()));
			}
		}

		return super.getSharedPreferences(name, mode);
	}

	// android-M
	// removed
	public File getSharedPreferencesPath(String name) {
		return getSharedPrefsFile(name);
	}

	private File getSharedPrefsFile(String name) {
		if (!name.startsWith(mPluginDescriptor.getPackageName() + "_")) {
			name = mPluginDescriptor.getPackageName() + "_" + name;
		}
		return makeFilename(new File(getPluginRootDir(), "shared_prefs"), name + ".xml");
	}

	@Override
	public File getDir(String name, int mode) {
		File dir = makeFilename(getPluginRootDir(), "app_" + name);
		if (!dir.exists()) {
			dir.mkdirs();
			// setpermisssion
		}
		return dir;
	}

	@Override
	public File getFilesDir() {
		File dir = new File(getPluginRootDir(), "files");
		if (!dir.exists()) {
			dir.mkdirs();
			// setpermisssion
		}
		return dir;
	}

	@Override
	public File getFileStreamPath(String name) {
		return makeFilename(getFilesDir(), name);
	}

	@Override
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		final boolean append = (mode & MODE_APPEND) != 0;
		File f = makeFilename(getFilesDir(), name);
		try {
			FileOutputStream fos = new FileOutputStream(f, append);
			// setFilePermissionsFromMode(f.getPath(), mode, 0);
			return fos;
		} catch (FileNotFoundException e) {
		}
		return super.openFileOutput(name, mode);
	}

	@Override
	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		File f = makeFilename(getFilesDir(), name);
		return new FileInputStream(f);
	}

	@Override
	public File getNoBackupFilesDir() {
		File dir = new File(getPluginRootDir(), "no_backup");
		if (!dir.exists()) {
			dir.mkdirs();
			// setpermisssion
		}
		return dir;
	}

	@Override
	public File getCacheDir() {
		File dir = new File(getPluginRootDir(), "cache");
		if (!dir.exists()) {
			dir.mkdirs();
			// setpermisssion
		}
		return dir;
	}

	@Override
	public File getCodeCacheDir() {
		File dir = new File(getPluginRootDir(), "code_cache");
		if (!dir.exists()) {
			dir.mkdirs();
			// setpermisssion
		}
		return dir;
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
		name = getAbsuloteDatabasePath(name);
		return super.openOrCreateDatabase(name, mode, factory);
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
			DatabaseErrorHandler errorHandler) {
		name = getAbsuloteDatabasePath(name);
		return super.openOrCreateDatabase(name, mode, factory, errorHandler);
	}

	@Override
	public boolean deleteDatabase(String name) {
		name = getAbsuloteDatabasePath(name);
		return super.deleteDatabase(name);
	}

	@Override
	public File getDatabasePath(String name) {
		name = getAbsuloteDatabasePath(name);
		return super.getDatabasePath(name);
	}

	@Override
	public String[] databaseList() {
		File f = new File(getPluginRootDir(), "databases");
		final String[] list = f.list();
		return (list != null) ? list : EMPTY_STRING_ARRAY;
	}

	@Override
	public String[] fileList() {
		final String[] list = getFilesDir().list();
		return (list != null) ? list : EMPTY_STRING_ARRAY;
	}

	@Override
	public boolean deleteFile(String name) {
		File f = makeFilename(getFilesDir(), name);
		return f.delete();
	}

	private String getAbsuloteDatabasePath(String name) {
		if (name.charAt(0) != File.separatorChar) {
			File f = makeFilename(new File(getPluginRootDir(), "databases"), name);
			name = f.getAbsolutePath();
		}
		return name;
	}

	private File makeFilename(File base, String name) {
		if (name.indexOf(File.separatorChar) < 0) {
			if (!base.exists()) {
				base.mkdirs();
				// setpermisssion
			}
			return new File(base, name);
		}
		throw new IllegalArgumentException("File " + name + " contains a path separator");
	}

	private static final String[] EMPTY_STRING_ARRAY = {};

	private File dataDir;

	// android-N
	// @Override
	public File getPluginRootDir() {
		if (dataDir == null) {
			File pluginFile = new File(mPluginDescriptor.getInstalledPath());
			QRomLog.i(TAG, "getDataDir pluginFile is " + pluginFile.getAbsolutePath());
			dataDir = new File(pluginFile.getParentFile().getParentFile(), "data");
			if (!dataDir.exists()) {
				dataDir.mkdirs();
			}
		}
		return dataDir;
	}

	public Context getOuter() {
		Context base = getBaseContext();
		while (base instanceof ContextWrapper) {
			base = ((ContextWrapper) base).getBaseContext();
		}
		if (HackContextImpl.instanceOf(base)) {
			base = new HackContextImpl(base).getOuterContext();
		}
		return base;
	}
}
