package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.PreferenceActivityDemo;
import com.tencent.tws.assistant.app.ProgressDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.Toast;

public class ShareWidgetSamples extends TwsActivity implements View.OnClickListener {

	private ProgressDialog mProgressDialog;
	private Handler mProgressHandler;
	private int mProgress;
	private static final int MAX_PROGRESS = 100;
	private static final int DIALOG_PROGRESS = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_share_widget);

		findViewById(R.id.alert_dialog).setOnClickListener(this);
		findViewById(R.id.widget_tab).setOnClickListener(this);
		findViewById(R.id.widget_toast).setOnClickListener(this);
		findViewById(R.id.widget_sidebar).setOnClickListener(this);
		findViewById(R.id.tws_base_widget).setOnClickListener(this);

		findViewById(R.id.tws_pickers_samples).setOnClickListener(this);
		findViewById(R.id.preference_activity).setOnClickListener(this);
		findViewById(R.id.listview_samples).setOnClickListener(this);

		getTwsActionBar().setTitle("共享控件");

		mProgressHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (mProgress >= MAX_PROGRESS) {
					mProgressDialog.dismiss();
				} else {
					mProgress++;
					mProgressDialog.incrementProgressBy(1);
					mProgressHandler.sendEmptyMessageDelayed(0, 100);
				}
			}
		};
	}

	@Override
	public void onClick(View view) {
		Intent intent;
		switch (view.getId()) {
		case R.id.alert_dialog:
			// 利用className打开共享控件的测试activity
			intent = new Intent();
			intent.setClassName(this, AlertDialogSamples.class.getName());
			startActivity(intent);
			break;
		case R.id.widget_tab:
			// 利用className打开共享控件的测试activity
			intent = new Intent();
			intent.setClassName(this, TabSamples.class.getName());
			startActivity(intent);
			break;
		case R.id.widget_sidebar:
			// 利用className打开共享控件的测试activity
			intent = new Intent();
			intent.setClassName(this, SideBarActivity.class.getName());
			intent.putExtra("testParam", "testParam");
			startActivity(intent);
			break;
		case R.id.widget_toast:
			Toast.makeText(this, "哈哈~其实没有什么，就一个提示而已！", Toast.LENGTH_SHORT).show();
			break;
		case R.id.tws_base_widget:
			intent = new Intent();
			intent.setClassName(this, TwsBaseWidget.class.getName());
			startActivity(intent);
			break;
		case R.id.tws_pickers_samples:
			intent = new Intent();
			intent.setClassName(this, PickersSamples.class.getName());
			startActivity(intent);
			break;
		case R.id.preference_activity:
			intent = new Intent();
			intent.setClassName(this, PreferenceActivityDemo.class.getName());
			startActivity(intent);
			break;

		case R.id.listview_samples:
			intent = new Intent();
			intent.setClassName(this, ListViewSamples.class.getName());
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			mProgressDialog = new ProgressDialog(ShareWidgetSamples.this);
			mProgressDialog.setIconAttribute(android.R.attr.alertDialogIcon);
			mProgressDialog.setTitle("Header title");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMax(100);
			mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "隐藏", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			return mProgressDialog;

		default:
			break;
		}

		return null;
	}
}
