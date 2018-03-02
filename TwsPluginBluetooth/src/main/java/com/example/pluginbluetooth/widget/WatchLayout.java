package com.example.pluginbluetooth.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;


import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.app.animation.WatchHandsAnimation;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class WatchLayout extends FrameLayout {

	private static final String TAG = "WatchLayout";
	// 表盘
	private ImageView mImageViewMeter;
	// 分针
	protected ImageView mImageViewWatchHandMinutes;
	// 时针
	protected ImageView mImageViewWatchHandHours;
	// 最顶部的遮罩，当前暂时不用
	protected View mImageViewGlass;

	private WatchHandsAnimation.WatchHandModel mWatchHandModel;
	private WatchHandsAnimation mWatchHandsAnimation;

	protected float mScaleXY;

	private boolean mScalingEnabled = false;

	private final Context mContext;
//	protected int mPointOffsetY = 0;

	public WatchLayout(Context context) {
		super(context);
		mContext = context;
		init(null);
	}

	public WatchLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init(attrs);
	}

	public WatchLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		boolean needPadding = false;
		if (attrs != null) {
//			TypedArray a = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.WatchLayout, 0, 0);
//			try {
//				mScalingEnabled = a.getBoolean(R.styleable.WatchLayout_scalingEnabled, mScalingEnabled);
//				needPadding = a.getBoolean(R.styleable.WatchLayout_needPointerPaddingTop, false);
//
//			} finally {
//				a.recycle();
//			}
		}

		if (needPadding) {
//			mPointOffsetY = getResources().getDimensionPixelOffset(R.dimen.complication_dial_pointer_margin_top);
		}
		Log.i(TAG, "mScalingEnabled is " + mScalingEnabled);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mImageViewMeter = (ImageView) findViewById(R.id.imageViewMeter);
		mImageViewWatchHandMinutes = (ImageView) findViewById(R.id.imageViewWatchHandMinutes);
		mImageViewWatchHandHours = (ImageView) findViewById(R.id.imageViewWatchHandHours);

		// 暂时不用这个遮罩
		mImageViewGlass = findViewById(R.id.imageViewWatchGlass);
		if (null != mImageViewGlass) {
			mImageViewGlass.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (!mScalingEnabled) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}
		int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
		int horizontalPadding = getPaddingRight() + getPaddingLeft();
		int verticalPadding = getPaddingTop() + getPaddingBottom();

		int contentWidth = widthMeasureSize - horizontalPadding;

		final int specUnspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		mImageViewMeter.measure(specUnspecified, specUnspecified);
		final int imageViewMeterWidth = mImageViewMeter.getMeasuredWidth();
		final int imageViewMeterHeight = mImageViewMeter.getMeasuredHeight();

		float aspectRatio = (float) imageViewMeterWidth / (float) imageViewMeterHeight;
		mScaleXY = (float) imageViewMeterWidth / (float) widthMeasureSize;

		int imageViewMeterFinalWidth = contentWidth;
		int imageViewMeterFinalHeight = (int) (contentWidth / aspectRatio);

		mImageViewMeter.measure(MeasureSpec.makeMeasureSpec(imageViewMeterFinalWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(imageViewMeterFinalHeight, MeasureSpec.EXACTLY));

		if (mImageViewGlass != null) {
			mImageViewGlass.measure(MeasureSpec.makeMeasureSpec(imageViewMeterFinalWidth, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(imageViewMeterFinalHeight, MeasureSpec.EXACTLY));
		}

		mImageViewWatchHandMinutes.measure(specUnspecified, specUnspecified);
		int imageViewHandWidth = mImageViewWatchHandMinutes.getMeasuredWidth();
		int imageViewHandHeight = mImageViewWatchHandMinutes.getMeasuredHeight();

		float imageViewWatchHandWidthScaled = imageViewHandWidth / mScaleXY;
		float imageViewWatchHandHeightScaled = imageViewHandHeight / mScaleXY;

		mImageViewWatchHandMinutes.measure(
				MeasureSpec.makeMeasureSpec((int) (imageViewWatchHandWidthScaled), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec((int) (imageViewWatchHandHeightScaled), MeasureSpec.EXACTLY));

		mImageViewWatchHandHours.measure(specUnspecified, specUnspecified);
		int imageViewHandHoursWidth = mImageViewWatchHandHours.getMeasuredWidth();
		int imageViewHandHoursHeight = mImageViewWatchHandHours.getMeasuredHeight();

		float imageViewWatchHandHoursWidthScaled = imageViewHandHoursWidth / mScaleXY;
		float imageViewWatchHandHoursHeightScaled = imageViewHandHoursHeight / mScaleXY;

		mImageViewWatchHandHours.measure(
				MeasureSpec.makeMeasureSpec((int) (imageViewWatchHandHoursWidthScaled), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec((int) (imageViewWatchHandHoursHeightScaled), MeasureSpec.EXACTLY));

		setMeasuredDimension(imageViewMeterFinalWidth + horizontalPadding, imageViewMeterFinalHeight + verticalPadding);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int horizontalPadding = getPaddingRight() + getPaddingLeft();
		int verticalPadding = getPaddingTop() + getPaddingBottom();
		int contentWidth = right - left - horizontalPadding;
		int contentHeight = getMeasuredHeight() - verticalPadding;

		final int leftValue = getPaddingLeft();
		final int topValue = getPaddingTop();
		final int rightValue = leftValue + contentWidth;
		final int bottomValue = topValue + contentHeight;
		mImageViewMeter.layout(leftValue, topValue, rightValue, bottomValue);

		if (mImageViewGlass != null) {
			mImageViewGlass.layout(leftValue, topValue, rightValue, bottomValue);
		}

		int imageViewHandWidth = mImageViewWatchHandMinutes.getMeasuredWidth();
		int imageViewHandHeight = mImageViewWatchHandMinutes.getMeasuredHeight();

		int lwhm = getPaddingLeft() + (contentWidth - imageViewHandWidth) / 2;
		int twhm = getPaddingTop()/* + mPointOffsetY*/;
		int rwhm = lwhm + imageViewHandWidth;
		int bwhm = twhm + imageViewHandHeight;

		mImageViewWatchHandMinutes.layout(lwhm, twhm, rwhm, bwhm);

		mImageViewWatchHandMinutes.setPivotX(imageViewHandWidth / 2f);
		mImageViewWatchHandMinutes.setPivotY(imageViewHandHeight - imageViewHandWidth / 2f);

		int imageViewHandHoursWidth = mImageViewWatchHandHours.getMeasuredWidth();
		int imageViewHandHoursHeight = mImageViewWatchHandHours.getMeasuredHeight();

		int lwhh = getPaddingLeft() + (contentWidth - imageViewHandHoursWidth) / 2;
		int twhh = getPaddingTop()/* + mPointOffsetY*/;
		int rwhh = lwhh + imageViewHandHoursWidth;
		int bwhh = twhh + imageViewHandHoursHeight;

		mImageViewWatchHandHours.layout(lwhh, twhh, rwhh, bwhh);

		mImageViewWatchHandHours.setPivotX(imageViewHandHoursWidth / 2f);
		mImageViewWatchHandHours.setPivotY(imageViewHandHoursHeight - imageViewHandHoursWidth / 2f);

	}

	public void setWatchHandModel(WatchHandsAnimation.WatchHandModel watchHandModel) {
		mWatchHandModel = watchHandModel;
		if (mWatchHandsAnimation != null) {
			mWatchHandsAnimation.setWatchHandModel(mWatchHandModel);
		}
	}

	public void setComplicationWatchHandModel(WatchHandsAnimation.WatchHandModel complicationWatchHandModel) {
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mWatchHandsAnimation == null) {
			updateHands(true);
		}
	}

	public void updateHands(boolean animate) {
		if (mWatchHandModel != null) {
			if (mWatchHandsAnimation == null) {
				mWatchHandsAnimation = new WatchHandsAnimation(mImageViewWatchHandMinutes, mImageViewWatchHandHours,
						mWatchHandModel);
			}
			mWatchHandsAnimation.update(animate);
		}
	}

	public void setScalingEnabled(boolean scalingEnabled) {
		mScalingEnabled = scalingEnabled;
	}

	public static WatchHandsAnimation.WatchHandModel createDefaultWatchHandModel() {

		return new WatchHandsAnimation.WatchHandModel() {
			@Override
			public float getMinutesInDegrees() {
				final Calendar calendar = new GregorianCalendar();
				calendar.setTimeInMillis(System.currentTimeMillis());
				return (calendar.get(Calendar.MINUTE) / 60f) * 360;
			}

			@Override
			public float getHoursInDegrees() {
				final Calendar calendar = new GregorianCalendar();
				calendar.setTimeInMillis(System.currentTimeMillis());
				return ((calendar.get(Calendar.HOUR_OF_DAY) % 12 + calendar.get(Calendar.MINUTE) / 60f) / 12f) * 360;
			}
		};
	}
}