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
import com.tencent.tws.assistant.widget.DatePicker;
import com.tencent.tws.assistant.widget.DatePicker.OnDateChangedListener;
import com.tencent.tws.sharelib.R;

/**
 * A simple dialog containing an {@link android.widget.DatePicker}.
 * 
 * <p>
 * See the <a href="{@docRoot}resources/tutorials/views/hello-datepicker.html">Date Picker tutorial</a>.
 * </p>
 */
public class DatePickerDialog extends AlertDialog implements OnClickListener, OnDateChangedListener {

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";

    private final DatePicker mDatePicker;
    private final OnDateSetListener mCallBack;
    private final Calendar mCalendar;

    // tws-start title needs update::2014-8-6
    private boolean mTitleNeedsUpdate = false;
    private CharSequence[] mButtons = null;
    private TextView mPositiveButton = null;
    private RelativeLayout mPickerButtons = null;
    // tws-end title needs update::2014-8-6

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {

        /**
         * @param view
         *            The view associated with this listener.
         * @param year
         *            The year that was set.
         * @param monthOfYear
         *            The month that was set (0-11) for compatibility with {@link java.util.Calendar}.
         * @param dayOfMonth
         *            The day of the month that was set.
         */
        void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param year
     *            The initial year of the dialog.
     * @param monthOfYear
     *            The initial month of the dialog.
     * @param dayOfMonth
     *            The initial day of the dialog.
     */
    public DatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear,
            int dayOfMonth) {
        this(context, 0, callBack, year, monthOfYear, dayOfMonth);
    }

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param theme
     *            the theme to apply to this dialog
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param year
     *            The initial year of the dialog.
     * @param monthOfYear
     *            The initial month of the dialog.
     * @param dayOfMonth
     *            The initial day of the dialog.
     */
    public DatePickerDialog(Context context, int theme, OnDateSetListener callBack, int year,
            int monthOfYear, int dayOfMonth) {
        this(context, true, theme, callBack, year, monthOfYear, dayOfMonth);
    }

    public DatePickerDialog(Context context, boolean isBottomDialog, int theme, OnDateSetListener callBack,
            int year, int monthOfYear, int dayOfMonth) {
//        super(context, theme, false, isBottomDialog);
        super(context, isBottomDialog);
        mCallBack = callBack;

        mCalendar = Calendar.getInstance();
        String ok = context.getResources().getString(R.string.ok);
        mButtons = new String[] {ok};

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
        View view = inflater.inflate(R.layout.date_picker_dialog, null);
        setView(view, 0, 0, 0, 0);
        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
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
                    tryNotifyDateSet();
                    dismiss();
                }
            });
        }
        //setTitle(R.string.date_picker_dialog_title);
    }

    public void onClick(DialogInterface dialog, int which) {
        tryNotifyDateSet();
    }

    public void onDateChanged(DatePicker view, int year, int month, int day) {
        mDatePicker.init(year, month, day, this);
        updateTitle(year, month, day);
    }

    /**
     * Gets the {@link DatePicker} contained in this dialog.
     * 
     * @return The calendar view.
     */
    public DatePicker getDatePicker() {
        return mDatePicker;
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
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    private void tryNotifyDateSet() {
        if (mCallBack != null) {
            mDatePicker.clearFocus();
            mCallBack.onDateSet(mDatePicker, mDatePicker.getYear(), mDatePicker.getMonth(),
                    mDatePicker.getDayOfMonth());
        }
    }

    @Override
    protected void onStop() {
//        tryNotifyDateSet();
        super.onStop();
    }

    private void updateTitle(int year, int month, int day) {
        // tws-start title needs update::2014-8-6
        // if (!mDatePicker.getCalendarViewShown()) {
        if (mTitleNeedsUpdate) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            String title = DateUtils.formatDateTime(mContext, mCalendar.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR
                            | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_WEEKDAY);
            setTitle(title);
            mTitleNeedsUpdate = true;
        }
        // tws-end title needs update::2014-8-6
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(YEAR, mDatePicker.getYear());
        state.putInt(MONTH, mDatePicker.getMonth());
        state.putInt(DAY, mDatePicker.getDayOfMonth());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int year = savedInstanceState.getInt(YEAR);
        int month = savedInstanceState.getInt(MONTH);
        int day = savedInstanceState.getInt(DAY);
        mDatePicker.init(year, month, day, this);
    }

    // tws-start title needs update::2014-8-6
    public void setTitleNeedsUpdate(boolean needsUpdate) {
        mTitleNeedsUpdate = needsUpdate;
    }

    public boolean getTitleNeedsUpdate() {
        return mTitleNeedsUpdate;
    }
    // tws-end title needs update::2014-8-6

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
