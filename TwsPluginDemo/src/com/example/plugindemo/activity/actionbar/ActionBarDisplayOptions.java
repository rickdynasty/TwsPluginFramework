/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.ActionBar;

/**
 * This demo shows how various action bar display option flags can be combined
 * and their effects.
 */
public class ActionBarDisplayOptions extends TwsActivity implements View.OnClickListener {
	private View mCustomView;
	private Button mToggleBack;
	private Button mToggleBackEnable;
	private Button mToggleTitle;
	private Button mToggleActionbar;
	private Button mToggleCustom;
	private ActionBar mActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.action_bar_display_options);
		mActionBar = getTwsActionBar();
		mActionBar.setTitle(R.string.action_bar_title);
		mActionBar.setSubtitle(R.string.action_bar_subtitle);
		initViews();
	}

	private void initViews() {
		mToggleBack = (Button) findViewById(R.id.toggle_back);
		mToggleBackEnable = (Button) findViewById(R.id.toggle_back_enable);
		mToggleTitle = (Button) findViewById(R.id.toggle_title);
		mToggleActionbar = (Button) findViewById(R.id.toggle_actionbar);
		mToggleCustom = (Button) findViewById(R.id.toggle_custom);
		mCustomView = getLayoutInflater().inflate(R.layout.action_bar_display_options_custom, null);

		mToggleBack.setOnClickListener(this);
		mToggleBackEnable.setOnClickListener(this);
		mToggleTitle.setOnClickListener(this);
		mToggleActionbar.setOnClickListener(this);
		mToggleCustom.setOnClickListener(this);
		mActionBar.setCustomView(mCustomView);
		mActionBar.setDisplayShowCustomEnabled(false);
		mActionBar.setShowHideAnimationEnabled(true);
	}

	@Override
	public void onClick(View v) {
		int displayOptions = mActionBar.getDisplayOptions();
		switch (v.getId()) {
		case R.id.toggle_back:
			if ((displayOptions & ActionBar.DISPLAY_SHOW_HOME) != 0) {
				mActionBar.setDisplayShowHomeEnabled(false);
				mToggleBack.setText(R.string.toggle_back_on);
			} else {
				mActionBar.setDisplayShowHomeEnabled(true);
				mToggleBack.setText(R.string.toggle_back_off);
			}
			break;
		case R.id.toggle_back_enable:
			if (mActionBar.twsGetBackOnclickEnabled()) {
				mActionBar.twsSetBackOnclickEnabled(false);
				mToggleBackEnable.setText(R.string.toggle_back_enable_on);
			} else {
				mActionBar.twsSetBackOnclickEnabled(true);
				mToggleBackEnable.setText(R.string.toggle_back_enable_off);
			}
			break;
		case R.id.toggle_title:
			if ((displayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
				mActionBar.setDisplayShowTitleEnabled(false);
				mToggleTitle.setText(R.string.toggle_title_on);
			} else {
				mActionBar.setDisplayShowTitleEnabled(true);
				mToggleTitle.setText(R.string.toggle_title_off);
			}
			break;
		case R.id.toggle_actionbar:
			if (mActionBar.isShowing()) {
				mActionBar.hide();
				mToggleActionbar.setText(R.string.toggle_actionbar_on);
			} else {
				mActionBar.show();
				mToggleActionbar.setText(R.string.toggle_actionbar_off);
			}
			break;
		case R.id.toggle_custom:
			if ((displayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
				mActionBar.setDisplayShowCustomEnabled(false);
				mToggleCustom.setText(R.string.toggle_custom_on);
			} else {
				mActionBar.setDisplayShowCustomEnabled(true);
				mToggleCustom.setText(R.string.toggle_custom_off);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Toast.makeText(ActionBarDisplayOptions.this, "Home Click", Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.action_bar_menu, menu);
		return true;
	}

	@Override
	public String changeTwsStatusBarColor() {
		return STATUSBAR_COLOR_BLACK;
	}
}
