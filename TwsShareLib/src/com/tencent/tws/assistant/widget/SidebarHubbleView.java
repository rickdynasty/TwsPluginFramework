package com.tencent.tws.assistant.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.tencent.tws.sharelib.R;


public class SidebarHubbleView extends View {
	public static String TAG = "SidebarHubbleView";

	public final int hubble_in_per_frame = getResources().getInteger(R.integer.hubble_in_per_frame);
	public final int hubble_out_per_frame = getResources().getInteger(R.integer.hubble_out_per_frame);
	public static int IN_FRAME_COUNT = 10;
	public static int OUT_FRAME_COUNT = 8;

	private Paint mPaint = new Paint();
	private Paint mNonExistPaint = new Paint();
	private final int fPaintSize = (int) getResources().getDimension(R.dimen.hubble_text_size);
	private LevelListDrawable mHubbleInDrawable;
	private LevelListDrawable mHubbleOutDrawable;
	private Drawable mHubbleLeft;
	private Drawable mHubbleRight;
	public final int NORMAL_SHOW = 0;
	public final int IN_ANIMA = 1;
	public final int OUT_ANIMA = 2;
	private int mStatus = NORMAL_SHOW;

	private ValueAnimator mAnimator;
	private String mText = null;
	private float offsetX = 0;
	private float offsetY = 0;
	// 0~7
	private final int ANIMA_IN_END_LEVEL = 7;
	// 3~7
	private final int ANIMA_OUT_BEGIN_LEVEL = 3;

	public final int PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;
	private final int offsetX_NORMAL = (int) getResources().getDimension(R.dimen.hubble_offsetx_in_10);
	private final int fSideBarHubbleViewHigh = (int) getResources().getDimension(R.dimen.sidbar_hubble_h);
	private final int fHubbleViewChassisWith = (int) getResources().getDimension(R.dimen.sidbar_hubble_chassis_w);
	private final int fHubbleViewLeftWith = (int) getResources().getDimension(R.dimen.sidbar_hubble_left_w);
	private final int fHubbleViewRightWith = (int) getResources().getDimension(R.dimen.sidbar_hubble_right_w);
	private final int fHubbleViewZoomX = (int) getResources().getDimension(R.dimen.sidbar_hubble_zoom_x);
	private final int fHubbleViewHigh = (int) getResources().getDimension(R.dimen.hubble_high);

	//save the pos x-zoom
	private final int[] offsetX_IN = { (int) getResources().getDimension(R.dimen.hubble_offsetx_in_1), (int) getResources().getDimension(R.dimen.hubble_offsetx_in_2),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_in_3), (int) getResources().getDimension(R.dimen.hubble_offsetx_in_4),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_in_5), (int) getResources().getDimension(R.dimen.hubble_offsetx_in_6),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_in_7), (int) getResources().getDimension(R.dimen.hubble_offsetx_in_8),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_in_9), offsetX_NORMAL, };
	private final float[] scaling_IN = { 0.606f, 0.606f, 0.67f, 0.777f, 0.85f, 0.896f, 0.956f, 0.956f, 0.978f, 1.00f, };
	private final int[] offsetX_OUT = { (int) getResources().getDimension(R.dimen.hubble_offsetx_out_1), (int) getResources().getDimension(R.dimen.hubble_offsetx_out_2),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_out_3), (int) getResources().getDimension(R.dimen.hubble_offsetx_out_4),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_out_5), (int) getResources().getDimension(R.dimen.hubble_offsetx_out_6),
			(int) getResources().getDimension(R.dimen.hubble_offsetx_out_7), (int) getResources().getDimension(R.dimen.hubble_offsetx_out_8), };
	private final float[] scaling_OUT = { 1.00f, 0.97f, 0.94f, 0.94f, 0.792f, 0.777f, 0.777f, 0.777f, };
	private boolean mIndexIsExsit = true;
	private int mNormalTextColor = Color.WHITE;// 0x4c000000;
	private int mNonExistTextColor = Color.WHITE;// 0x19000000;
	private int mNormalBgColor = 0xFF000000;
	private int mNonExistBgColor = 0xFFe5e5e5;

	private int animateCount = 0;
	private int mLevel = -1;
	private Rect[] mHubble_R_IN;// = new Rect[10];
	private Rect[] mHubble_R_OUT;

	private Rect[] mHubble_L_IN;
	private Rect[] mHubble_L_OUT;

	public SidebarHubbleView(Context context) {
		this(context, null);
	}

	public SidebarHubbleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SidebarHubbleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	public int getHubbleInPerFrame(){
		return hubble_in_per_frame;
	}
	
	public int getHubbleOutPerFrame(){
		return hubble_out_per_frame;
	}

	private void init() {
		mHubbleInDrawable = (LevelListDrawable) getResources().getDrawable(R.drawable.hubble_in_frame_levellist);
		mHubbleOutDrawable = (LevelListDrawable) getResources().getDrawable(R.drawable.hubble_out_frame_levellist);
		mHubbleLeft = getResources().getDrawable(R.drawable.sidbar_hubble_part_left);
		mHubbleRight = getResources().getDrawable(R.drawable.sidbar_hubble_part_right);
		setBackgroundColor(Color.TRANSPARENT);
		mPaint.setTextSize(fPaintSize);
		mPaint.setColor(mNormalTextColor);
		mNonExistPaint.setTextSize(fPaintSize);
		mNonExistPaint.setColor(mNonExistTextColor);

		mHubbleInDrawable.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
		mHubbleOutDrawable.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
		mHubbleLeft.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
		mHubbleRight.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);

		//cal all pos-rect
		// IN
		mHubble_L_IN = new Rect[scaling_IN.length];
		float scal = 0.0f;
		for (int index = 0; index < mHubble_L_IN.length; index++) {
			scal = scaling_IN[index];
			mHubble_L_IN[index] = new Rect();
			mHubble_L_IN[index].left = (int) (offsetX_IN[index] - fHubbleViewZoomX * scal);
			mHubble_L_IN[index].right = (int) (mHubble_L_IN[index].left + fHubbleViewLeftWith * scal);
			mHubble_L_IN[index].top = (int) (fSideBarHubbleViewHigh - fHubbleViewHigh * scal) / 2;
			mHubble_L_IN[index].bottom = (int) (mHubble_L_IN[index].top + fHubbleViewHigh * scal);
		}
		mHubble_R_IN = new Rect[scaling_IN.length];
		for (int index = 0; index < mHubble_R_IN.length; index++) {
			scal = scaling_IN[index];
			mHubble_R_IN[index] = new Rect();
			mHubble_R_IN[index].left = mHubble_L_IN[index].right;
			mHubble_R_IN[index].right = (int) (mHubble_R_IN[index].left + fHubbleViewRightWith * scal);

			mHubble_R_IN[index].top = mHubble_L_IN[index].top;
			mHubble_R_IN[index].bottom = mHubble_L_IN[index].bottom;
		}
		// OUT
		mHubble_L_OUT = new Rect[scaling_OUT.length];
		for (int index = 0; index < mHubble_L_OUT.length; index++) {
			scal = scaling_OUT[index];
			mHubble_L_OUT[index] = new Rect();
			mHubble_L_OUT[index].left = (int) (offsetX_OUT[index] - fHubbleViewZoomX * scal);
			mHubble_L_OUT[index].right = (int) (mHubble_L_OUT[index].left + fHubbleViewLeftWith * scal);
			mHubble_L_OUT[index].top = (int) (fSideBarHubbleViewHigh - fHubbleViewHigh * scal) / 2;
			mHubble_L_OUT[index].bottom = (int) (mHubble_L_OUT[index].top + fHubbleViewHigh * scal);
		}
		mHubble_R_OUT = new Rect[scaling_OUT.length];
		for (int index = 0; index < mHubble_R_OUT.length; index++) {
			scal = scaling_OUT[index];
			mHubble_R_OUT[index] = new Rect();
			mHubble_R_OUT[index].left = mHubble_L_OUT[index].right;
			mHubble_R_OUT[index].right = (int) (mHubble_R_OUT[index].left + fHubbleViewRightWith * scal);

			mHubble_R_OUT[index].top = mHubble_L_OUT[index].top;
			mHubble_R_OUT[index].bottom = mHubble_L_OUT[index].bottom;
		}
	}

	public void setNormalTextColor(int color) {
		if (mNormalTextColor != color) {
			mNormalTextColor = color;
			mPaint.setColor(mNormalTextColor);
		}
	}

	public void setNonExistTextColor(int color) {
		if (mNonExistTextColor != color) {
			mNonExistTextColor = color;
			mNonExistPaint.setColor(mNonExistTextColor);
		}
	}

	public void setNormalBgColor(int color) {
		if (mNormalBgColor != color) {
			mNormalBgColor = color;
			if (mIndexIsExsit) {
				setNormalBgColor();
			}
		}
	}

	public void setNonExistBgColor(int color) {
		if (mNonExistBgColor != color) {
			mNonExistBgColor = color;
			if (!mIndexIsExsit) {
				setNonExistBgColor();
			}
		}
	}

	public void setNormalBgColor() {
		if (mNormalBgColor != mNonExistBgColor) {
			mHubbleInDrawable.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleOutDrawable.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleLeft.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleRight.setColorFilter(mNormalBgColor, PorterDuff.Mode.SRC_IN);
		}
	}

	public void setNonExistBgColor() {
		if (mNonExistBgColor != mNormalBgColor) {
			mHubbleInDrawable.setColorFilter(mNonExistBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleOutDrawable.setColorFilter(mNonExistBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleLeft.setColorFilter(mNonExistBgColor, PorterDuff.Mode.SRC_IN);
			mHubbleRight.setColorFilter(mNonExistBgColor, PorterDuff.Mode.SRC_IN);
		}
	}

	public void showHubbleWithAnimation() {
		if (mAnimator != null) {
			mAnimator.cancel();
			mAnimator = null;
		}
		mLevel = -1;
		animateCount = 0;

		mAnimator = ValueAnimator.ofInt(0, IN_FRAME_COUNT - 1);
		long duratime = IN_FRAME_COUNT * hubble_in_per_frame;
		mAnimator.setDuration(duratime);
		//mAnimator.setEvaluator(new QuadEaseOut(duratime));
		mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				++animateCount;
				int iValue = animator.getAnimatedValue() == null ? 0 : (Integer) animator.getAnimatedValue();
				if (iValue < 0 || IN_FRAME_COUNT <= iValue)
					return;

				if (mLevel == iValue)
					return;

				mLevel = iValue;
				// set ChassisDrawable level
				if (mLevel <= ANIMA_IN_END_LEVEL)
					mHubbleInDrawable.setLevel(mLevel);

				invalidate();
			}
		});

		mAnimator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator arg0) {
				mStatus = IN_ANIMA;
				animateCount = 0;
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				mStatus = NORMAL_SHOW;
			}

			@Override
			public void onAnimationCancel(Animator arg0) {
			}
		});

		mStatus = IN_ANIMA;
		mAnimator.start();
	}

	public void hidHubbleWithAnimation() {
		if (mAnimator != null) {
			mAnimator.cancel();
			mAnimator = null;
		}
		mLevel = -1;
		animateCount = 0;

		mAnimator = ValueAnimator.ofInt(0, OUT_FRAME_COUNT - 1);
		long duratime = OUT_FRAME_COUNT * hubble_out_per_frame;
		mAnimator.setDuration(duratime);
		//mAnimator.setEvaluator(new QuadEaseOut(duratime));
		mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				++animateCount;
				final int iValue = animator.getAnimatedValue() == null ? 0 : (Integer) animator.getAnimatedValue();
				if (iValue < 0 || OUT_FRAME_COUNT <= iValue || mLevel == iValue)
					return;

				mLevel = iValue;

				// ChassisDrawable
				if (ANIMA_OUT_BEGIN_LEVEL <= mLevel)
					mHubbleOutDrawable.setLevel(mLevel - ANIMA_OUT_BEGIN_LEVEL);

				invalidate();
			}
		});

		mAnimator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator arg0) {
				mStatus = OUT_ANIMA;
				animateCount = 0;
			}

			@Override
			public void onAnimationRepeat(Animator arg0) {
			}

			@Override
			public void onAnimationEnd(Animator arg0) {
				// mStatus = NORMAL_SHOW;
			}

			@Override
			public void onAnimationCancel(Animator arg0) {
			}
		});

		mStatus = OUT_ANIMA;
		mAnimator.start();
	}

	public boolean stopOut() {
		if (OUT_ANIMA == mStatus && mAnimator.isRunning()) {
			mAnimator.cancel();
			mStatus = NORMAL_SHOW;
			mHubbleInDrawable.setLevel(IN_FRAME_COUNT - 1);
			invalidate();
			return true;
		}

		return false;
	}

	public void setText(String text) {
		if (TextUtils.equals(mText, text))
			return;

		mText = text;

		invalidate();
	}

	public float theRestOfInAnim() {
		float rest = 0.0f;
		if (mStatus == IN_ANIMA) {
			rest = 1.0f - ((float) mLevel) / (IN_FRAME_COUNT - 1);
		}

		return rest;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.save();

		Rect bounds = new Rect(getWidth() - fHubbleViewChassisWith, 0, getWidth(), getHeight());
		if (mFontMetricsInt == null)
			mFontMetricsInt = mPaint.getFontMetricsInt();

		if (mStatus == OUT_ANIMA) {
			//draw Chassis
			if (ANIMA_OUT_BEGIN_LEVEL <= mLevel && needDrawChassis()) {
				mHubbleOutDrawable.setBounds(bounds);
				mHubbleOutDrawable.getLevel();
				mHubbleOutDrawable.draw(canvas);
			}

			mHubbleLeft.setBounds(mHubble_L_OUT[mLevel]);
			mHubbleLeft.draw(canvas);

			if (mLevel < ANIMA_OUT_BEGIN_LEVEL) {
				mHubbleRight.setBounds(mHubble_R_OUT[mLevel]);
				mHubbleRight.draw(canvas);
			}

			offsetX = offsetX_OUT[mLevel] - fHubbleViewZoomX + (fHubbleViewLeftWith - mPaint.measureText(mText)) / 2;
			offsetY = (getHeight() + Math.abs(mFontMetricsInt.ascent + mFontMetricsInt.descent)) / 2;
		} else {
			if (NORMAL_SHOW == mStatus && mHubble_R_IN.length - 1 != mLevel) {
				mLevel = mHubble_R_IN.length - 1;
				mHubbleInDrawable.setLevel(mLevel);
			}

			if (mLevel <= ANIMA_IN_END_LEVEL && needDrawChassis()) {
				mHubbleInDrawable.setBounds(bounds);
				mHubbleInDrawable.draw(canvas);
			}

			mHubbleRight.setBounds(mHubble_R_IN[mLevel]);
			mHubbleRight.draw(canvas);

			mHubbleLeft.setBounds(mHubble_L_IN[mLevel]);
			mHubbleLeft.draw(canvas);

			offsetX = offsetX_IN[mLevel] - fHubbleViewZoomX + (fHubbleViewLeftWith - mPaint.measureText(mText)) / 2;
			offsetY = (getHeight() + Math.abs(mFontMetricsInt.ascent + mFontMetricsInt.descent)) / 2;
		}

		if (mText != null && 0 < offsetX && 0 < offsetY) {
			if (mIndexIsExsit)
				canvas.drawText(mText, offsetX, offsetY, mPaint);
			else
				canvas.drawText(mText, offsetX, offsetY, mNonExistPaint);
		}

		canvas.restore();
	}

	private FontMetricsInt mFontMetricsInt = null;

	public void setIndexIsExsit(boolean exist) {
		if (mIndexIsExsit != exist) {
			if (exist) {
				setNormalBgColor();
				mPaint.setColor(mNormalTextColor);
			} else {
				setNonExistBgColor();
				mPaint.setColor(mNonExistTextColor);
			}
		}

		mIndexIsExsit = exist;
	}

	private boolean needDrawChassis() {
		if ((mStatus == OUT_ANIMA && ANIMA_OUT_BEGIN_LEVEL <= mLevel) || (mStatus == IN_ANIMA && mLevel <= ANIMA_IN_END_LEVEL)) {
			return true;
		}

		return true;
	}
}
