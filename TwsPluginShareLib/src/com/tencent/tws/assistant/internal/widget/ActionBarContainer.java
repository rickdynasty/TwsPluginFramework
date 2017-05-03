/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.tencent.tws.assistant.internal.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.sharelib.R;

/**
 * This class acts as a container for the action bar view and action mode
 * context views. It applies special styles as needed to help handle animated
 * transitions between them.
 * 
 * @hide
 */
public class ActionBarContainer extends FrameLayout {
	private static final String TAG = "rick_Debug:ActionBarContainer";
	private boolean mIsTransitioning;
	private ScrollingTabContainerView mTabContainer;
	private ActionBarView mActionBarView;

	private Drawable mBackground;
	private Drawable mStackedBackground;
	private Drawable mSplitBackground;
	private boolean mIsSplit;
	private boolean mIsStacked;

	private int mBackgroundResId;

	public ActionBarContainer(Context context) {
		this(context, null);
	}

	public ActionBarContainer(Context context, AttributeSet attrs) {
		super(context, attrs);

		setBackgroundDrawable(null);
		Log.w(TAG, "context is " + context);
		Log.w(TAG, "context.Res is " + context.getResources());

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar);
		Log.d(TAG, "ActionBarContainer a is " + a);
		Resources resources = a.getResources();
		resources.getDrawable(R.drawable.ic_launcher);
		
		mBackground = a.getDrawable(R.styleable.ActionBar_background);
		if (mBackground == null) {
			mBackground = new ColorDrawable(getResources().getColor(R.color.tws_bar_background));
		}
		mBackgroundResId = a.getResourceId(R.styleable.ActionBar_background, R.color.tws_bar_background);

		mStackedBackground = a.getDrawable(R.styleable.ActionBar_backgroundStacked);
		if (mStackedBackground == null) {
			mStackedBackground = new ColorDrawable(getResources().getColor(R.color.tws_bar_background));
		}
		Log.d(TAG, "mBackground is " + mBackground + " mBackgroundResId=" + Integer.toHexString(mBackgroundResId)
				+ " mStackedBackground=" + mStackedBackground);

		if (getId() == R.id.tws_split_action_bar) {
			mIsSplit = true;
			mSplitBackground = a.getDrawable(R.styleable.ActionBar_backgroundSplit);
		}
		a.recycle();

		setWillNotDraw(mIsSplit ? mSplitBackground == null : mBackground == null && mStackedBackground == null);
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
		mActionBarView = (ActionBarView) findViewById(R.id.tws_action_bar);
	}

	public void setPrimaryBackground(Drawable bg) {
		if (mBackground != null) {
			mBackground.setCallback(null);
			unscheduleDrawable(mBackground);
		}
		mBackground = bg;
		if (bg != null) {
			bg.setCallback(this);
			if (mActionBarView != null) {
				mBackground.setBounds(mActionBarView.getLeft(), mActionBarView.getTop(), mActionBarView.getRight(),
						mActionBarView.getBottom());
			}
		}
		setWillNotDraw(mIsSplit ? mSplitBackground == null : mBackground == null && mStackedBackground == null);
		invalidate();
	}

	public void setStackedBackground(Drawable bg) {
		if (mStackedBackground != null) {
			mStackedBackground.setCallback(null);
			unscheduleDrawable(mStackedBackground);
		}
		mStackedBackground = bg;
		if (bg != null) {
			bg.setCallback(this);
			if ((mIsStacked && mStackedBackground != null)) {
				mStackedBackground.setBounds(mTabContainer.getLeft(), mTabContainer.getTop(), mTabContainer.getRight(),
						mTabContainer.getBottom());
			}
		}
		setWillNotDraw(mIsSplit ? mSplitBackground == null : mBackground == null && mStackedBackground == null);
		invalidate();
	}

	public void setSplitBackground(Drawable bg) {
		if (mSplitBackground != null) {
			mSplitBackground.setCallback(null);
			unscheduleDrawable(mSplitBackground);
		}
		mSplitBackground = bg;
		if (bg != null) {
			bg.setCallback(this);
			if (mIsSplit && mSplitBackground != null) {
				mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
			}
		}
		setWillNotDraw(mIsSplit ? mSplitBackground == null : mBackground == null && mStackedBackground == null);
		invalidate();
	}

	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		final boolean isVisible = visibility == VISIBLE;
		if (mBackground != null)
			mBackground.setVisible(isVisible, false);
		if (mStackedBackground != null)
			mStackedBackground.setVisible(isVisible, false);
		if (mSplitBackground != null)
			mSplitBackground.setVisible(isVisible, false);
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return (who == mBackground && !mIsSplit) || (who == mStackedBackground && mIsStacked)
				|| (who == mSplitBackground && mIsSplit) || super.verifyDrawable(who);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mBackground != null && mBackground.isStateful()) {
			mBackground.setState(getDrawableState());
		}
		if (mStackedBackground != null && mStackedBackground.isStateful()) {
			mStackedBackground.setState(getDrawableState());
		}
		if (mSplitBackground != null && mSplitBackground.isStateful()) {
			mSplitBackground.setState(getDrawableState());
		}
	}

	@Override
	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (mBackground != null) {
			mBackground.jumpToCurrentState();
		}
		if (mStackedBackground != null) {
			mStackedBackground.jumpToCurrentState();
		}
		if (mSplitBackground != null) {
			mSplitBackground.jumpToCurrentState();
		}
	}

	public int getPrimaryBackgroundResId() {
		return mBackgroundResId;
	}

	/**
	 * Set the action bar into a "transitioning" state. While transitioning the
	 * bar will block focus and touch from all of its descendants. This prevents
	 * the user from interacting with the bar while it is animating in or out.
	 * 
	 * @param isTransitioning
	 *            true if the bar is currently transitioning, false otherwise.
	 */
	public void setTransitioning(boolean isTransitioning) {
		mIsTransitioning = isTransitioning;
		setDescendantFocusability(isTransitioning ? FOCUS_BLOCK_DESCENDANTS : FOCUS_AFTER_DESCENDANTS);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return mIsTransitioning || super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		super.onTouchEvent(ev);

		// An action bar always eats touch events.
		return true;
	}

	@Override
	public boolean onHoverEvent(MotionEvent ev) {
		super.onHoverEvent(ev);

		// An action bar always eats hover events.
		return true;
	}

	public void setTabContainer(ScrollingTabContainerView tabView) {
		if (mTabContainer != null) {
			removeView(mTabContainer);
		}
		mTabContainer = tabView;

		if (mTabContainer != null)
			mTabContainer.setStackedDrawable(mStackedBackground);

		if (tabView != null) {
			addView(tabView);
			final ViewGroup.LayoutParams lp = tabView.getLayoutParams();
			lp.width = LayoutParams.MATCH_PARENT;
			lp.height = LayoutParams.WRAP_CONTENT;
			tabView.setAllowCollapse(false);
		}
	}

	public View getTabContainer() {
		return mTabContainer;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (getWidth() == 0 || getHeight() == 0) {
			return;
		}

		if (mIsSplit) {
			if (mSplitBackground != null)
				mSplitBackground.draw(canvas);
		} else {
			if (mBackground != null) {
				mBackground.draw(canvas);
			}
			if (mTabContainer != null && mTabContainer.getTabWaveEnable()) {
				if (mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_OVERLAY) {
					return;
				} else if (mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND
						|| mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) {
					if (mStackedBackground != null && mIsStacked) {
						mStackedBackground.draw(canvas);
					}
				}
			} else {
				if (mStackedBackground != null && mIsStacked) {
					mStackedBackground.draw(canvas);
				}
			}
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mActionBarView == null)
			return;

		final LayoutParams lp = (LayoutParams) mActionBarView.getLayoutParams();

		final int actionBarViewHeight = mActionBarView.isCollapsed() ? 0 : mActionBarView.getMeasuredHeight()
				+ lp.topMargin + lp.bottomMargin;

		if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
			final int mode = MeasureSpec.getMode(heightMeasureSpec);
			if (mode == MeasureSpec.AT_MOST) {
				final int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
				setMeasuredDimension(getMeasuredWidth(),
						Math.min(actionBarViewHeight + mTabContainer.getMeasuredHeight(), maxHeight));
			}
		}
	}

	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		// tws-start modify for actionbar shadow::2014-12-24
		// final boolean hasTabs = mTabContainer != null &&
		// mTabContainer.getVisibility() != GONE;
		final boolean hasTabs = mTabContainer != null;
		// tws-end modify for actionbar shadow::2014-12-24

		if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
			final int containerHeight = getMeasuredHeight();
			final int tabHeight = mTabContainer.getMeasuredHeight();
			final int shadowHeight = getResources().getDimensionPixelSize(R.dimen.tws_action_bar_shadow_height);

			if (((mActionBarView.getDisplayOptions() & ActionBar.DISPLAY_SHOW_HOME) == 0)
					&& ((mActionBarView.getDisplayOptions() & ActionBar.DISPLAY_SHOW_TITLE) == 1)) {
				// Not showing home, put tabs on top.
				final int count = getChildCount();
				for (int i = 0; i < count; i++) {
					final View child = getChildAt(i);

					if (child == mTabContainer)
						continue;

					if (!mActionBarView.isCollapsed()) {
						child.offsetTopAndBottom(tabHeight);
					}
				}
				mTabContainer.layout(l, 0, r, tabHeight);
			} else {
				if (mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND
						|| mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) {
					mTabContainer.layout(l, containerHeight - tabHeight - shadowHeight, r, containerHeight);
				} else {
					mTabContainer.layout(l, containerHeight - tabHeight, r, containerHeight);
				}
			}
		}

		boolean needsInvalidate = false;
		if (mIsSplit) {
			if (mSplitBackground != null) {
				mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
				needsInvalidate = true;
			}
		} else {
			if (mBackground != null) {
				mBackground.setBounds(mActionBarView.getLeft(), mActionBarView.getTop(), mActionBarView.getRight(),
						mActionBarView.getBottom());
				needsInvalidate = true;
			}
			if ((mIsStacked = hasTabs && mStackedBackground != null)) {
				if (mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND
						|| mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) {
					mStackedBackground.setBounds(
							getLeft(),
							getTop(),
							getRight(),
							getBottom()
									- (mTabContainer.getTabWaveEnable() ? (int) getResources().getDimension(
											R.dimen.tws_actionbartab_overlay_offset) : 0));
				} else if (mTabContainer.getTabMode() == ActionBar.ACTIONBAR_TAB_OVERLAY) {
					mStackedBackground.setBounds(getLeft(), getTop(), getRight(), mTabContainer.getBottom());
				} else {
					mStackedBackground.setBounds(mTabContainer.getLeft(), getTop(), getRight(),
							mTabContainer.getBottom());
				}
				needsInvalidate = true;
			}
		}

		if (needsInvalidate) {
			invalidate();
		}
	}

	public void twsSetTabTextSelectChange(boolean change) {
		if (mTabContainer != null) {
			mTabContainer.twsSetTabTextSelectChange(change);
		}
		requestLayout();
	}

	public void twsSetTabLeftView(View view) {
		if (mTabContainer != null) {
			mTabContainer.twsSetTabLeftView(view);
		}
		requestLayout();
	}

	public void twsSetTabRightView(View view) {
		if (mTabContainer != null) {
			mTabContainer.twsSetTabRightView(view);
		}
		requestLayout();
	}

	public void twsSetTabCustomEnd() {
		if (mTabContainer != null) {
			mTabContainer.twsSetTabCustomEnd();
		}
		requestLayout();
	}
}
