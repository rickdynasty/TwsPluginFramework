package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.category.tab.TabViewpagerActivity;

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
			// eg
			intent = new Intent();
			intent.setClassName(this, TabViewpagerActivity.class.getName());
			break;
		case R.id.tws_actionbar_tab:
			break;
		case R.id.tws_actionbar_tab_custom:
			break;
		case R.id.tws_actionbar_second:
			break;
		case R.id.tws_actionbar_second_custom:
			break;
		default:
			break;
		}

		if (intent != null) {
			startActivity(intent);
		}
	}
}
