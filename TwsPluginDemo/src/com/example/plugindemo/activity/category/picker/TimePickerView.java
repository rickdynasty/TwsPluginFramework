/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.plugindemo.activity.category.picker;

import java.util.Calendar;

import android.app.TwsActivity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.TimePickerDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.TimePicker;

public class TimePickerView extends TwsActivity implements OnClickListener {

	// where we display the selected date and time
	private TextView mTimeDisplay;
	static final int TIME_DIALOG_ID = 0;

	// time
	private int mHour;
	private int mMinute;

	private TimePicker mTimePicker;
	private Button mTo24Btn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.numberpicker_time_example);
		setTitle("TimePickerView");
		
		final Calendar c = Calendar.getInstance();
		mHour = c.get(Calendar.HOUR);
		mMinute = c.get(Calendar.MINUTE);

		mTimeDisplay = (TextView) findViewById(R.id.dateDisplay);
		mTimePicker = (TimePicker) findViewById(R.id.timePicker);

		mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				mHour = hourOfDay;
				mMinute = minute;
				updateDisplay();
			}
		});
		findViewById(R.id.pickTime).setOnClickListener(this);
		updateDisplay();
		updateTimePicker(mHour, mMinute);

		mTo24Btn = (Button) findViewById(R.id.to_apply_24_hour);
		mTo24Btn.setOnClickListener(this);
	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			boolean is24HourView = false;
			ContentResolver cv = this.getContentResolver();
			// String strTimeFormat =
			// android.provider.Settings.System.getString(cv,
			// android.provider.Settings.System.TIME_12_24);
			// if (strTimeFormat.equals("24")) {
			// is24HourView = true;
			// } else {
			// is24HourView = false;
			// }
			return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, is24HourView);
		}
		return null;
	}

	@Override
	protected void onPrepareTwsDialog(int id, TwsDialog dialog) {
		switch (id) {
		case TIME_DIALOG_ID:
			((TimePickerDialog) dialog).updateTime(mHour, mMinute);
			break;
		}
	}

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay();
			updateTimePicker(mHour, mMinute);
		}
	};

	private void updateDisplay() {
		mTimeDisplay.setText(new StringBuilder().append(pad(mHour)).append(":").append(pad(mMinute)));
	}

	private void updateTimePicker(int hour, int minute) {
		mTimePicker.setCurrentHour(hour);
		mTimePicker.setCurrentMinute(minute);
	}

	private static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

	private boolean mIs24Hour = false;

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.pickTime:
			showTwsDialog(TIME_DIALOG_ID);
			break;
		case R.id.to_apply_24_hour:
			mIs24Hour = !mIs24Hour;
			mTimePicker.setIs24HourView(mIs24Hour);
			if (mIs24Hour) {
				mTo24Btn.setText("12小时制");
			} else {
				mTo24Btn.setText("24小时制");
			}
			break;

		default:
			break;
		}
	}
}
