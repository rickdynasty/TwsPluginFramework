package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.category.picker.DatePickerView;
import com.example.plugindemo.activity.category.picker.DateTimePickerView;
import com.example.plugindemo.activity.category.picker.PickerDialogOutside;
import com.example.plugindemo.activity.category.picker.ProfileDataPickerView;
import com.example.plugindemo.activity.category.picker.TimePickerView;

public class PickersSamples extends TwsActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pickers_samples);

		findViewById(R.id.date_picker_view).setOnClickListener(this);
		findViewById(R.id.date_time_picker_view).setOnClickListener(this);
		findViewById(R.id.picker_dialog_outside).setOnClickListener(this);
		findViewById(R.id.profile_data_picker_view).setOnClickListener(this);
		findViewById(R.id.time_picker_view).setOnClickListener(this);

		getTwsActionBar().setTitle("Pickers示例");
	}

	@Override
	public void onClick(View view) {
		Intent intent;
		switch (view.getId()) {
		case R.id.date_picker_view:
			intent = new Intent();
			intent.setClassName(this, DatePickerView.class.getName());
			startActivity(intent);
			break;
		case R.id.date_time_picker_view:
			intent = new Intent();
			intent.setClassName(this, DateTimePickerView.class.getName());
			startActivity(intent);
			break;
		case R.id.picker_dialog_outside:
			intent = new Intent();
			intent.setClassName(this, PickerDialogOutside.class.getName());
			startActivity(intent);
			break;
		case R.id.profile_data_picker_view:
			intent = new Intent();
			intent.setClassName(this, ProfileDataPickerView.class.getName());
			startActivity(intent);
			break;
		case R.id.time_picker_view:
			intent = new Intent();
			intent.setClassName(this, TimePickerView.class.getName());
			startActivity(intent);
			break;
		default:
			break;
		}
	}
}
