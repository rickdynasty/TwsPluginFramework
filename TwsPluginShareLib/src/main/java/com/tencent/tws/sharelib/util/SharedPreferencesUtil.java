package com.tencent.tws.sharelib.util;

import java.util.HashMap;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

@SuppressLint("NewApi")
public class SharedPreferencesUtil {

	// 存储的sharedpreferences文件名
	// note:统一放置
	private static final String SP_HEALTH_FILE_NAME = "health_band";
	private static final String SP_KRONABY_FILE_NAME = "kronaby_sp";
	private static final String DEFAULT_FILE_NAME = "host_default_sp_file";
	// key is [PackageName_name_mode]
	private HashMap<String, SharedPreferences> mCache = new HashMap<String, SharedPreferences>();
	private SharedPreferences mSharedPreferences;

	private static SharedPreferencesUtil mInstance;

	public static SharedPreferencesUtil getInstance() {
		if (mInstance == null) {
			synchronized (SharedPreferencesUtil.class) {
				if (mInstance == null) {
					mInstance = new SharedPreferencesUtil();
				}
			}
		}

		return mInstance;
	}

	public SharedPreferencesUtil getHealthSharedPreferences(Context context) {
		return getSharedPreferences(context, SP_HEALTH_FILE_NAME);
	}

	public SharedPreferencesUtil getKronabySharedPreferences(Context context) {
		return getSharedPreferences(context, SP_KRONABY_FILE_NAME);
	}

	public SharedPreferencesUtil getSharedPreferences(Context context, String name) {
		return getSharedPreferences(context, name, Context.MODE_PRIVATE);
	}

	public SharedPreferencesUtil getSharedPreferences(Context context, String name, int mode) {
		if (null == context || TextUtils.isEmpty(name)) {
			throw new IllegalAccessError("illegal parameter(context:" + context + ", name:" + name + ", mode:" + mode
					+ ")");
		}

		// 组建key
		final String spKey = context.getPackageName() + "_" + name + "_" + mode;

		SharedPreferences sharedPreferences = mCache.get(spKey);
		if (null == sharedPreferences) {
			sharedPreferences = context.getSharedPreferences(name, mode);
			mCache.put(spKey, sharedPreferences);
		}

		mSharedPreferences = sharedPreferences;

		return mInstance;
	}

	public void putInt(String key, int data) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		mSharedPreferences.edit().putInt(key, data).commit();
		mSharedPreferences = null;
	}

	public void putBoolean(String key, boolean data) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		mSharedPreferences.edit().putBoolean(key, data).commit();
		mSharedPreferences = null;
	}

	public void putString(String key, String data) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		mSharedPreferences.edit().putString(key, data).commit();
		mSharedPreferences = null;
	}

	public void putLong(String key, long data) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		mSharedPreferences.edit().putLong(key, data).commit();
		mSharedPreferences = null;
	}

	public void putFloat(String key, float data) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		mSharedPreferences.edit().putFloat(key, data).commit();
		mSharedPreferences = null;
	}

	public void putStringSet(String key, Set<String> values) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		mSharedPreferences.edit().putStringSet(key, values).commit();
		mSharedPreferences = null;
	}

	// //////////////////// get ////////////////////
	public long getLong(String key, long defValue) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		long rlt = mSharedPreferences.getLong(key, defValue);
		mSharedPreferences = null;
		return rlt;
	}

	public int getInt(String key, int defValue) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		int rlt = mSharedPreferences.getInt(key, defValue);
		mSharedPreferences = null;
		return rlt;
	}

	public boolean getBoolean(String key, boolean defValue) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		boolean rlt = mSharedPreferences.getBoolean(key, defValue);
		mSharedPreferences = null;
		return rlt;
	}

	public String getString(String key, String defValue) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		String rlt = mSharedPreferences.getString(key, defValue);
		mSharedPreferences = null;
		return rlt;
	}

	public float getFloat(String key, float defValue) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}
		float rlt = mSharedPreferences.getFloat(key, defValue);
		mSharedPreferences = null;
		return rlt;
	}

	public Set<String> getStringSet(String key, Set<String> defValues) {
		if (null == mSharedPreferences) {
			throw new IllegalAccessError("pleast call getSharedPreferences first~");
		}

		Set<String> rlt = mSharedPreferences.getStringSet(key, defValues);
		mSharedPreferences = null;
		return rlt;
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static void saveIntData(Context context, String key, int data) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(key, data);
		editor.apply();
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static void saveBooleanData(Context context, String key, boolean data) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, data);
		editor.apply();
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static void saveStringData(Context context, String key, String data) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(key, data);
		editor.apply();
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static int getIntData(Context context, String key, int defValue) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		return sharedPreferences.getInt(key, defValue);
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static boolean getBooleanData(Context context, String key, boolean defValue) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		return sharedPreferences.getBoolean(key, defValue);
	}

	/**
	 * @author p_syongchen
	 * @deprecated
	 */
	public static String getStringData(Context context, String key, String defValue) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
		return sharedPreferences.getString(key, defValue);
	}
}
