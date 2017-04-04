package com.example.plugindemo.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import tws.component.log.TwsLog;
import android.app.ActionBar;
import android.app.TwsActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

import com.example.plugindemo.R;
import com.tencent.tws.sharelib.SharePOJO;

public class LauncherActivity extends TwsActivity implements View.OnClickListener {

	private static final String TAG = "rick_Print:LauncherActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_launcher);

		TwsLog.d("xxx1", "activity_welcome ID= " + R.layout.plugin_launcher);
		Log.e("xxx1", "activity_welcome ID= " + R.layout.plugin_launcher);
		Log.e("xxx2", getResources().getResourceEntryName(R.layout.plugin_launcher));
		TwsLog.d("xxx2", getResources().getResourceEntryName(R.layout.plugin_launcher));
		TwsLog.d(
				"xxx3",
				getResources().getString(R.string.app_name) + "  "
						+ getPackageManager().getApplicationLabel(getApplicationInfo()));
		TwsLog.d(
				"xxx3",
				getResources().getString(R.string.app_name) + "  "
						+ getPackageManager().getApplicationLabel(getApplicationInfo()));
		TwsLog.d("xxx4", getPackageName() + ", " + getText(R.string.app_name));
		TwsLog.d("xxx5", getResources().getString(android.R.string.httpErrorBadUrl));
		TwsLog.d("xxx6",
				getResources().getString(getResources().getIdentifier("app_name", "string", "com.example.plugindemo")));
		TwsLog.d("xxx7", getResources().getString(getResources().getIdentifier("app_name", "string", getPackageName())));
		// TwsLog.e("xxx8",
		// getResources().getString(getResources().getIdentifier("app_name",
		// "string", "com.tencent.tws.gdevicemanager")));

		ActionBar actionBar = getActionBar();
		if (actionBar == null) {
			setTitle("这是插件首屏");
		} else {
			actionBar.setTitle("这是插件首屏");
			actionBar.setSubtitle("这是副标题");
			actionBar.setLogo(R.drawable.ic_launcher);
			actionBar.setIcon(R.drawable.ic_launcher);
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
					| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		}

		findViewById(R.id.onClickHellowrld).setOnClickListener(this);
		findViewById(R.id.onClickFacebook).setOnClickListener(this);
		findViewById(R.id.test_base).setOnClickListener(this);
		findViewById(R.id.test_ShareWidget).setOnClickListener(this);

		if (Build.VERSION.SDK_INT >= 21) {// Build.VERSION_CODES.KITKAT
			findViewById(R.id.onClickFacebook).setOnClickListener(this);
		} else {
			findViewById(R.id.onClickFacebook).setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.onClickHellowrld:
			onClickHellowrld(v);
			break;
		case R.id.onClickFacebook:
			onClickFacebook();
			break;
		case R.id.test_base:
			testBase();
			break;
		case R.id.test_ShareWidget:
			testShareWidget();
			break;
		default:
			break;
		}
	}

	private void testBase() {
		Intent intent = new Intent();
		intent.setClassName(this, TestPluginCoreBaseActivity.class.getName());
		startActivity(intent);
	}

	private void onClickHellowrld(View v) {
		Intent intent = getPackageManager().getLaunchIntentForPackage("com.example.pluginhelloworld");
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickFacebook() {
		// 通过packageManager查询其他插件信息并打开
		Intent intent = getPackageManager().getLaunchIntentForPackage("com.facebook.rebound.demo");
		startActivity(intent);
	}

	private void testShareWidget() {
		// 利用className打开共享控件的测试activity
		Intent intent = new Intent();
		intent.setClassName(this, TestShareWidgetAcitivity.class.getName());
		intent.putExtra("testParam", "testParam");
		intent.putExtra("paramVO", new SharePOJO("测试VO"));
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("cc");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		super.onResume();
		testDataApi();
	}

	private void testDataApi() {

		SharedPreferences sp = getSharedPreferences("aaa", 0);
		sp.edit().putString("xyz", "123").commit();
		File f = getDir("bbb", 0);
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		f = getFilesDir();
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		// if (Build.VERSION.SDK_INT >= 21) {
		// f = getNoBackupFilesDir();
		// TwsLog.d(TAG, f.getAbsoluteFile() + " exists:" + f.exists() +
		// " canRead:" + f.canRead() + " canWrite:" + f.canWrite());
		// }

		f = getCacheDir();
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		// if (Build.VERSION.SDK_INT >= 21) {
		// f = getCodeCacheDir();
		// }
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		SQLiteDatabase db = openOrCreateDatabase("ccc", 0, null);
		try {
			String sql = "create table IF NOT EXISTS  userDb (_id integer primary key autoincrement, column_one text not null);";
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}

		f = getDatabasePath("ccc");
		TwsLog.d(TAG,
				f.getAbsoluteFile() + " exists:" + f.exists() + " canRead:" + f.canRead() + " canWrite:" + f.canWrite());

		String[] list = databaseList();

		try {
			FileOutputStream fo = openFileOutput("ddd", 0);
			fo.write(122);
			fo.flush();
			fo.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		TwsLog.d(TAG, getFileStreamPath("eee").getAbsolutePath());

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyDown keyCode=" + keyCode);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		TwsLog.d(TAG, "onKeyUp keyCode=" + keyCode);
		return super.onKeyUp(keyCode, event);
	}
}