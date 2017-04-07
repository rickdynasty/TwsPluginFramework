package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TwsTabWidget;
import android.widget.TwsTabWidget.OnTabItemCenterPosListener;

import com.tencent.tws.assistant.support.v4.view.ViewConfigurationCompat;
import com.tencent.tws.assistant.support.v4.view.ViewPager;
import com.tencent.tws.sharelib.R;

/**
 * Draws a line for each page. The current page line is colored differently than
 * the unselected page lines.
 */
public class TabIndicator extends View implements ViewPager.OnPageChangeListener, OnTabItemCenterPosListener {
	private static final int INVALID_POINTER = -1;
	private static final int FADE_FRAME_MS = 30;

	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Drawable mIndicatorDrawable;

	private boolean mFades;
	private int mFadeDelay;
	private int mFadeLength;
	private int mFadeBy;

	private ViewPager mViewPager;
	private ViewPager.OnPageChangeListener mListener;
	private int mScrollState;
	private int mCurrentPage;
	private float mPositionOffset;

	private int mTouchSlop;
	private float mLastMotionX = -1;
	private int mActivePointerId = INVALID_POINTER;
	private boolean mIsDragging;

	private final Runnable mFadeRunnable = new Runnable() {
		@Override
		public void run() {
			if (!mFades)
				return;

			final int alpha = Math.max(mPaint.getAlpha() - mFadeBy, 0);
			mPaint.setAlpha(alpha);
			invalidate();
			if (alpha > 0) {
				postDelayed(this, FADE_FRAME_MS);
			}
		}
	};

	public TabIndicator(Context context) {
		this(context, null);
	}

	public TabIndicator(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.tabIndicatorStyle);
	}

	public TabIndicator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (isInEditMode())
			return;

		final Resources res = getResources();

		// Load defaults from resources
		final boolean defaultFades = res.getBoolean(R.bool.default_tab_indicator_fades);
		final int defaultFadeDelay = res.getInteger(R.integer.default_tab_indicator_fade_delay);
		final int defaultFadeLength = res.getInteger(R.integer.default_tab_indicator_fade_length);
		final int defaultSelectedColor = res.getColor(R.color.default_tab_indicator_selected_color);

		// Retrieve styles attributes
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabIndicator, defStyle, 0);

		setFades(a.getBoolean(R.styleable.TabIndicator_indicatorFades, defaultFades));
		setSelectedColor(a.getColor(R.styleable.TabIndicator_indicatorSelectedColor, defaultSelectedColor));
		setFadeDelay(a.getInteger(R.styleable.TabIndicator_indicatorFadeDelay, defaultFadeDelay));
		setFadeLength(a.getInteger(R.styleable.TabIndicator_indicatorFadeLength, defaultFadeLength));

		Drawable background = a.getDrawable(R.styleable.TabIndicator_android_background);
		if (background != null) {
			setBackgroundDrawable(background);
		}
		mIndicatorDrawable = a.getDrawable(R.styleable.TabIndicator_android_src);
		if (mIndicatorDrawable == null)
			mIndicatorDrawable = res.getDrawable(R.drawable.tab_indicator_light);

		a.recycle();

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
	}

	public boolean getFades() {
		return mFades;
	}

	public void setFades(boolean fades) {
		if (fades != mFades) {
			mFades = fades;
			if (fades) {
				post(mFadeRunnable);
			} else {
				removeCallbacks(mFadeRunnable);
				mPaint.setAlpha(0xFF);
				invalidate();
			}
		}
	}

	public int getFadeDelay() {
		return mFadeDelay;
	}

	public void setFadeDelay(int fadeDelay) {
		mFadeDelay = fadeDelay;
	}

	public int getFadeLength() {
		return mFadeLength;
	}

	public void setFadeLength(int fadeLength) {
		mFadeLength = fadeLength;
		mFadeBy = 0xFF / (mFadeLength / FADE_FRAME_MS);
	}

	public int getSelectedColor() {
		return mPaint.getColor();
	}

	public void setSelectedColor(int selectedColor) {
		mPaint.setColor(selectedColor);
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mViewPager == null || 0 == mViewPager.getAdapter().getCount()) {
			return;
		}
		final int count = mViewPager.getAdapter().getCount();
		if (mCurrentPage >= count) {
			setCurrentItem(count - 1);
			return;
		}
		final int height = getHeight();
		final int width = getWidth();

		final int paddingLeft = getPaddingLeft();
		final float pageWidth = (width - paddingLeft - getPaddingRight()) / (1f * count);
		final float left = paddingLeft + pageWidth * (mCurrentPage + mPositionOffset);
		final float right = left + pageWidth;
		final float top = getPaddingTop();
		final float bottom = height - getPaddingBottom();
		canvas.drawRect(left, top, right, bottom, mPaint);

		if (mIndicatorDrawable != null) {
			float drawableWidth = mIndicatorDrawable.getIntrinsicWidth();
			float drawableHeight = mIndicatorDrawable.getIntrinsicHeight();
			if (drawableWidth > pageWidth) {
				drawableWidth = pageWidth;
			}
			if (drawableHeight > height) {
				drawableHeight = height;
			}

			final float drawableTop = height - drawableHeight + getPaddingTop();
			final float drawableBottom = height - getPaddingBottom();
			float drawableLeft, drawableRight;
			if (mTabItemCenterPosX != null && mCurrentPage < mTabItemCenterPosX.length && 0 < mTabItemCenterPosX[mCurrentPage]) {
				drawableLeft = mTabItemCenterPosX[mCurrentPage] - drawableWidth / 2;
				float pageWidthEx = 0;
				if (mCurrentPage == mTabItemCenterPosX.length - 1) {
					pageWidthEx = drawableLeft < width ? width - mTabItemCenterPosX[mCurrentPage] : width;
				} else {
					pageWidthEx = mTabItemCenterPosX[mCurrentPage + 1] - mTabItemCenterPosX[mCurrentPage];
				}
				drawableLeft += pageWidthEx * mPositionOffset;
				drawableRight = drawableLeft + drawableWidth;
			} else {
				drawableLeft = paddingLeft + pageWidth * (mCurrentPage + mPositionOffset) + ((pageWidth - drawableWidth) / 2);
				drawableRight = drawableLeft + drawableWidth;
			}

			mIndicatorDrawable.setBounds((int) drawableLeft, (int) drawableTop, (int) drawableRight, (int) drawableBottom);
			mIndicatorDrawable.draw(canvas);
		}
	}

	public void setViewPager(ViewPager viewPager) {
		if (mViewPager == viewPager) {
			return;
		}
		if (mViewPager != null) {
			// Clear us from the old pager.
			mViewPager.setOnPageChangeListener(null);
		}
		if (viewPager.getAdapter() == null) {
			throw new IllegalStateException("ViewPager does not have adapter instance.");
		}
		mViewPager = viewPager;
		mViewPager.setOnPageChangeListener(this);
		invalidate();
		post(new Runnable() {
			@Override
			public void run() {
				if (mFades) {
					post(mFadeRunnable);
				}
			}
		});
	}

	public void setViewPager(ViewPager view, int initialPosition) {
		setViewPager(view);
		setCurrentItem(initialPosition);
	}

	public void setCurrentItem(int item) {
		if (mViewPager == null) {
			throw new IllegalStateException("ViewPager has not been bound.");
		}
		mViewPager.setCurrentItem(item);
		mCurrentPage = item;
		invalidate();
	}

	public void notifyDataSetChanged() {
		invalidate();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		mScrollState = state;

		if (mListener != null) {
			mListener.onPageScrollStateChanged(state);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		mCurrentPage = position;
		mPositionOffset = positionOffset;
		if (mFades) {
			if (positionOffsetPixels > 0) {
				removeCallbacks(mFadeRunnable);
				mPaint.setAlpha(0xFF);
			} else if (mScrollState != ViewPager.SCROLL_STATE_DRAGGING) {
				postDelayed(mFadeRunnable, mFadeDelay);
			}
		}
		invalidate();

		if (mListener != null) {
			mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
		}
	}

	@Override
	public void onPageSelected(int position) {
		if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
			mCurrentPage = position;
			mPositionOffset = 0;
			invalidate();
			mFadeRunnable.run();
		}
		if (mListener != null) {
			mListener.onPageSelected(position);
		}
	}

	public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
		mListener = listener;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		mCurrentPage = savedState.currentPage;
		requestLayout();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.currentPage = mCurrentPage;
		return savedState;
	}

	static class SavedState extends BaseSavedState {
		int currentPage;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			currentPage = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(currentPage);
		}

		@SuppressWarnings("UnusedDeclaration")
		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	private int[] mTabItemCenterPosX = null;

	@Override
	public void setTabsPos(int[] positions) {
		mTabItemCenterPosX = positions;
	}
}