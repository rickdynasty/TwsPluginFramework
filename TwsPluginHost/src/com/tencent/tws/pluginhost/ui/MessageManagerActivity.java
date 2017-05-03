package com.tencent.tws.pluginhost.ui;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.framework.HostProxy;
import com.tencent.tws.pluginhost.R;

public class MessageManagerActivity extends TwsFragmentActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getTwsActionBar().setTitle(getResources().getString(R.string.activity_message_mgr_title));
		
		setContentView(R.layout.activity_message_manager);
		findViewById(R.id.send_notification).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.send_notification:
			testNotification();
			break;

		default:
			break;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void testNotification() {
		// 当前交由宿主执行，使用的资源id得宿主能解析得到
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification.Builder builder = new Notification.Builder(HostProxy.getApplication());

		Intent intent = new Intent();
		// 唤起指定Activity
		intent.setClassName(getPackageName(), MessageManagerActivity.class.getName());
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
}
