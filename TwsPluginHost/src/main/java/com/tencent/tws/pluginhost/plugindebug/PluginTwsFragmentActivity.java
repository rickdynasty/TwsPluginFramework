package com.tencent.tws.pluginhost.plugindebug;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.assistant.support.v4.app.FragmentTransaction;
import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.pluginhost.R;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.annotation.PluginContainer;

/**
 * @author yongchen
 */
public class PluginTwsFragmentActivity extends TwsFragmentActivity implements PluginContainer {

	public static final String FRAGMENT_ID_IN_PLUGIN = "PluginDispatcher.fragmentId";
	public static final String FRAGMENT_PLUGIN_ID = "PluginDispatcher.fragment.PluginId";
	private static final String LOG_TAG = PluginFragmentActivity.class.getSimpleName();
	private String mPluginID = "";
	private String mFragmentID = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_plugin_fragment);
		mPluginID = getIntent().getStringExtra(FRAGMENT_PLUGIN_ID);

		String classId = getIntent().getStringExtra(FRAGMENT_ID_IN_PLUGIN);
		if (classId == null && savedInstanceState != null) {
			classId = savedInstanceState.getString(FRAGMENT_ID_IN_PLUGIN);
		}

		loadPluginFragment(classId);
	}

	@Override
	public void setPluginId(String id) {
		mPluginID = id;
	}

	@Override
	public String getPluginId() {
		return mPluginID;
	}

	private void loadPluginFragment(String classId) {
		try {
			if (classId == null) {
				Toast.makeText(this, "缺少参数:PluginDispatcher.fragmentId", Toast.LENGTH_SHORT).show();
				mFragmentID = null;
				return;
			}
			Log.d(LOG_TAG, "loadPluginFragment, classId is " + classId);
			mFragmentID = classId;

			@SuppressWarnings("rawtypes")
			Class clazz = PluginLoader.loadPluginFragmentClassById(classId);
			Fragment fragment = (Fragment) clazz.newInstance();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.fragment_container, fragment).commit();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null) {
			outState.remove(FRAGMENTS_TAG);
		}

		if (!TextUtils.isEmpty(mFragmentID)) {
			outState.putString(FRAGMENT_ID_IN_PLUGIN, mFragmentID);
		}
	}
}
