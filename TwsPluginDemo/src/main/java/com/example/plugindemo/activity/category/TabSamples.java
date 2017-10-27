package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.category.tab.TabViewpagerActivity;
import com.example.plugindemo.activity.category.tab.TwsActionBarTab;
import com.example.plugindemo.activity.category.tab.TwsActionBarTabCustom;
import com.example.plugindemo.activity.category.tab.TwsActionBarTabSecond;
import com.example.plugindemo.activity.category.tab.TwsActionBarTabSecondCustom;

public class TabSamples extends TwsActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_samples);

		findViewById(R.id.tab_view_pager).setOnClickListener(this);
		findViewById(R.id.tws_actionbar_tab).setOnClickListener(this);
		findViewById(R.id.tws_actionbar_tab_custom).setOnClickListener(this);
		findViewById(R.id.tws_actionbar_second).setOnClickListener(this);
		findViewById(R.id.tws_actionbar_second_custom).setOnClickListener(this);

		getTwsActionBar().setTitle("Pickers示例");
	}

	@Override
	public void onClick(View view) {
		Intent intent = null;
		switch (view.getId()) {
		case R.id.tab_view_pager:
			intent = new Intent();
			intent.setClassName(this, TabViewpagerActivity.class.getName());
			break;
		case R.id.tws_actionbar_tab:
			intent = new Intent();
			intent.setClassName(this, TwsActionBarTab.class.getName());
			break;
		case R.id.tws_actionbar_tab_custom:
			intent = new Intent();
			intent.setClassName(this, TwsActionBarTabCustom.class.getName());
			break;
		case R.id.tws_actionbar_second:
			intent = new Intent();
			intent.setClassName(this, TwsActionBarTabSecond.class.getName());
			break;
		case R.id.tws_actionbar_second_custom:
			intent = new Intent();
			intent.setClassName(this, TwsActionBarTabSecondCustom.class.getName());
			break;
		default:
			break;
		}

		if (intent != null) {
			startActivity(intent);
		}
	}
}
