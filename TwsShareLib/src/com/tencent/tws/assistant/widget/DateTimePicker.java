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

package com.tencent.tws.assistant.widget;

//import android.annotation.Widget;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CalendarView;
import android.widget.FrameLayout;

import com.tencent.tws.assistant.utils.ChineseCalendar;
import com.tencent.tws.assistant.widget.NumberPicker.Formatter;
import com.tencent.tws.assistant.widget.NumberPicker.OnValueChangeListener;
import com.tencent.tws.sharelib.R;

/**
 * This class is a widget for selecting a date. The date can be selected by a
 * year, month, and day spinners or a {@link CalendarView}. The set of spinners
 * and the calendar view are automatically synchronized. The client can
 * customize whether only the spinners, or only the calendar view, or both to be
 * displayed. Also the minimal and maximal date from which dates to be selected
 * can be customized.
 * <p>
 * See the <a href="{@docRoot}
 * resources/tutorials/views/hello-datepicker.html">Date Picker tutorial</a>.
 * </p>
 * <p>
 * For a dialog using this view, see {@link android.app.DatePickerDialog}.
 * </p>
 * 
 */
// @Widget
public class DateTimePicker extends FrameLayout {

	private static final String LOG_TAG = DateTimePicker.class.getSimpleName();

	private static final String DATE_FORMAT = "yyyy/MM/dd/HH/mm";

	private static final boolean DEFAULT_ENABLED_STATE = true;

	private final NumberPicker mLunarPicker;

	private final NumberPicker mDayPicker;

	private final NumberPicker mHourPicker;

	private final NumberPicker mMinutePicker;

	private Locale mCurrentLocale;

	private OnDateTimeChangedListener mOnDateTimeChangedListener;

	private final java.text.DateFormat mDateFormat = new SimpleDateFormat(DATE_FORMAT);

	private ChineseCalendar mCurrentDate;
	private static final ThreadLocal<Calendar> sCalCache = new ThreadLocal<Calendar>();
	private DayFormatter mDayFormatter;

	private int mDayLastValue;
	private String[] mDayDisplayValues;

	private int mMinuteInterval = 1;
	private String[] mMinuteDisplayValues;

	private String[] mLunarDisplayValues;
	private boolean mIsLunar = false;

	private String[] mAmPmStrings;
	private boolean mIs24HourView;
	private boolean mIsAm;
	private static final int HOURS_IN_HALF_DAY = 12;

	private boolean mIsEnabled = DEFAULT_ENABLED_STATE;
	private String mDayFormat = "M-d";
	private String mTodayStr = "Today";
	private String mWeekDaysStr[] = new String[] { "", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	private String mHourName = "";
	private String mMinuteName = "";
	private Context mContext;
	private Calendar mMinDate;
	private Calendar mMaxDate;
	private static final int DEFAULT_START_YEAR = 1970;
	private static final int DEFAULT_END_YEAR = 2036;
	private static final int ONE_DAY_TIME = 24 * 60 * 60 * 1000;

	/**
	 * The callback used to indicate the user changes\d the date.
	 */
	public interface OnDateTimeChangedListener {
		/**
		 * Called upon a date change.
		 * @param view The view associated with this listener.
		 * @param time The time that was set.
		 */
		void onDateTimeChanged(DateTimePicker view, long time);
	}

	public DateTimePicker(Context context) {
		this(context, null);
	}

	public DateTimePicker(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.dateTimePickerStyle);
	}

	public DateTimePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mDayFormat = getResources().getString(R.string.datetime_picker_day_format);
		mTodayStr = getResources().getString(R.string.datetime_picker_today_str);
		mWeekDaysStr = getResources().getStringArray(R.array.tws_calendar_weekdays);
		mAmPmStrings = getResources().getStringArray(R.array.tws_calendar_ampm);
		mLunarDisplayValues = getResources().getStringArray(R.array.tws_calendar_type);
		mHourName = getResources().getString(R.string.calendar_hour);
		mMinuteName = getResources().getString(R.string.calendar_mintue);
		mCurrentDate = new ChineseCalendar(mContext);
		mDayFormatter = new DayFormatter(mContext);
		adjustCalendar(mCurrentDate, true);
		Calendar cal = (Calendar) sCalCache.get();
		if (cal == null) {
			cal = Calendar.getInstance();
			sCalCache.set(cal);
		}
		int layoutResourceId = R.layout.date_time_picker;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(layoutResourceId, this, true);

		mLunarPicker = (NumberPicker) findViewById(R.id.datetime_lunar);
		mDayPicker = (NumberPicker) findViewById(R.id.datetime_date);
		mHourPicker = (NumberPicker) findViewById(R.id.datetime_hour);
		mMinutePicker = (NumberPicker) findViewById(R.id.datetime_minute);

		mLunarPicker.setOnValueChangedListener(mValueChangeListener);
		mDayPicker.setOnValueChangedListener(mValueChangeListener);
		mHourPicker.setOnValueChangedListener(mValueChangeListener);
		mMinutePicker.setOnValueChangedListener(mValueChangeListener);

		mMinDate = Calendar.getInstance();
		mMaxDate = Calendar.getInstance();

		updatePickers();

		updateMinOrMaxDate();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (mIsEnabled == enabled) {
			return;
		}
		super.setEnabled(enabled);
		mLunarPicker.setEnabled(enabled);
		mDayPicker.setEnabled(enabled);
		mHourPicker.setEnabled(enabled);
		mMinutePicker.setEnabled(enabled);
		mIsEnabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return mIsEnabled;
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		onPopulateAccessibilityEvent(event);
		return true;
	}

	@Override
	public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
		super.onPopulateAccessibilityEvent(event);

		final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
		String selectedDateUtterance = DateUtils.formatDateTime(mContext, mCurrentDate.getTimeInMillis(), flags);
		event.getText().add(selectedDateUtterance);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setCurrentLocale(newConfig.locale);
	}

	/**
	 * Sets the current locale.
	 * 
	 * @param locale
	 *            The current locale.
	 */
	private void setCurrentLocale(Locale locale) {
		if (locale.equals(mCurrentLocale)) {
			return;
		}
		mCurrentLocale = locale;
		mCurrentDate = new ChineseCalendar(mContext);
		mMinDate = getCalendarForLocale(mMinDate, locale);
		mMaxDate = getCalendarForLocale(mMaxDate, locale);
	}

	/**
	 * Gets a calendar for locale bootstrapped with the value of a given
	 * calendar.
	 * 
	 * @param oldCalendar
	 *            The old calendar.
	 * @param locale
	 *            The locale.
	 */
	private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
		if (oldCalendar == null) {
			return Calendar.getInstance(locale);
		} else {
			final long currentTimeMillis = oldCalendar.getTimeInMillis();
			Calendar newCalendar = Calendar.getInstance(locale);
			newCalendar.setTimeInMillis(currentTimeMillis);
			return newCalendar;
		}
	}

	// Override so we are in complete control of save / restore for this widget.
	@Override
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
		dispatchThawSelfOnly(container);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		return new SavedState(superState, isLunar(), getTimeInMillis());
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		setIsLunar(ss.mIsLunar);
		updateDateTime(ss.mTime);
	}

	/**
	 * Initialize the state. If the provided values designate an inconsistent
	 * date the values are normalized before updating the spinners.
	 * 
	 * @param time
	 *            The initial time.
	 * @param OnDateTimeChangedListener
	 *            How user is notified date is changed by user, can be null.
	 */
	public void init(long time, OnDateTimeChangedListener onDateTimeChangedListener) {
		updateDateTime(time);
		mOnDateTimeChangedListener = onDateTimeChangedListener;
	}

	/**
	 * Parses the given <code>date</code> and in case of success sets the result
	 * to the <code>outDate</code>.
	 * 
	 * @return True if the date was parsed.
	 */
	private boolean parseDate(String date, Calendar outDate) {
		try {
			outDate.setTime(mDateFormat.parse(date));
			return true;
		} catch (ParseException e) {
			Log.w(LOG_TAG, "Date: " + date + " not in format: " + DATE_FORMAT);
			return false;
		}
	}

	private boolean isNewDate(long time) {
		return mCurrentDate.getTimeInMillis() != time;
	}

	private void adjustCalendar(Calendar c, boolean adjustForward) {
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		int reminMinute = c.get(Calendar.MINUTE) % mMinuteInterval;
		if (reminMinute != 0) {
			if (adjustForward) {
				c.add(Calendar.MINUTE, (mMinuteInterval - reminMinute));
			} else {
				c.add(Calendar.MINUTE, -reminMinute);
			}
		}
	}

	private void updateLunarPicker() {
		mLunarPicker.setMinValue(0);
		mLunarPicker.setMaxValue(1);
		mLunarPicker.setDisplayedValues(mLunarDisplayValues);
		if (mIsLunar) {
			mLunarPicker.setValue(ChineseCalendar.CALENDAR_TYPE_LUNAR);
		} else {
			mLunarPicker.setValue(ChineseCalendar.CALENDAR_TYPE_GREGORIAN);
		}
		mLunarPicker.setSlowScroller(true);
	}

	private void updateDayPicker() {
		checkDisplayeValid(mDayPicker, 0, 6);
		boolean isLoop = true;
		long minTime = mCurrentDate.getTimeInMillis() - mMinDate.getTimeInMillis();
		long maxTime = mMaxDate.getTimeInMillis() - mCurrentDate.getTimeInMillis();
		if (minTime >= 0 && minTime < ONE_DAY_TIME * 3.5d) {
			isLoop = false;
		} else if (maxTime >= 0 && maxTime < ONE_DAY_TIME * 3.5d) {
			isLoop = false;
		} else {
			isLoop = true;
		}

		mDayPicker.setMinValue(0);
		mDayPicker.setMaxValue(6);
		mDayPicker.setWrapSelectorWheel(isLoop);
		int count = (mDayPicker.getMaxValue() - mDayPicker.getMinValue()) + 1;
		if ((mDayDisplayValues == null) || (mDayDisplayValues.length != count)) {
			mDayDisplayValues = new String[count];
		}
		int cv = mDayPicker.getValue();
		Calendar cal = (Calendar) sCalCache.get();
		if (cal == null) {
			cal = Calendar.getInstance();
			sCalCache.set(cal);
		}
		cal.setTimeInMillis(mCurrentDate.getTimeInMillis());
		mDayDisplayValues[cv] = formatDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH));
		for (int i = 1; i <= 3; i++) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			int index = (cv + i) % 7;
			if (index < mDayDisplayValues.length) {
				mDayDisplayValues[index] = formatDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
						cal.get(Calendar.DAY_OF_MONTH));
			}
		}
		cal.setTimeInMillis(mCurrentDate.getTimeInMillis());
		for (int i = 1; i <= 3; i++) {
			cal.add(Calendar.DAY_OF_YEAR, -1);
			int index = ((cv - i) + 7) % 7;
			if (index < mDayDisplayValues.length) {
				mDayDisplayValues[index] = formatDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
						cal.get(Calendar.DAY_OF_MONTH));
			}
		}
		mDayPicker.setDisplayedValues(mDayDisplayValues);
	}

	private void updateHourPicker() {
		if (is24HourView()) {
			mHourPicker.setMinValue(0);
			mHourPicker.setMaxValue(23);
		} else {
			mHourPicker.setMinValue(1);
			mHourPicker.setMaxValue(12);
		}
		mHourPicker.setFormatter(mHourFormatter);
		mHourPicker.setWrapSelectorWheel(true);
		setCurrentHour(mCurrentDate.get(Calendar.HOUR_OF_DAY));
	}

	private void updateMinutePicker() {
		checkDisplayeValid(mMinutePicker, 0, ((60 / mMinuteInterval) - 1));
		mMinutePicker.setMinValue(0);
		mMinutePicker.setMaxValue(((60 / mMinuteInterval) - 1));
		mMinutePicker.setWrapSelectorWheel(true);
		int count = (mMinutePicker.getMaxValue() - mMinutePicker.getMinValue()) + 1;
		if ((mMinuteDisplayValues == null) || (mMinuteDisplayValues.length != count)) {
			mMinuteDisplayValues = new String[count];
			for (int i = 0; i < count; i++) {
				mMinuteDisplayValues[i] = (((mMinutePicker.getMinValue() + i) * mMinuteInterval) + mMinuteName);
			}
			mMinutePicker.setDisplayedValues(mMinuteDisplayValues);
		}
		int minuteValue = mCurrentDate.get(Calendar.MINUTE) / mMinuteInterval;
		mMinutePicker.setValue(minuteValue);
	}

	private void updatePickers() {
		updateLunarPicker();
		updateDayPicker();
		updateAmPmLabel();
		updateHourPicker();
		updateMinutePicker();
	}

	private void checkDisplayeValid(NumberPicker picker, int toMin, int toMax) {
		String[] display = picker.getDisplayedValues();
		if ((display != null) && (display.length < ((toMax - toMin) + 1))) {
			picker.setDisplayedValues(null);
		}
	}

	public void setMinuteInterval(int minuteInterval) {
		if (mMinuteInterval == minuteInterval) {
			return;
		}
		mMinuteInterval = minuteInterval;
		adjustCalendar(mCurrentDate, true);
		updateMinutePicker();
	}

	public void updateDateTime(long timeInMillis) {
		mCurrentDate.setTimeInMillis(timeInMillis);
		adjustCalendar(mCurrentDate, true);
		updateMinOrMaxDate();
		updatePickers();
	}

	public void updateDateTime(Time time) {
		mCurrentDate.setTimeZone(TimeZone.getTimeZone(time.timezone));
		updateDateTime(time.toMillis(true));
	}

	public Time getTime() {
		Time t = new Time(mCurrentDate.getTimeZone().getID());
		t.set(mCurrentDate.getTimeInMillis());
		return t;
	}

	public void setIs24HourView(Boolean is24HourView) {
		if (mIs24HourView == is24HourView) {
			return;
		}
		// cache the current hour since spinner range changes and BEFORE
		// changing mIs24HourView!!
		int currentHour = getCurrentHour();
		// Order is important here.
		mIs24HourView = is24HourView;
		updateHourPicker();
		// set value after spinner range is updated - be aware that because
		// mIs24HourView has
		// changed then getCurrentHour() is not equal to the currentHour we
		// cached before so
		// explicitly ask for *not* propagating any onTimeChanged()
		setCurrentHour(currentHour, false /* no onTimeChanged() */);
		updateAmPmLabel();
	}

	/**
	 * @return true if this is in 24 hour view else false.
	 */
	public boolean is24HourView() {
		return mIs24HourView;
	}

	public Integer getCurrentHour() {
		int currentHour = mHourPicker.getValue();
		if (is24HourView()) {
			return currentHour;
		} else if (mIsAm) {
			return currentHour % HOURS_IN_HALF_DAY;
		} else {
			return (currentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
		}
	}

	/**
	 * Set the current hour.
	 */
	public void setCurrentHour(Integer currentHour) {
		setCurrentHour(currentHour, true);
	}

	private void setCurrentHour(Integer currentHour, boolean notifyTimeChanged) {
		// why was Integer used in the first place?
		if (currentHour == null || currentHour == getCurrentHour()) {
			return;
		}
		if (!is24HourView()) {
			// convert [0,23] ordinal to wall clock display
			if (currentHour >= HOURS_IN_HALF_DAY) {
				mIsAm = false;
				if (currentHour > HOURS_IN_HALF_DAY) {
					currentHour = currentHour - HOURS_IN_HALF_DAY;
				}
			} else {
				mIsAm = true;
				if (currentHour == 0) {
					currentHour = HOURS_IN_HALF_DAY;
				}
			}
		}
		mHourPicker.setValue(currentHour);
	}

	private void updateAmPmLabel() {
		if (is24HourView()) {
			mHourPicker.setLabel(null);
		} else {
			int index = mIsAm ? Calendar.AM : Calendar.PM;
			mHourPicker.setLabel(mAmPmStrings[index], NumberPicker.LABEL_LEFT);
		}
	}

	OnValueChangeListener mValueChangeListener = new OnValueChangeListener() {
		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			if (picker == mLunarPicker) {
				if (newVal == ChineseCalendar.CALENDAR_TYPE_LUNAR) {
					setIsLunar(true);
				} else {
					setIsLunar(false);
				}
			} else if (picker == mDayPicker) {
				int increase = (((picker.getValue() - mDayLastValue) + 7) % 7) == 1 ? 1 : -1;
				mCurrentDate.add(Calendar.DAY_OF_YEAR, increase);
				mDayLastValue = picker.getValue();
				updateDayPicker();
			} else if (picker == mHourPicker) {
				if (!is24HourView()) {
					if ((oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY)
							|| (oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1)) {
						mIsAm = !mIsAm;
						updateAmPmLabel();
					}
				}
				mCurrentDate.set(Calendar.HOUR_OF_DAY, getCurrentHour());
			} else if (picker == mMinutePicker) {
				mCurrentDate.set(Calendar.MINUTE, (mMinuteInterval * mMinutePicker.getValue()));
			} else {
				throw new IllegalArgumentException();
			}
			notifyDateChanged();
		}
	};

	private class DayFormatter extends SimpleDateFormat {
		private ChineseCalendar mTempCalendar;
		int currentYear = 1970;
		int currentMonth = 1;
		int currentDay = 1;

		public DayFormatter(Context context) {
			mTempCalendar = new ChineseCalendar(context);
			mTempCalendar.setTimeInMillis(System.currentTimeMillis());
			currentDay = mTempCalendar.get(Calendar.DAY_OF_MONTH);
			currentMonth = mTempCalendar.get(Calendar.MONTH);
			currentYear = mTempCalendar.get(Calendar.YEAR);
			applyLocalizedPattern(mDayFormat);
			DateFormatSymbols symbols = getDateFormatSymbols();
			symbols.setShortWeekdays(mWeekDaysStr);
			setDateFormatSymbols(symbols);
		}

		public String formatDay(int year, int monthOfYear, int dayOfMonth) {
			String time = "";
			mTempCalendar.set(year, monthOfYear, dayOfMonth);
			if (mIsLunar) {
				time = mTempCalendar.getChinese(ChineseCalendar.CHINESE_MONTH)
						+ mTempCalendar.getChinese(ChineseCalendar.CHINESE_DATE);
			} else {
				time = format(mTempCalendar.getTime());
			}
			if (year == currentYear && monthOfYear == currentMonth && dayOfMonth == currentDay) {
				time = mTodayStr;
			}
			return time;
		}
	}

	private String formatDay(int year, int monthOfYear, int dayOfMonth) {
		if (mDayFormatter == null) {
			mDayFormatter = new DayFormatter(mContext);
		}
		return mDayFormatter.formatDay(year, monthOfYear, dayOfMonth);
	}

	public void setIsLunar(boolean isLunar) {
		mIsLunar = isLunar;
		mDayPicker.setDisplayedValues(null);
		updateDayPicker();
		mLunarPicker.setValue(isLunar ? ChineseCalendar.CALENDAR_TYPE_LUNAR : ChineseCalendar.CALENDAR_TYPE_GREGORIAN);
	}

	public boolean isLunar() {
		return mIsLunar;
	}

	public void setLunarSpinnerVisibility(boolean isLunar) {
		mLunarPicker.setVisibility(isLunar ? View.VISIBLE : View.GONE);
		if (!isLunar) {
			mLunarPicker.setValue(ChineseCalendar.CALENDAR_TYPE_GREGORIAN);
			setIsLunar(isLunar);
		}
		invalidate();
	}

	/**
	 * @return The selected year.
	 */
	public int getYear() {
		return mCurrentDate.get(Calendar.YEAR);
	}

	/**
	 * @return The selected month.
	 */
	public int getMonth() {
		return mCurrentDate.get(Calendar.MONTH);
	}

	/**
	 * @return The selected day of day_of_month.
	 */
	public int getDayOfMonth() {
		return mCurrentDate.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * @return The selected day of hour_of_day(0-23).
	 */
	public int getHourOfDay() {
		return mCurrentDate.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * @return The selected day of minute.
	 */
	public int getMinute() {
		return mCurrentDate.get(Calendar.MINUTE);
	}

	/**
	 * @return The selected time in millisecond.
	 */
	public long getTimeInMillis() {
		return mCurrentDate.getTimeInMillis();
	}

	/**
	 * Notifies the listener, if such, for a change in the selected date.
	 */
	private void notifyDateChanged() {
		// sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
		if (mOnDateTimeChangedListener != null) {
			mOnDateTimeChangedListener.onDateTimeChanged(this, getTimeInMillis());
		}
	}

	/**
	 * Class for managing state storing/restoring.
	 */
	private static class SavedState extends BaseSavedState {

		private final boolean mIsLunar;
		private final long mTime;

		/**
		 * Constructor called from {@link DateTimePicker#onSaveInstanceState()}
		 */
		private SavedState(Parcelable superState, boolean isLunar, long time) {
			super(superState);
			mIsLunar = isLunar;
			mTime = time;
		}

		/**
		 * Constructor called from {@link #CREATOR}
		 */
		private SavedState(Parcel in) {
			super(in);
			mIsLunar = (Boolean) in.readValue(null);
			mTime = in.readLong();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeLong(mTime);
		}

		@SuppressWarnings("all")
		// suppress unused and hiding
		public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	Formatter mHourFormatter = new NumberPicker.Formatter() {
		@Override
		public String format(int value) {
			return value + mHourName;
		}
	};

	public long getMinDate() {
		return mMinDate.getTimeInMillis();
	}

	public void setMinDate(long minDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(minDate);
		if (cal.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
				&& cal.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
			return;
		}
		mMinDate.setTimeInMillis(minDate);
		if (mCurrentDate.before(mMinDate)) {
			mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
		}
		long minTime = mCurrentDate.getTimeInMillis() - mMinDate.getTimeInMillis();
		if (minTime >= 0) {
			int day = (int) Math.floor(minTime / ONE_DAY_TIME);
			int cv = day % 7;
			mDayLastValue = cv;
			mDayPicker.setValue(cv);
		}
		updatePickers();
	}

	public long getMaxDate() {
		return mMaxDate.getTimeInMillis();
	}

	public void setMaxDate(long maxDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(maxDate);
		if (cal.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
				&& cal.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
			return;
		}
		mMaxDate.setTimeInMillis(maxDate);
		if (mCurrentDate.after(mMaxDate)) {
			mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
		}
		long maxTime = mMaxDate.getTimeInMillis() - mCurrentDate.getTimeInMillis();
		if (maxTime >= 0) {
			int day = (int) Math.floor(maxTime / ONE_DAY_TIME);
			int cv = 6 - day % 7;
			mDayLastValue = cv;
			mDayPicker.setValue(cv);
		}
		updatePickers();
	}

	private void updateMinOrMaxDate() {
		Calendar tempCalendar = Calendar.getInstance();
		mMinDate.set(DEFAULT_START_YEAR, 0, 1, 0, 0, 0);
		mMinDate.set(Calendar.MILLISECOND, 0);
		mMaxDate.set(DEFAULT_END_YEAR, 11, 31, 23, 59, 59);
		mMaxDate.set(Calendar.MILLISECOND, 0);
		long diffStartTime = Math.abs(mCurrentDate.getTimeInMillis() - mMinDate.getTimeInMillis());
		long diffEndTime = Math.abs(mMaxDate.getTimeInMillis() - mCurrentDate.getTimeInMillis());
		if (diffStartTime >= diffEndTime) {
			tempCalendar.clear();
			tempCalendar.setTimeInMillis(mMaxDate.getTimeInMillis());
			setMaxDate(tempCalendar.getTimeInMillis());
		} else {
			tempCalendar.clear();
			tempCalendar.setTimeInMillis(mMinDate.getTimeInMillis());
			setMinDate(tempCalendar.getTimeInMillis());
		}
	}
}
