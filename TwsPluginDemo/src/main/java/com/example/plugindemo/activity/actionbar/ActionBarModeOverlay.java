package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.Window;

public class ActionBarModeOverlay extends TwsActivity {

	@Override
	protected void onCreate(Bundle bundle) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(bundle);
	}

}
