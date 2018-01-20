package com.rick.tws.pluginhost.debug;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.rick.tws.pluginhost.R;
import com.tws.plugin.core.PluginLoader;
import com.tws.plugin.core.annotation.PluginContainer;

/**
 * 一个非常普通的FragmentActivty， 用来展示一个来自插件中的fragment。
 * 这里需要通过注解@FragmentContainer来通知插件框架,此activity要展示
 * 的fragment来自那个插件，从而提前更换当前Activity的Context为插件Context
 * 
 * @author yongchen
 * 
 */
public class PluginFragmentActivity extends FragmentActivity implements PluginContainer {

	public static final String FRAGMENT_ID_IN_PLUGIN = "PluginDispatcher.fragmentId";
	private static final String LOG_TAG = PluginFragmentActivity.class.getSimpleName();
	static final String FRAGMENTS_TAG = "android:fragments";

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
				return;
			}
			Log.d(LOG_TAG, "loadPluginFragment, classId is " + classId);
			@SuppressWarnings("rawtypes")
            Class cls = PluginLoader.loadPluginFragmentClassById(classId);
			if (cls != null) {
				Fragment fragment = (Fragment) cls.newInstance();
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container, fragment).commit();
			}
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

			if (!TextUtils.isEmpty(mFragmentID)) {
				outState.putString(FRAGMENT_ID_IN_PLUGIN, mFragmentID);
			}
		}
	}
}
