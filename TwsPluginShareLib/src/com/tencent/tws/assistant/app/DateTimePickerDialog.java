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

import java.util.Calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.DateTimePicker;
import com.tencent.tws.assistant.widget.DateTimePicker.OnDateTimeChangedListener;
import com.tencent.tws.sharelib.R;

/**
 * A simple dialog containing an {@link android.widget.DateTimePicker}.
 * 
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener, OnDateTimeChangedListener {

    private static final String TIME = "time";

    private final DateTimePicker mDateTimePicker;
    private final OnDateTimeSetListener mCallBack;
    private final Calendar mCalendar;

    // tws-start title needs update::2014-8-6
    private boolean mTitleNeedsUpdate = false;
    private CharSequence[] mButtons = null;
    // tws-end title needs update::2014-8-6
    private TextView mPositiveButton = null;
    private RelativeLayout mPickerButtons = null;

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateTimeSetListener {

        /**
         * @param view
         *            The view associated with this listener.
         * @param time
         *            The time that was set.
         */
        void onDateTimeSet(DateTimePicker view, long time);
    }

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param time
     *            The initial time of the dialog.
     */
    public DateTimePickerDialog(Context context, OnDateTimeSetListener callBack, long time) {
        this(context, 0, callBack, time);
    }

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param theme
     *            the theme to apply to this dialog
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param time
     *            The initial time of the dialog.
     */
    public DateTimePickerDialog(Context context, int theme, OnDateTimeSetListener callBack, long time) {
        this(context, true, theme, callBack, time);
    }

    public DateTimePickerDialog(Context context, boolean isBottomDialog, int theme, OnDateTimeSetListener callBack, long time) {
        super(context, isBottomDialog);
        
        mCallBack = callBack;
        
        mCalendar = Calendar.getInstance();
        
        String ok = context.getResources().getString(R.string.ok);
        mButtons = new String[]{ok};

        Context themeContext = getContext();
        if (isBottomDialog) {
            setBottomButtons(mButtons, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    tryNotifyDateSet();
                }
            });
        } else {
            setButton(BUTTON_POSITIVE, themeContext.getText(R.string.date_time_set), this);
            setButton(BUTTON_NEGATIVE, themeContext.getText(R.string.cancel), (OnClickListener) null);
        }
        setIcon(0);
        
        LayoutInflater inflater = (LayoutInflater) themeContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.date_time_picker_dialog, null);
        setView(view,0,0,0,0);
        mDateTimePicker = (DateTimePicker) view.findViewById(R.id.dateTimePicker);
        mDateTimePicker.init(time, this);
        mPositiveButton = (TextView) view.findViewById(R.id.picker_dialog_positive);
        mPickerButtons = (RelativeLayout) view.findViewById(R.id.picker_dialog_buttons);
        if (mPositiveButton != null) {
			
			boolean bRipple = ThemeUtils.isShowRipple(themeContext);
			if(bRipple){
				if (android.os.Build.VERSION.SDK_INT > 15) {
					mPositiveButton.setBackground(TwsRippleUtils.getDefaultDrawable(getContext()));
				}
				else {
					mPositiveButton.setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(getContext()));
				}
			}
            mPositiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    tryNotifyDateSet();
                    dismiss();
                }
            });
        }
        //setTitle(R.string.time_picker_dialog_title);
    }

    public void onClick(DialogInterface dialog, int which) {
        tryNotifyDateSet();
    }

    /**
     * Gets the {@link DateTimePicker} contained in this dialog.
     * 
     * @return The calendar view.
     */
    public DateTimePicker getDateTimePicker() {
        return mDateTimePicker;
    }

    /**
     * Sets the current date.
     * 
     * @param year
     *            The date year.
     * @param monthOfYear
     *            The date month.
     * @param dayOfMonth
     *            The date day of month.
     */
    public void updateDate(long time) {
        mDateTimePicker.updateDateTime(time);
    }

    private void tryNotifyDateSet() {
        if (mCallBack != null) {
            mDateTimePicker.clearFocus();
            mCallBack.onDateTimeSet(mDateTimePicker, mDateTimePicker.getTimeInMillis());
        }
    }

    @Override
    protected void onStop() {
//        tryNotifyDateSet();
        super.onStop();
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putLong(TIME, mDateTimePicker.getTimeInMillis());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long time = savedInstanceState.getLong(TIME);
        mDateTimePicker.init(time, this);
    }

    public void onDateTimeChanged(DateTimePicker view, long time) {
        view.init(time, this);
        updateTitle(time);
    }

    private void updateTitle(long time) {
        if (mTitleNeedsUpdate) {
            String title = DateUtils.formatDateTime(mContext, time,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR
                    | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_WEEKDAY);
            setTitle(title);
        }
    }

    public void setTitleNeedsUpdate(boolean needsUpdate) {
        mTitleNeedsUpdate = needsUpdate;
    }

    public boolean getTitleNeedsUpdate() {
        return mTitleNeedsUpdate;
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
