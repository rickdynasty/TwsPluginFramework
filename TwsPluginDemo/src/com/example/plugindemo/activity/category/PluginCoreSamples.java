package com.example.plugindemo.activity.category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import tws.component.log.TwsLog;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TwsActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.LauncherActivity;
import com.example.plugindemo.activity.PluginForDialogActivity;
import com.example.plugindemo.activity.PluginFragmentTestActivity;
import com.example.plugindemo.activity.PluginSingleTaskActivity;
import com.example.plugindemo.activity.PluginTestActivity;
import com.example.plugindemo.activity.PluginTestOpenPluginActivity;
import com.example.plugindemo.activity.PluginWebViewActivity;
import com.example.plugindemo.activity.TransparentActivity;
import com.example.plugindemo.activity.category.tab.PluginTestTabActivity;
import com.example.plugindemo.provider.PluginDbTables;
import com.example.plugindemo.receiver.PluginTestReceiver2;
import com.example.plugindemo.service.PluginTestService;
import com.tencent.tws.framework.HostProxy;

public class PluginCoreSamples extends TwsActivity implements OnClickListener {
	private static final String TAG = "rick_Print:TestPluginCoreBaseActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_base);
		getTwsActionBar().setTitle("框架基础能力");
		findViewById(R.id.onClickPluginNormalFragment).setOnClickListener(this);
		findViewById(R.id.onClickPluginSpecFragment).setOnClickListener(this);
		findViewById(R.id.onClickPluginSpecTwsFragment).setOnClickListener(this);
		findViewById(R.id.onClickPluginForDialogActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginForOppoAndVivoActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginNotInManifestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginFragmentTestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginSingleTaskActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestOpenPluginActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestTabActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginWebViewActivity).setOnClickListener(this);
		findViewById(R.id.onClickTransparentActivity).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestReceiver).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestReceiver2).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestService).setOnClickListener(this);
		findViewById(R.id.onClickPluginTestService2).setOnClickListener(this);
		findViewById(R.id.db_insert).setOnClickListener(this);
		findViewById(R.id.db_read).setOnClickListener(this);
		findViewById(R.id.test_read_assert).setOnClickListener(this);
		findViewById(R.id.test_notification).setOnClickListener(this);
	}

	private static void startFragmentInHostActivity(Context context, String targetId) {
		Intent pluginActivity = new Intent();
		pluginActivity.setClassName(context, "com.tencent.tws.pluginhost.plugindebug.PluginFragmentActivity");
		pluginActivity.putExtra("PluginDispatcher.fragmentId", targetId);
		pluginActivity.putExtra("PluginDispatcher.fragment.PluginId", "com.example.plugindemo");
		pluginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(pluginActivity);
	}

	private static void startTwsFragmentInHostActivity(Context context, String targetId) {
		Intent pluginActivity = new Intent();
		pluginActivity.setClassName(context, "com.tencent.tws.pluginhost.plugindebug.PluginTwsFragmentActivity");
		pluginActivity.putExtra("PluginDispatcher.fragmentId", targetId);
		pluginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(pluginActivity);
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.onClickPluginNormalFragment:
			onClickPluginNormalFragment(v);
			break;
		case R.id.onClickPluginSpecFragment:
			onClickPluginSpecFragment(v);
			break;
		case R.id.onClickPluginSpecTwsFragment:
			onClickPluginSpecTwsFragment(v);
			break;
		case R.id.onClickPluginForDialogActivity:
			onClickPluginForDialogActivity(v);
			break;
		case R.id.onClickPluginForOppoAndVivoActivity:
			onClickPluginForOppoAndVivoActivity(v);
			break;
		case R.id.onClickPluginNotInManifestActivity:
			onClickPluginNotInManifestActivity(v);
			break;
		case R.id.onClickPluginFragmentTestActivity:
			onClickPluginFragmentTestActivity(v);
			break;
		case R.id.onClickPluginSingleTaskActivity:
			onClickPluginSingleTaskActivity(v);
			break;
		case R.id.onClickPluginTestActivity:
			onClickPluginTestActivity(v);
			break;
		case R.id.onClickPluginTestOpenPluginActivity:
			onClickPluginTestOpenPluginActivity(v);
			break;
		case R.id.onClickPluginTestTabActivity:
			onClickPluginTestTabActivity(v);
			break;
		case R.id.onClickPluginWebViewActivity:
			onClickPluginWebViewActivity(v);
			break;
		case R.id.onClickTransparentActivity:
			onClickTransparentActivity(v);
			break;
		case R.id.onClickPluginTestReceiver:
			onClickPluginTestReceiver(v);
			break;
		case R.id.onClickPluginTestReceiver2:
			onClickPluginTestReceiver2(v);
			break;
		case R.id.onClickPluginTestService:
			onClickPluginTestService(v);
			break;
		case R.id.onClickPluginTestService2:
			onClickPluginTestService2(v);
			break;
		case R.id.db_insert:
			// 插件ContentProvider是在插件首次被唤起时安装的, 属于动态安装。
			// 因此需要在插件被唤起后才可以使用相应的ContentProvider
			// 若要静态安装，需要更改PluginLoader的安装策略～
			ContentValues values = new ContentValues();
			values.put(PluginDbTables.PluginFirstTable.MY_FIRST_PLUGIN_NAME, "test web" + System.currentTimeMillis());
			getContentResolver().insert(PluginDbTables.PluginFirstTable.CONTENT_URI, values);
			Toast.makeText(this, "ContentResolver insert test web", Toast.LENGTH_SHORT).show();
			break;
		case R.id.db_read:
			boolean isSuccess = false;
			Cursor cursor = getContentResolver().query(PluginDbTables.PluginFirstTable.CONTENT_URI, null, null, null,
					null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int index = cursor.getColumnIndex(PluginDbTables.PluginFirstTable.MY_FIRST_PLUGIN_NAME);
					if (index != -1) {
						isSuccess = true;
						String pluginName = cursor.getString(index);
						TwsLog.d(TAG, pluginName);
						Toast.makeText(this, "ContentResolver " + pluginName + " count=" + cursor.getCount(),
								Toast.LENGTH_SHORT).show();
					}
				}
				cursor.close();
			}
			if (!isSuccess) {
				Toast.makeText(this, "ContentResolver 查无数据", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.test_read_assert:
			testReadAssert();
			break;
		case R.id.test_notification:
			testNotification();
			break;
		default:
			break;
		}
	}

	private void onClickPluginNormalFragment(View v) {
		startFragmentInHostActivity(this, "some_id_for_fragment1");
	}

	private void onClickPluginSpecFragment(View v) {
		startFragmentInHostActivity(this, "some_id_for_fragment2");
	}

	private void onClickPluginSpecTwsFragment(View v) {
		startTwsFragmentInHostActivity(this, "some_id_for_fragment3");
	}

	private void onClickPluginForDialogActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginForDialogActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginForOppoAndVivoActivity(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.ijk");
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginNotInManifestActivity(View v) {
		// 利用scheme打开
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setData(Uri.parse("testscheme://testhost"));
		intent.putExtra("testParam", "testParam");
		startActivity(intent);

	}

	private void onClickPluginFragmentTestActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginFragmentTestActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginSingleTaskActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginSingleTaskActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginTestActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);

	}

	private void onClickPluginTestOpenPluginActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestOpenPluginActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginTestTabActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestTabActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginWebViewActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginWebViewActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickTransparentActivity(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, TransparentActivity.class.getName());
		intent.putExtra("testParam", "testParam");
		startActivity(intent);
	}

	private void onClickPluginTestReceiver(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.rst2");// 两个Receive都配置了这个aciton，这里可以同时唤起两个Receiver
		intent.putExtra("testParam", "testParam");
		sendBroadcast(intent);
	}

	private void onClickPluginTestReceiver2(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestReceiver2.class.getName());
		intent.putExtra("testParam", "testParam");
		sendBroadcast(intent);
	}

	private void onClickPluginTestService(View v) {
		// 利用className打开
		Intent intent = new Intent();
		intent.setClassName(this, PluginTestService.class.getName());
		intent.putExtra("testParam", "testParam");
		startService(intent);
		// stopService(intent);
	}

	private void onClickPluginTestService2(View v) {
		// 利用Action打开
		Intent intent = new Intent("test.lmn2");
		intent.putExtra("testParam", "testParam");
		startService(intent);
		// stopService(intent);
	}

	private void testReadAssert() {
		try {
			InputStream assestInput = getAssets().open("test.json");
			String text = streamToString(assestInput);
			Toast.makeText(this, "read assets from plugin" + text, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void testNotification() {
		// 当前交由宿主执行，使用的资源id得宿主能解析得到
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification.Builder builder = new Notification.Builder(HostProxy.getApplication());

		Intent intent = new Intent();
		// 唤起指定Activity
		intent.setClassName(getPackageName(), LauncherActivity.class.getName());
		// 还可以支持唤起service、receiver等等。
		intent.putExtra("param1", "这是来自通知栏的参数");
		PendingIntent contentIndent = PendingIntent.getActivity(HostProxy.getApplication(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIndent).setSmallIcon(HostProxy.getApplicationIconId())// 设置状态栏里面的图标（小图标）【这里尽可能的用宿主的icon】
				// .setLargeIcon(BitmapFactory.decodeResource(res,R.drawable.i5))//下拉下拉列表里面的图标（大图标）
				// .setTicker("this is bitch!")//设置状态栏的显示的信息
				.setWhen(System.currentTimeMillis())// 设置时间发生时间
				.setAutoCancel(true)// 设置可以清除
				.setContentTitle("来自插件ContentTitle")// 设置下拉列表里的标题
				.setDefaults(Notification.DEFAULT_SOUND)// 设置为默认的声音
				.setContentText("来自插件ContentText");// 设置上下文内容

		if (Build.VERSION.SDK_INT >= 21) {
			// api大于等于21时，测试通知栏携带插件布局资源文件
			// builder.setContent(new
			// RemoteViews(this.getPackageName(),
			// R.layout.plugin_notification));
		}

		Notification notification = builder.getNotification();

		final int notifyId = 100;
		notificationManager.notify(notifyId, notification);
	}

	private static String streamToString(InputStream input) throws IOException {
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader reader = new BufferedReader(isr);

		String line;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		isr.close();
		return sb.toString();
	}

}
