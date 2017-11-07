package com.rick.tws.pluginhost.main.ui;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rick.tws.pluginhost.R;

public class SettingsActivity extends AppCompatActivity implements OnClickListener {

	private RelativeLayout mAccountLayout, mFeedbackLayout, mConnectProtectLayout, mPrivacyLayout,
			mWatchAssistantLayout, mDebugPluginFramework;
	private View mAccountLogOut;
	private TextView mVersionCodeText, mVersionTipsText, mNickName, mDebug_trapdoor;
	private ImageView mRedpointImg, mAccoutnImg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initActionBar();

		setContentView(R.layout.activity_sttings);
		mPrivacyLayout = (RelativeLayout) findViewById(R.id.about_watch_assistant_privacy_layout);
		mAccountLogOut = (View) findViewById(R.id.settings_account_logout_btn);
		mDebug_trapdoor = (TextView) findViewById(R.id.debug_trapdoor);
		mDebugPluginFramework = (RelativeLayout) findViewById(R.id.debug_plugin_framework);
		if (ActivityManager.isUserAMonkey()) {
			mAccountLogOut.setEnabled(false);
		}

		mDebugPluginFramework.setVisibility(View.VISIBLE);
		mDebugPluginFramework.setOnClickListener(this);
	}

	private void initActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(getResources().getString(R.string.activity_settings_title));
		actionBar.setStackedBackgroundDrawable(getResources().getDrawable(R.mipmap.ab_solid_light_holo_opacity));
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.debug_plugin_framework:
			Intent intent = new Intent(this, DebugPluginActivity.class);
			startActivity(intent);
			break;
		}
	}
}
