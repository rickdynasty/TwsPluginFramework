package com.example.plugindemo.activity;

import android.app.TwsActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.tab.TabViewpagerActivity;
import com.tencent.tws.assistant.app.ProgressDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.sharelib.SharePOJO;

public class TestShareWidgetAcitivity extends TwsActivity implements View.OnClickListener {

	private ProgressDialog mProgressDialog;
	private Handler mProgressHandler;
	private int mProgress;
	private static final int MAX_PROGRESS = 100;
	private static final int DIALOG_PROGRESS = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_share_widget);

		findViewById(R.id.progress_dialog).setOnClickListener(this);
		findViewById(R.id.widget_tabviewpager).setOnClickListener(this);
		findViewById(R.id.widget_sidebar).setOnClickListener(this);
		findViewById(R.id.widget_toast).setOnClickListener(this);
		
		getTwsActionBar().setTitle("测试共享控件");

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
		case R.id.progress_dialog:
			showProgressDialog();
			break;
		case R.id.widget_tabviewpager:
			// 利用className打开共享控件的测试activity
			intent = new Intent();
			intent.setClassName(this, TabViewpagerActivity.class.getName());
			startActivity(intent);
			break;
		case R.id.widget_sidebar:
			// 利用className打开共享控件的测试activity
			intent = new Intent();
			intent.setClassName(this, SideBarActivity.class.getName());
			intent.putExtra("testParam", "testParam");
			intent.putExtra("paramVO", new SharePOJO("测试VO"));
			startActivity(intent);
			break;
		case R.id.widget_toast:
			Toast.makeText(getApplication(), "哈哈~其实没有什么，就一个提示而已！", Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}
	}

	private void showProgressDialog() {
		showTwsDialog(DIALOG_PROGRESS);
		mProgress = 0;
		mProgressDialog.setProgress(0);
		mProgressHandler.sendEmptyMessage(0);
	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			mProgressDialog = new ProgressDialog(TestShareWidgetAcitivity.this);
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
