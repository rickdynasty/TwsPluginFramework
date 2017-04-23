package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.plugindemo.R;

public class ActionBarSamples extends TwsActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_test_actionbar);
		setTitle("ActionBarSamples");

		findViewById(R.id.action_bar_displayoptions).setOnClickListener(this);
		findViewById(R.id.action_bar_normalbutton).setOnClickListener(this);
		findViewById(R.id.action_menu_selectedtext).setOnClickListener(this);
		findViewById(R.id.action_mode).setOnClickListener(this);
		findViewById(R.id.action_bar_gradient).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		Intent intent = null;
		switch (view.getId()) {
		case R.id.action_bar_displayoptions:
			intent = new Intent();
			intent.setClassName(this, ActionBarDisplayOptions.class.getName());
			break;
		case R.id.action_bar_normalbutton:
			intent = new Intent();
			intent.setClassName(this, ActionBarNormalButton.class.getName());
			break;
		case R.id.action_menu_selectedtext:
			intent = new Intent();
			intent.setClassName(this, ActionMenuSelectedText.class.getName());
			break;
		case R.id.action_mode:
			intent = new Intent();
			intent.setClassName(this, ActionModeNormal.class.getName());
			break;
		case R.id.action_bar_gradient:
			intent = new Intent();
			intent.setClassName(this, ActionBarGradient.class.getName());
			break;
		default:
			break;
		}

		if (intent != null) {
			startActivity(intent);
		}
	}

}
