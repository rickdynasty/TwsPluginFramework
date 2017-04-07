package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tencent.tws.sharelib.R;

public class TwsButton extends FrameLayout {

	private Context mContext;
	private int mButtomMode = -1;
	public static final int NormalButton = 0;
	public static final int RecommendedButton = 1;
	public static final int ProgressButton = 2;
	private boolean isProgressButton = false;

	private int mBackground;
	private int mFocusBackground;
	private int mPressedBackground;
	private int mDisabledBackground;

	private int mBorderColor;
	private int mFocusedBorderColor;
	private int mPressedBorderColor;
	private int mDisabledBorderColor;

	private int mBorderWidth;

	private int mProgressColor;
	private int mProgressBorderColor;
	private int mProgressBorderWith;
	private int mProgressMax = 100;

	private String mText;
	private int mHeight;
	private int mRadius;

	private int mTextSize;
	private int mTextColor;
	private int mPressedTextColor;
	private int mDisableTextColor;

	// # components
	private TextView mTv;
	private ProgressBar mProgressBar;

	public TwsButton(Context context) {
		this(context, null);
	}

	public TwsButton(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.twsButtonStyle);
	}

	public TwsButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwsButton, 0, 0);
		mText = a.getString(R.styleable.TwsButton_text);
		mHeight = (int) a.getDimension(R.styleable.TwsButton_height,
				getResources().getDimensionPixelSize(R.dimen.tws_button_high));
		mButtomMode = a.getInt(R.styleable.TwsButton_twsButtonMode, -1);
		if (-1 == mButtomMode) {
			mBackground = a.getColor(R.styleable.TwsButton_normal_background,
					getResources().getColor(R.color.tws_button_normal_background));
			mPressedBackground = a.getColor(R.styleable.TwsButton_pressed_background,
					getResources().getColor(R.color.tws_button_pressed_background));
			mFocusBackground = a.getColor(R.styleable.TwsButton_focused_background,
					getResources().getColor(R.color.tws_button_focused_background));
			mDisabledBackground = a.getColor(R.styleable.TwsButton_disabled_background,
					getResources().getColor(R.color.tws_button_disabled_background));

			mBorderColor = a.getColor(R.styleable.TwsButton_normal_borderColor,
					getResources().getColor(R.color.tws_button_normal_borderColor));
			mFocusedBorderColor = a.getColor(R.styleable.TwsButton_focused_borderColor,
					getResources().getColor(R.color.tws_button_focused_borderColor));
			mPressedBorderColor = a.getColor(R.styleable.TwsButton_pressed_borderColor,
					getResources().getColor(R.color.tws_button_pressed_borderColor));
			mDisabledBorderColor = a.getColor(R.styleable.TwsButton_disabled_borderColor,
					getResources().getColor(R.color.tws_button_disabled_borderColor));

			mBorderWidth = (int) a.getDimension(R.styleable.TwsButton_borderWidth, getResources()
					.getDimensionPixelSize(R.dimen.tws_button_border));
			mProgressColor = a.getColor(R.styleable.TwsButton_progressColor, Color.TRANSPARENT);
			if (mProgressColor != Color.TRANSPARENT) {
				isProgressButton = true;
				mProgressBorderColor = a.getColor(R.styleable.TwsButton_progress_borderColor, mProgressColor);
				mProgressBorderColor = mProgressColor;
				mProgressBorderWith = mBorderWidth;
				mProgressMax = a.getInt(R.styleable.TwsButton_android_max, 100);
			}

			mRadius = (int) a.getDimension(R.styleable.TwsButton_radius,
					getResources().getDimensionPixelSize(R.dimen.tws_button_round_radius));
			mTextColor = a.getColor(R.styleable.TwsButton_textColor,
					getResources().getColor(R.color.tws_button_textColor));
			mPressedTextColor = mTextColor;
			mDisableTextColor = a.getColor(R.styleable.TwsButton_disabled_textColor,
					getResources().getColor(R.color.tws_button_disabled_textColor));
			mTextSize = (int) a.getDimension(R.styleable.TwsButton_textSize,
					getResources().getDimensionPixelSize(R.dimen.tws_Medium_TextSize));
		} else {
			initDataFromMode(a);
		}

		a.recycle();

		LayoutParams containerParams = new LayoutParams(LayoutParams.WRAP_CONTENT, mHeight);
		setLayoutParams(containerParams);
		setClickable(true);
		setFocusable(true);

		mTv = new TextView(mContext);
		mTv.setText(mText);
		mTv.setGravity(Gravity.CENTER);
		mTv.setTextColor(mTextColor);
		mTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
		mTv.setSingleLine(true);
		mTv.setEllipsize(TruncateAt.END);
		FrameLayout.LayoutParams layoutParam = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		layoutParam.gravity = Gravity.CENTER;
		layoutParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.tws_button_content_margin_left);
		layoutParam.rightMargin = getResources().getDimensionPixelSize(R.dimen.tws_button_content_margin_right);
		mTv.setLayoutParams(layoutParam);

		if (isProgressButton) {
			setupProgressBar();
		}

		updateButtonBackground();

		if (mTv != null) {
			ColorStateList colors = new ColorStateList(new int[][] { { -android.R.attr.state_enabled },
					{ android.R.attr.state_pressed, android.R.attr.state_focused }, { 0 } }, new int[] {
					mDisableTextColor, mPressedTextColor, mTextColor });
			mTv.setTextColor(colors);
		}

		int viewIndex = 0;
		if (mProgressBar != null) {
			this.addView(mProgressBar, viewIndex);
			++viewIndex;
		}

		if (mTv != null) {
			this.addView(mTv, viewIndex);
			++viewIndex;
		}
	}

	public void setText(CharSequence text) {
		if (mTv != null)
			mTv.setText(text);
	}

	public int getProgressMax() {
		if (mProgressBar == null) {
			return 0;
		} else {
			return mProgressBar.getMax();
		}
	}

	public int getProgress() {
		if (mProgressBar == null) {
			return 0;
		} else {
			return mProgressBar.getProgress();
		}
	}

	public void setProgress(int progress) {
		if (mProgressBar != null)
			mProgressBar.setProgress(progress);
	}

	public void setTextColor(int color) {
		if (mTv != null)
			mTv.setTextColor(color);
	}

	public void setTextSize() {
		if (mTv != null) {

		}
	}

	public void setTextSize(float size) {
		if (mTv != null) {
			mTv.setTextSize(size);
		}
	}

	private void initDataFromMode(TypedArray mTypedArray) {
		isProgressButton = false;
		mBackground = getResources().getColor(R.color.tws_button_normal_background);
		mPressedBackground = getResources().getColor(R.color.tws_button_pressed_background);
		mFocusBackground = getResources().getColor(R.color.tws_button_focused_background);
		mDisabledBackground = getResources().getColor(R.color.tws_button_disabled_background);

		mBorderWidth = getResources().getDimensionPixelSize(R.dimen.tws_button_border);

		mRadius = getResources().getDimensionPixelSize(R.dimen.tws_button_round_radius);

		// mTextSize =
		// getResources().getDimensionPixelSize(R.dimen.tws_Medium_TextSize);
		mTextSize = (int) mTypedArray.getDimension(R.styleable.TwsButton_textSize, getResources()
				.getDimensionPixelSize(R.dimen.tws_Medium_TextSize));
		updateBgColorsByMode();
	}

	private void setupProgressBar() {
		mProgressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleHorizontal);

		GradientDrawable bgDrawable = new GradientDrawable();
		bgDrawable.setColor(Color.TRANSPARENT);
		bgDrawable.setCornerRadius(mRadius);

		GradientDrawable secondaryDrawable = new GradientDrawable();
		secondaryDrawable.setColor(Color.TRANSPARENT);
		secondaryDrawable.setCornerRadius(mRadius);
		ClipDrawable secondaryClipDrawable = new ClipDrawable(secondaryDrawable, Gravity.START, ClipDrawable.HORIZONTAL);

		GradientDrawable progressDrawable = new GradientDrawable();
		progressDrawable.setColor(mProgressColor);
		progressDrawable.setCornerRadius(mRadius);
		progressDrawable.setStroke(mProgressBorderWith, mProgressBorderColor);
		ClipDrawable progressClipDrawable = new ClipDrawable(progressDrawable, Gravity.START, ClipDrawable.HORIZONTAL);

		Drawable[] progressDrawables = new Drawable[] { bgDrawable, secondaryClipDrawable, progressClipDrawable };
		LayerDrawable progressLayerDrawable = new LayerDrawable(progressDrawables);
		progressLayerDrawable.setId(0, android.R.id.background);
		progressLayerDrawable.setId(1, android.R.id.secondaryProgress);
		progressLayerDrawable.setId(2, android.R.id.progress);

		mProgressBar.setProgressDrawable(progressLayerDrawable);
		mProgressBar.setMax(mProgressMax);
		FrameLayout.LayoutParams layoutParam = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		mProgressBar.setLayoutParams(layoutParam);
		mProgressBar.setProgress(0);
	}

	private void updateBgColorsByMode() {
		switch (mButtomMode) {
		case RecommendedButton:
			mBorderColor = getResources().getColor(R.color.tws_button_normal_borderColor_Recommended);
			mFocusedBorderColor = getResources().getColor(R.color.tws_button_focused_borderColor_Recommended);
			mPressedBorderColor = getResources().getColor(R.color.tws_button_pressed_borderColor_Recommended);
			mDisabledBorderColor = getResources().getColor(R.color.tws_button_disabled_borderColor_Recommended);

			mTextColor = getResources().getColor(R.color.tws_button_textColor_Recommended);
			mPressedTextColor = mTextColor;
			mDisableTextColor = getResources().getColor(R.color.tws_button_disabled_textColor_Recommended);
			break;
		case ProgressButton:
			isProgressButton = true;
			mBorderColor = getResources().getColor(R.color.tws_button_normal_borderColor_Recommended);
			mFocusedBorderColor = getResources().getColor(R.color.tws_button_focused_borderColor_Recommended);
			mPressedBorderColor = getResources().getColor(R.color.tws_button_pressed_borderColor_Recommended);
			mDisabledBorderColor = getResources().getColor(R.color.tws_button_disabled_borderColor_Progress);
			mProgressColor = getResources().getColor(R.color.tws_brand_percent_20);
			mProgressBorderColor = getResources().getColor(R.color.tws_button_progress_borderColor);
			mProgressBorderWith = mBorderWidth;
			mTextColor = getResources().getColor(R.color.tws_button_textColor_Progress);
			mPressedTextColor = mTextColor;
			mDisableTextColor = getResources().getColor(R.color.tws_button_disabled_textColor_Progress);
			break;
		case NormalButton:
		default:
			mBorderColor = getResources().getColor(R.color.tws_button_normal_borderColor);
			mFocusedBorderColor = getResources().getColor(R.color.tws_button_focused_borderColor);
			mPressedBorderColor = getResources().getColor(R.color.tws_button_pressed_borderColor);
			mDisabledBorderColor = getResources().getColor(R.color.tws_button_disabled_borderColor);
			mTextColor = getResources().getColor(R.color.tws_button_textColor);
			mPressedTextColor = mTextColor;
			mDisableTextColor = getResources().getColor(R.color.tws_button_disabled_textColor);
			break;
		}
	}

	private void updateButtonBackground() {
		GradientDrawable normalDrawable = new GradientDrawable();
		normalDrawable.setCornerRadius(mRadius);
		normalDrawable.setColor(mBackground);
		normalDrawable.setStroke(mBorderWidth, mBorderColor);

		GradientDrawable focusDrawable = new GradientDrawable();
		focusDrawable.setCornerRadius(mRadius);
		focusDrawable.setColor(mFocusBackground);
		focusDrawable.setStroke(mBorderWidth, mFocusedBorderColor);

		GradientDrawable pressedDrawable = new GradientDrawable();
		pressedDrawable.setCornerRadius(mRadius);
		pressedDrawable.setColor(mPressedBackground);
		pressedDrawable.setStroke(mBorderWidth, mPressedBorderColor);

		GradientDrawable disabledDrawable = new GradientDrawable();
		disabledDrawable.setCornerRadius(mRadius);
		disabledDrawable.setColor(mDisabledBackground);
		disabledDrawable.setStroke(mBorderWidth, mDisabledBorderColor);

		StateListDrawable states = new StateListDrawable();

		states.addState(new int[] { -android.R.attr.state_enabled }, disabledDrawable);
		states.addState(new int[] { android.R.attr.state_pressed }, pressedDrawable);
		states.addState(new int[] { android.R.attr.state_focused }, focusDrawable);
		states.addState(new int[] {}, normalDrawable);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			this.setBackgroundDrawable(states);
		} else {
			this.setBackground(states);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (mTv != null) {
			mTv.setEnabled(enabled);
		}

		if (mProgressBar != null) {
			mProgressBar.setEnabled(enabled);
		}
	}

	public void setButtonMode(int mode) {
		mButtomMode = mode;
		updateBgColorsByMode();
		updateButtonBackground();
	}
}
