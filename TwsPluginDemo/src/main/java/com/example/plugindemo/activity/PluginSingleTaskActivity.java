package com.example.plugindemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class PluginSingleTaskActivity extends Activity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button btn = new Button(this);
		btn.setText("点击测试插件SingleTask");
		setContentView(btn);
		btn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Toast.makeText(this, "这是一个SingleTaskActivity", Toast.LENGTH_SHORT).show();

		startActivity(new Intent(this, PluginSingleTaskActivity.class));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Toast.makeText(this, "onNewIntent Called!!", Toast.LENGTH_SHORT).show();
	}
}
