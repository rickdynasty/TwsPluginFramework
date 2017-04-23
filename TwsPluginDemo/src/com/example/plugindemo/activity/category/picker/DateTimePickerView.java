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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.TwsActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.DateTimePickerDialog;
import com.tencent.tws.assistant.app.DateTimePickerDialog.OnDateTimeSetListener;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.DateTimePicker;
import com.tencent.tws.assistant.widget.DateTimePicker.OnDateTimeChangedListener;

/**
 * Basic example of using date and time widgets, including
 * {@link android.app.TimePickerDialog} and {@link android.widget.DatePicker}.
 * 
 * Also provides a good example of using {@link Activity#onCreateDialog},
 * {@link Activity#onPrepareDialog} and {@link Activity#showDialog} to have the
 * activity automatically save and restore the state of the dialogs.
 */
public class DateTimePickerView extends TwsActivity {

	// where we display the selected date and time
	private TextView mDateDisplay;

	private long mTime;

	static final int DATE_DIALOG_ID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.numberpicker_datetime_example);
		setTitle("DateTimePickerView");

		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		mTime = c.getTimeInMillis();
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);

		Button pickDateTime = (Button) findViewById(R.id.pickDateTime);
		pickDateTime.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// showTwsDialog(DATE_DIALOG_ID);
				DateTimePickerDialog dialog = new DateTimePickerDialog(DateTimePickerView.this, mDateTimeSetListener,
						mTime);
				dialog.setDialogDimAmount(0.0f);
				dialog.setBottomButtonVisible(false);
				DateTimePicker picker = dialog.getDateTimePicker();
				picker.init(mTime, new OnDateTimeChangedListener() {
					@Override
					public void onDateTimeChanged(DateTimePicker arg0, long time) {
						mTime = time;
						updateDisplay();
					}
				});
				picker.setIsLunar(true);
				dialog.setPickerPositiveVisibility(true);
				dialog.show();
			}
		});
		DateTimePicker dateTimePicker = (DateTimePicker) findViewById(R.id.dateTimePicker);
		dateTimePicker.init(mTime, new OnDateTimeChangedListener() {
			@Override
			public void onDateTimeChanged(DateTimePicker dateTimePicker, long time) {
				mTime = time;
				updateDisplay();
			}
		});
	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			// return new DateTimePickerDialog(this, mDateTimeSetListener,
			// mTime);
			DateTimePickerDialog dialog = new DateTimePickerDialog(this, mDateTimeSetListener, mTime);
			dialog.setDialogDimAmount(0.0f);
			// dialog.setBottomButtonVisible(false);
			DateTimePicker picker = dialog.getDateTimePicker();
			picker.init(mTime, new OnDateTimeChangedListener() {
				@Override
				public void onDateTimeChanged(DateTimePicker arg0, long time) {
					mTime = time;
					updateDisplay();
				}
			});
			return dialog;
		}
		return null;
	}

	@Override
	protected void onPrepareTwsDialog(int id, TwsDialog dialog) {
		switch (id) {
		case DATE_DIALOG_ID:
			((DateTimePickerDialog) dialog).updateDate(mTime);
			break;
		}
	}

	private void updateDisplay() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String text = format.format(mTime);
		mDateDisplay.setText(text);
	}

	private OnDateTimeSetListener mDateTimeSetListener = new OnDateTimeSetListener() {

		public void onDateTimeSet(DateTimePicker view, long time) {
			mTime = time;
			updateDisplay();
		}
	};
}
