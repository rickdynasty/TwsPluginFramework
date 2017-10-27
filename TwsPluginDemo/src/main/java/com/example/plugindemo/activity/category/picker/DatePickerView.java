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

import android.app.Activity;
import android.app.TwsActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.DatePickerDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.DatePicker;
import com.tencent.tws.assistant.widget.DatePicker.OnDateChangedListener;

/**
 * Basic example of using date and time widgets, including
 * {@link android.app.TimePickerDialog} and {@link android.widget.DatePicker}.
 * 
 * Also provides a good example of using {@link Activity#onCreateDialog},
 * {@link Activity#onPrepareDialog} and {@link Activity#showDialog} to have the
 * activity automatically save and restore the state of the dialogs.
 */
public class DatePickerView extends TwsActivity implements OnClickListener {

	// where we display the selected date and time
	private TextView mDateDisplay;
	private DatePicker mDatePicker;
	// date
	private int mYear;
	private int mMonth;
	private int mDay;

	static final int DATE_DIALOG_ID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("DatePickerView");

		setContentView(R.layout.numberpicker_date_example);

		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);

		findViewById(R.id.pickDate).setOnClickListener(this);
		Button show_lunar = (Button) findViewById(R.id.show_lunar);
		show_lunar.setOnClickListener(this);
		Button show_unit = (Button) findViewById(R.id.show_unit);
		show_unit.setOnClickListener(this);
		show_lunar.setText("显示农历&公历");
		show_unit.setText("显示Unit");

		mDatePicker = (DatePicker) findViewById(R.id.datePicker);
		Calendar calendar = Calendar.getInstance();
		mDatePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH), new OnDateChangedListener() {
					@Override
					public void onDateChanged(DatePicker arg0, int year, int month, int day) {
						mYear = year;
						mMonth = month;
						mDay = day;
						updateDisplay();
					}
				});

		updateDisplay();
	}

	private boolean show_lunar = false, show_unit = false;

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.pickDate:
			showTwsDialog(DATE_DIALOG_ID);
			break;
		case R.id.show_lunar:
			show_lunar = !show_lunar;
			mDatePicker.setLunarShown(show_lunar);
			if (show_lunar) {
				((Button) view).setText("隐藏农历&公历");
			} else {
				((Button) view).setText("显示农历&公历");
			}
			break;
		case R.id.show_unit:
			show_unit = !show_unit;
			mDatePicker.setUnitShown(show_unit);
			if (show_unit) {
				((Button) view).setText("隐藏Unit");
			} else {
				((Button) view).setText("显示Unit");
			}
			break;
		}
	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
		}
		return null;
	}

	@Override
	protected void onPrepareTwsDialog(int id, TwsDialog dialog) {
		switch (id) {
		case DATE_DIALOG_ID:
			((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
			break;
		}
	}

	private void updateDisplay() {
		mDateDisplay.setText(new StringBuilder().append(mMonth + 1).append("-").append(mDay).append("-").append(mYear)
				.append(" "));
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			updateDisplay();
		}
	};
}
