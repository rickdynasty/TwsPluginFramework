package com.example.plugindemo.activity.category;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.TwsButton;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class TwsButtonSimples extends TwsActivity implements OnClickListener {
	TwsButton mButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twsbutton_samples);
		findViewById(R.id.tws_button_01).setOnClickListener(this);
		findViewById(R.id.tws_button_02).setOnClickListener(this);
		findViewById(R.id.tws_button_03).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.tws_button_01:

			break;
		case R.id.tws_button_02:

			break;
		case R.id.tws_button_03:

			break;

		default:
			break;
		}
	}

}
