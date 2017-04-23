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

import android.app.TwsActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.NumberPicker.Formatter;
import com.tencent.tws.assistant.widget.ProfileDataPicker;
import com.tencent.tws.assistant.widget.ProfileDataPicker.OnDateChangedListener;
import com.tencent.tws.assistant.widget.ProfileDataPicker.ProfileDataStruct;

public class ProfileDataPickerView extends TwsActivity {

	// where we display the selected date and time
	private TextView mNumberDisplay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_profiledatapicker_view);
		setTitle("ProfileDataPickerView");

		mNumberDisplay = (TextView) findViewById(R.id.numberDisplay);

		ProfileDataStruct majorWeight = new ProfileDataStruct(15, 99, 45, null);
		ProfileDataStruct minorWeight = new ProfileDataStruct(0, 9, 0, new Formatter() {
			@Override
			public String format(int value) {
				return "." + value;
			}
		});
		ProfileDataPicker picker = (ProfileDataPicker) findViewById(R.id.profile_data_picker);
		picker.init(majorWeight, minorWeight, new OnDateChangedListener() {
			@Override
			public void onDateChanged(ProfileDataPicker view, int majorValue, int minorValue) {
				mNumberDisplay.setText(majorValue + "." + minorValue + " 公斤");
			}
		});
	}

}
