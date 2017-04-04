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

package com.tencent.tws.assistant.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.TimePicker;
import com.tencent.tws.assistant.widget.TimePicker.OnTimeChangedListener;
import com.tencent.tws.sharelib.R;

/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}
 * .
 * 
 * <p>
 * See the <a href="{@docRoot}
 * resources/tutorials/views/hello-timepicker.html">Time Picker tutorial</a>.
 * </p>
 */
public class TimePickerDialog extends AlertDialog implements OnClickListener, OnTimeChangedListener {

	/**
	 * The callback interface used to indicate the user is done filling in the
	 * time (they clicked on the 'Set' button).
	 */
	public interface OnTimeSetListener {

		/**
		 * @param view
		 *            The view associated with this listener.
		 * @param hourOfDay
		 *            The hour that was set.
		 * @param minute
		 *            The minute that was set.
		 */
		void onTimeSet(TimePicker view, int hourOfDay, int minute);
	}

	private static final String HOUR = "hour";
	private static final String MINUTE = "minute";
	private static final String IS_24_HOUR = "is24hour";

	private final TimePicker mTimePicker;
	private final OnTimeSetListener mCallback;

	int mInitialHourOfDay;
	int mInitialMinute;
	boolean mIs24HourView;
	// tws-start bottom dialog::2015-11-04
	private CharSequence[] mButtons = null;
	private TextView mPositiveButton = null;
	private RelativeLayout mPickerButtons = null;

	// tws-start bottom dialog::2015-11-04
	/**
	 * @param context
	 *            Parent.
	 * @param callBack
	 *            How parent is notified.
	 * @param hourOfDay
	 *            The initial hour.
	 * @param minute
	 *            The initial minute.
	 * @param is24HourView
	 *            Whether this is a 24 hour view, or AM/PM.
	 */
	public TimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
		this(context, 0, callBack, hourOfDay, minute, is24HourView);
	}

	public TimePickerDialog(Context context, int theme, OnTimeSetListener callBack, int hourOfDay, int minute,
			boolean is24HourView) {
		this(context, true, 0, callBack, hourOfDay, minute, is24HourView);

	}

	/**
	 * @param context
	 *            Parent.
	 * @param theme
	 *            the theme to apply to this dialog
	 * @param callBack
	 *            How parent is notified.
	 * @param hourOfDay
	 *            The initial hour.
	 * @param minute
	 *            The initial minute.
	 * @param is24HourView
	 *            Whether this is a 24 hour view, or AM/PM.
	 */
	public TimePickerDialog(Context context, boolean isBottomDialog, int theme, OnTimeSetListener callBack,
			int hourOfDay, int minute, boolean is24HourView) {
		// super(context, theme);
		super(context, isBottomDialog);
		mCallback = callBack;
		mInitialHourOfDay = hourOfDay;
		mInitialMinute = minute;
		mIs24HourView = is24HourView;

		setIcon(0);
		// setTitle(R.string.time_picker_dialog_title);

		String ok = context.getResources().getString(R.string.ok);
		mButtons = new String[] { ok };

		Context themeContext = getContext();
		// Log.i("TimePickerDialog", "isBottomDialog = " + isBottomDialog);
		if (isBottomDialog) {
			setBottomButtons(mButtons, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					tryNotifyTimeSet();
				}
			});
		} else {
			setButton(BUTTON_POSITIVE, themeContext.getText(R.string.date_time_set), this);
			setButton(BUTTON_NEGATIVE, themeContext.getText(R.string.cancel), (OnClickListener) null);
		}

		LayoutInflater inflater = (LayoutInflater) themeContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.time_picker_dialog, null);
		setView(view, 0, 0, 0, 0);
		mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
		mPickerButtons = (RelativeLayout) view.findViewById(R.id.picker_dialog_buttons);
		mPositiveButton = (TextView) view.findViewById(R.id.picker_dialog_positive);
		if (mPositiveButton != null) {
			boolean bRipple = ThemeUtils.isShowRipple(themeContext);
			if (bRipple) {
				if (android.os.Build.VERSION.SDK_INT > 15) {
					mPositiveButton.setBackground(TwsRippleUtils.getDefaultDrawable(getContext()));
				} else {
					mPositiveButton.setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(getContext()));
				}
			}
			mPositiveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					tryNotifyTimeSet();
					dismiss();
				}
			});
		}

		// initialize state
		mTimePicker.setIs24HourView(mIs24HourView);
		mTimePicker.setCurrentHour(mInitialHourOfDay);
		mTimePicker.setCurrentMinute(mInitialMinute);
		mTimePicker.setOnTimeChangedListener(this);
	}

	public void onClick(DialogInterface dialog, int which) {
		tryNotifyTimeSet();
	}

	public void updateTime(int hourOfDay, int minutOfHour) {
		mTimePicker.setCurrentHour(hourOfDay);
		mTimePicker.setCurrentMinute(minutOfHour);
	}

	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		/* do nothing */
	}

	private void tryNotifyTimeSet() {
		if (mCallback != null) {
			mTimePicker.clearFocus();
			mCallback.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
		}
	}

	@Override
	protected void onStop() {
		// tryNotifyTimeSet();
		super.onStop();
	}

	@Override
	public Bundle onSaveInstanceState() {
		Bundle state = super.onSaveInstanceState();
		state.putInt(HOUR, mTimePicker.getCurrentHour());
		state.putInt(MINUTE, mTimePicker.getCurrentMinute());
		state.putBoolean(IS_24_HOUR, mTimePicker.is24HourView());
		return state;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int hour = savedInstanceState.getInt(HOUR);
		int minute = savedInstanceState.getInt(MINUTE);
		mTimePicker.setIs24HourView(savedInstanceState.getBoolean(IS_24_HOUR));
		mTimePicker.setCurrentHour(hour);
		mTimePicker.setCurrentMinute(minute);
	}

	public void setPickerPositiveVisibility(boolean isVisible) {
		mPickerButtons.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}

	public void setPickerPositiveBackground(int background) {
		if (background > 0) {
			setPickerPositiveBackground(getContext().getResources().getDrawable(background));
		}
	}

	public void setPickerPositiveBackground(Drawable background) {
		if (android.os.Build.VERSION.SDK_INT > 15) {
			mPickerButtons.setBackground(background);
		} else {
			mPickerButtons.setBackgroundDrawable(background);
		}
	}

	public View getPickerPositiveButton() {
		return mPositiveButton;
	}
}
