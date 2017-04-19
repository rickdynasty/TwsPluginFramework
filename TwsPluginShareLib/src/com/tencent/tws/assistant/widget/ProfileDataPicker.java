package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.tencent.tws.assistant.widget.NumberPicker.Formatter;
import com.tencent.tws.assistant.widget.NumberPicker.OnValueChangeListener;
import com.tencent.tws.sharelib.R;

public class ProfileDataPicker extends LinearLayout implements OnValueChangeListener {
	protected static final String TAG = "ProfileDataPicker";
	private final NumberPicker mMajor;
	private final NumberPicker mMinor;

	private OnDateChangedListener mOnDateChangedListener;

	public interface OnDateChangedListener {
		void onDateChanged(ProfileDataPicker view, int majorValue, int minorValue);
	}

	public ProfileDataPicker(Context context) {
		this(context, null);
	}

	public ProfileDataPicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ProfileDataPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOrientation(LinearLayout.HORIZONTAL);

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER_HORIZONTAL;
		setLayoutParams(lp);

		// 主要的
		mMajor = new NumberPicker(context);
		mMajor.setTextAlignType(NumberPicker.ALIGN_RIGHT_TYPE);
		LayoutParams mMajorLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mMajor.setLayoutParams(mMajorLP);
		mMajor.setFocusable(true);
		mMajor.setFocusableInTouchMode(true);
		mMajor.setMinValue(15);
		mMajor.setMaxValue(99);
		mMajor.setWrapSelectorWheel(true);
		mMajor.setValue(45);
		mMajor.setOnValueChangedListener(this);
		addView(mMajor, 0);

		// 次要的
		mMinor = new NumberPicker(context);
		mMinor.setTextAlignType(NumberPicker.ALIGN_LEFT_TYPE);
		LayoutParams mMinorLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mMinorLP.leftMargin = getResources().getDimensionPixelSize(R.dimen.profile_picker_child_margin_left);
		
		mMinor.setLayoutParams(mMinorLP);
		mMinor.setFocusable(true);
		mMinor.setFocusableInTouchMode(true);
		mMinor.setMinValue(0);
		mMinor.setMaxValue(9);
		mMinor.setWrapSelectorWheel(true);
		mMinor.setValue(0);
		mMinor.setOnValueChangedListener(this);
		addView(mMinor, 1);

		setDefautlMinorFormatter();
	}

	public void init(OnDateChangedListener onDateChangedListener) {
		mOnDateChangedListener = onDateChangedListener;
	}

	public void init(ProfileDataStruct major, ProfileDataStruct minor, OnDateChangedListener onDateChangedListener) {
		if (null != major) {
			mMajor.setMinValue(major.minValue);
			mMajor.setMaxValue(major.maxValue);
			mMajor.setValue(major.value);
			mMajor.setFormatter(major.formatter);
		}

		if (null != minor) {
			mMinor.setMinValue(minor.minValue);
			mMinor.setMaxValue(minor.maxValue);
			mMinor.setValue(minor.value);
			mMinor.setFormatter(minor.formatter);
		}

		mOnDateChangedListener = onDateChangedListener;
		notifyDateChanged();
	}

	public void setDefautlMinorFormatter() {
		mMinor.setFormatter(mMinorFormatter);
	}

	Formatter mMinorFormatter = new Formatter() {
		@Override
		public String format(int value) {
			return "." + value;
		}
	};

	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		Log.d(TAG, "oldVal = " + oldVal + " newVal=" + newVal);
		if (picker == mMajor) {
			mMajor.setValue(newVal);
		} else if (picker == mMinor) {
			mMinor.setValue(newVal);
		}

		notifyDateChanged();
	}

	private void notifyDateChanged() {
		if (mOnDateChangedListener != null) {
			mOnDateChangedListener.onDateChanged(this, mMajor.getValue(), mMinor.getValue());
		}
	}

	public static class ProfileDataStruct {
		public int minValue;
		public int maxValue;
		public int value;
		public Formatter formatter = null;

		public ProfileDataStruct(int minValue, int maxValue, int value, Formatter formatter) {
			this.minValue = minValue;
			if (maxValue <= minValue) {
				this.maxValue = minValue + 1;
			} else {
				this.maxValue = maxValue;
			}

			if (value < minValue || maxValue < value) {
				this.value = minValue;
			} else {
				this.value = value;
			}

			this.formatter = formatter;
		}
	}
}
