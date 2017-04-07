package com.tencent.tws.pluginhost.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.pluginhost.R;

public class MessageManagerActivity extends TwsFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initActionBar();
	}

	private void initActionBar() {
		ActionBar actionBar = getTwsActionBar();
		int actionBarBgColor = getResources().getColor(R.color.my_action_bar_bg_color);
		actionBar.setTitle(getResources().getString(R.string.activity_message_mgr_title));
		actionBar.setBackgroundDrawable(new ColorDrawable(actionBarBgColor));
		actionBar.setStackedBackgroundDrawable(getResources().getDrawable(R.drawable.ab_solid_light_holo_opacity));
	}
}
