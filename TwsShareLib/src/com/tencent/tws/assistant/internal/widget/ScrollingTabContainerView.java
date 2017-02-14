/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.TwsActivity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class ScrollingTabContainerView extends HorizontalScrollView
        implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "ScrollingTabContainerView";
    Runnable mTabSelector;
    private TabClickListener mTabClickListener;
    private int mTabMode = ActionBar.ACTIONBAR_TAB_STANDARD;
    private LinearLayout mTabLayout;
    private RelativeLayout mTabOutLayout;
    private Spinner mTabSpinner;
    private boolean mAllowCollapse;

    int mMaxTabWidth;
    private int mContentHeight;
    private int mSelectedTabIndex;

    protected Animator mVisibilityAnim;
    protected final VisibilityAnimListener mVisAnimListener = new VisibilityAnimListener();

    private static final TimeInterpolator sAlphaInterpolator = new DecelerateInterpolator();

    private static final int FADE_DURATION = 200;
    
    private ActionBarContextView mContextView;


	// tws-start add smooth scroll feature to tab::2014-11-18
    private boolean mInit = false;
    private int mSelectedColor = 0;
    private int mNormalColor = 0;
    private ColorStateList mNormalColors;
	private Rect mIndicatorRect = new Rect();
	private Drawable mIndicator = null;
	private boolean  mDrawIndicator = false;

	ArrayList<Drawable> mDrawableDepot = new ArrayList<Drawable>();
	// tws-end add smooth scroll feature to tab ::2014-11-18
	
	private HorizontalWaveView mWaveView;
	private int mWScrollCnt;
	private int mAmplitude;
	private int mBeforeTabPostion;
	private int mWaveHeight;
	private Drawable mStackDrawable;
	private boolean mTabWaveEnable;
	private float mAnimStart, mAnimEnd;
	private boolean mHasPageScrolled, mHasPageSelected, mPageSelectedNeedReset;
	private float mIndicatorViewOffset;
	private boolean mTabTextSelectChange;
	private View mLeftView, mRightView;
	private boolean mTabButtonEnable;
	private boolean mFirstBuildTab = true;
	
	private int mActionBarTabTheme;
	private boolean mActionBarTabThemeWave;
	private Animator mAMAnimator;
	
    public ScrollingTabContainerView(Context context) {
    	this(context, ActionBar.ACTIONBAR_TAB_STANDARD, false, false);
    }

    public ScrollingTabContainerView(Context context, int tabMode, boolean hasButton, boolean tabWaveEnable) {
    	super(context);
    	
    	mTabMode = tabMode;
    	mTabButtonEnable = hasButton;
    	mTabWaveEnable = tabWaveEnable;
    	
        setHorizontalScrollBarEnabled(false);

        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionBar_height, 0));

        
        mActionBarTabThemeWave = ThemeUtils.isActionBarTabStyleWave(context);
        mActionBarTabTheme = a.getInt(R.styleable.ActionBar_actionbartabTheme, ThemeUtils.ACTIONBARTAB_THEME_NORMAL);
        
        if (!(mActionBarTabThemeWave && mActionBarTabTheme == ThemeUtils.ACTIONBARTAB_THEME_WAVE)) {
        	mTabWaveEnable = false;
        }
        
        setTabContentHeight(mTabMode);
        
        
        a.recycle();

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY || mTabMode == ActionBar.ACTIONBAR_TAB_STANDARD) {
        	mWaveHeight = mContentHeight-getPaddingTop();
        }
        else if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND || mTabMode == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) {
        	mWaveHeight = mContentHeight+(int)getResources().getDimension(R.dimen.tws_action_bar_shadow_height);
        }
        mWaveView = new HorizontalWaveView(context, mWaveHeight);
        if (!mTabWaveEnable)
        	mWaveView.setVisibility(View.GONE);
        mIndicatorViewOffset = getResources().getDimension(R.dimen.tws_actionbartab_overlay_offset);
        mAnimStart = mWaveHeight + (int)getResources().getDimension(R.dimen.tws_actionbartab_second_overlay_padding);
        mAnimEnd = mAnimStart - mIndicatorViewOffset;
        frameLayout.addView(mWaveView);
        mAMAnimator = ObjectAnimator.ofInt(mWaveView, AM_VALUE, 5, 0);
        mAMAnimator.setDuration(500);
        
        if (hasButton) {
        	mTabLayout = createTabInternalLayout();
        	mTabOutLayout = createTabRelativeLayout();
        }
        else {
        	mTabLayout = createTabLayout();
        }
        
        if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY) {
        	mAnimStart = mWaveHeight 
        			- (tabWaveEnable ? (int)getResources().getDimension(R.dimen.tws_actionbartab_overlay_padding_tab) 
        					: (int)getResources().getDimension(R.dimen.tws_actionbartab_overlay_padding));
            mAnimEnd = mAnimStart - mIndicatorViewOffset;
        	mTabLayout.setPadding(0, TwsActivity.getStatusBarHeight(), 0, 0);
        }
        
        if (hasButton) {
        	frameLayout.addView(mTabOutLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        			ViewGroup.LayoutParams.MATCH_PARENT));
        }
        else {
        	frameLayout.addView(mTabLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        addView(frameLayout);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final boolean lockedExpanded = widthMode == MeasureSpec.EXACTLY;
        setFillViewport(lockedExpanded);

        final int childCount = mTabLayout.getChildCount();
        if (childCount > 1 &&
                (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST)) {
            if (childCount > 2) {
                mMaxTabWidth = (int) (MeasureSpec.getSize(widthMeasureSpec) * 0.4f);
            } else {
                mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
            }
        } else {
            mMaxTabWidth = -1;
        }

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY);

        final boolean canCollapse = !lockedExpanded && mAllowCollapse;

        if (canCollapse) {
            // See if we should expand
            mTabLayout.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec);
            if (mTabLayout.getMeasuredWidth() > MeasureSpec.getSize(widthMeasureSpec)) {
                performCollapse();
            } else {
                performExpand();
            }
        } else {
            performExpand();
        }

        final int oldWidth = getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int newWidth = getMeasuredWidth();

        if (lockedExpanded && oldWidth != newWidth) {
            // Recenter the tab display if we're at a new (scrollable) size.
            setTabSelected(mSelectedTabIndex);
        }
        
        if (mContextView != null && mTabMode != ActionBar.ACTIONBAR_TAB_STANDARD) {
            mContextView.setContentHeight(mContentHeight);
            mContextView.setPadding(0, getPaddingTop()+TwsActivity.getStatusBarHeight(), 0, 0);
        }
    }
    
    public void setContextView(ActionBarContextView view) {
        mContextView = view;
    }

    /**
     * Indicates whether this view is collapsed into a dropdown menu instead
     * of traditional tabs.
     * @return true if showing as a spinner
     */
    private boolean isCollapsed() {
        return mTabSpinner != null && mTabSpinner.getParent() == this;
    }

    public void setAllowCollapse(boolean allowCollapse) {
        mAllowCollapse = allowCollapse;
    }

    private void performCollapse() {
        if (isCollapsed()) return;

        if (mTabSpinner == null) {
            mTabSpinner = createSpinner();
        }
        removeView(mTabLayout);
        addView(mTabSpinner, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (mTabSpinner.getAdapter() == null) {
            mTabSpinner.setAdapter(new TabAdapter());
        }
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
            mTabSelector = null;
        }
        mTabSpinner.setSelection(mSelectedTabIndex);
    }

    private boolean performExpand() {
        if (!isCollapsed()) return false;

        removeView(mTabSpinner);
        addView(mTabLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setTabSelected(mTabSpinner.getSelectedItemPosition());
        return false;
    }

    public void setTabSelected(int position) {
        mSelectedTabIndex = position;
        final int tabCount = mTabLayout.getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = mTabLayout.getChildAt(i);
            final boolean isSelected = i == position;
            child.setSelected(isSelected);
            if (isSelected) {
                animateToTab(position);
                if (mFirstBuildTab) {
                	((TabView)child).twsShowIndicatorViewEnd();
                }
                else {
                	if (!ActionBar.mIsInActionMode)
                		((TabView)child).twsShowIndicatorView();
                	else
                		((TabView)child).twsShowIndicatorViewEnd();
                }
            }
            else {
            	((TabView)child).twsHideIndicatorViewEnd();
            }
        }
    }

    public void setContentHeight(int contentHeight) {
        mContentHeight = contentHeight;
        requestLayout();
    }
    
    private class TabLayout extends LinearLayout {
        public TabLayout(Context context) {
            this(context, null);
        }

        public TabLayout(Context context, AttributeSet attrs) {
            this(context, attrs, com.android.internal.R.attr.actionBarTabBarStyle);
        }

        public TabLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
        	super.onDraw(canvas);

			if (mIndicator != null && mInit && mDrawIndicator)
			{
				mIndicator.setBounds(mIndicatorRect.left, mIndicatorRect.top, mIndicatorRect.right, mIndicatorRect.bottom);
        		mIndicator.draw(canvas);
			}
        }
    };
    
    private LinearLayout createTabLayout() {
        final LinearLayout tabLayout = new TabLayout(getContext(), null,
                com.android.internal.R.attr.actionBarTabBarStyle);
        tabLayout.setMeasureWithLargestChildEnabled(true);
        tabLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return tabLayout;
    }

    
    private class TabRelativeLayout extends RelativeLayout {
        public TabRelativeLayout(Context context) {
            this(context, null);
        }

        public TabRelativeLayout(Context context, AttributeSet attrs) {
        	super(context, attrs);
        }

        public TabRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
        	super.onDraw(canvas);

			if (mIndicator != null && mInit && mDrawIndicator)
			{
				mIndicator.setBounds(mIndicatorRect.left, mIndicatorRect.top, mIndicatorRect.right, mIndicatorRect.bottom);
        		mIndicator.draw(canvas);
			}
        }
    };

    private RelativeLayout createTabRelativeLayout() {
        final RelativeLayout tabLayout = new TabRelativeLayout(getContext());
        tabLayout.setLayoutParams(new RelativeLayout.LayoutParams(
        		RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        return tabLayout;
    }
    
    private LinearLayout createTabInternalLayout() {
        final LinearLayout tabLayout = new LinearLayout(getContext(), null,
                com.android.internal.R.attr.actionBarTabBarStyle);
        tabLayout.setMeasureWithLargestChildEnabled(true);
        tabLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return tabLayout;
    }

    private Spinner createSpinner() {
        final Spinner spinner = new Spinner(getContext(), null,
                com.android.internal.R.attr.actionDropDownStyle);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
        spinner.setOnItemSelectedListener(this);
        return spinner;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar,
                com.android.internal.R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionBar_height, 0));
        
        setTabContentHeight(mTabMode);
        
        a.recycle();
    }

    public void animateToVisibility(int visibility) {
        if (mVisibilityAnim != null) {
            mVisibilityAnim.cancel();
        }
        if (visibility == VISIBLE) {
            if (getVisibility() != VISIBLE) {
                setAlpha(0);
            }
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "alpha", 1);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);

            anim.addListener(mVisAnimListener.withFinalVisibility(visibility));
            anim.start();
        } else {
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "alpha", 0);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);

            anim.addListener(mVisAnimListener.withFinalVisibility(visibility));
            anim.start();
        }
    }

    public void animateToTab(final int position) {
        final View tabView = mTabLayout.getChildAt(position);
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
        mTabSelector = new Runnable() {
            public void run() {
//                final int scrollPos = tabView.getLeft() - (getWidth() - tabView.getWidth()) / 2;
//                smoothScrollTo(scrollPos, 0);
                mTabSelector = null;
            }
        };
        post(mTabSelector);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mTabSelector != null) {
            // Re-post the selector we saved
            post(mTabSelector);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
    }

    private TabView createTabView(ActionBar.Tab tab, boolean forAdapter) {
        final TabView tabView = new TabView(getContext(), tab, forAdapter);
        if (forAdapter) {
        	if (android.os.Build.VERSION.SDK_INT > 15) {
        		tabView.setBackground(null);
        	}
        	else {
        		tabView.setBackgroundDrawable(null);
        	}
            tabView.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    mContentHeight));
        } else {
            tabView.setFocusable(true);

            if (mTabClickListener == null) {
                mTabClickListener = new TabClickListener();
            }
            tabView.setOnClickListener(mTabClickListener);
        }
        return tabView;
    }
    
    public void enableTabClick(boolean enable) {
    	final int tabCount = mTabLayout.getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = mTabLayout.getChildAt(i);
            child.setClickable(enable);
        }
    }

    public void addTab(ActionBar.Tab tab, boolean setSelected) {
        TabView tabView = createTabView(tab, false);
        mTabLayout.addView(tabView, new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1));
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (setSelected) {
            tabView.setSelected(true);
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }
    
    public void addTabInternal(ActionBar.Tab tab, boolean setSelected) {
        TabView tabView = createTabView(tab, false);
        mTabLayout.addView(tabView, new LinearLayout.LayoutParams(0,
                LayoutParams.MATCH_PARENT, 1));
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (setSelected) {
            tabView.setSelected(true);
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }
    
    public void addTabLayout() {
    	RelativeLayout.LayoutParams params0 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
    	params0.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        if (mLeftView != null) {
        	mTabOutLayout.addView(mLeftView, params0);
        	if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY) {
        		mLeftView.setPadding(0, TwsActivity.getStatusBarHeight(), 0, 0);
        	}
        	mLeftView.measure(0, 0);
        }
        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
    	params1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        if (mRightView != null) {
        	mTabOutLayout.addView(mRightView, params1);
        	if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY) {
        		mRightView.setPadding(0, TwsActivity.getStatusBarHeight(), 0, 0);
        	}
        	mRightView.measure(0, 0);
        }
        final int screenWidth = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
        int leftViewWidth = 0;
        int rightViewWidth = 0;
        if (mLeftView != null) {
        	leftViewWidth = mLeftView.getMeasuredWidth();
        }
        else {
        	leftViewWidth = (int) getResources().getDimension(R.dimen.tws_tab_customview_minwidth);
        }
        if (mRightView != null) {
        	rightViewWidth = mRightView.getMeasuredWidth();
        }
        else {
        	rightViewWidth = (int) getResources().getDimension(R.dimen.tws_tab_customview_minwidth);
        }
        
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams((screenWidth-leftViewWidth-rightViewWidth),
        		LayoutParams.MATCH_PARENT);
        params2.addRule(RelativeLayout.CENTER_IN_PARENT);
        if (mTabLayout != null) {
        	mTabOutLayout.addView(mTabLayout, params2);
        }
    }
    
    public void addTab(ActionBar.Tab tab, int position, boolean setSelected) {
        final TabView tabView = createTabView(tab, false);
        mTabLayout.addView(tabView, position, new LinearLayout.LayoutParams(
                0, LayoutParams.MATCH_PARENT, 1));
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (setSelected) {
            tabView.setSelected(true);
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }

    public void updateTab(int position) {
        ((TabView) mTabLayout.getChildAt(position)).update();
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }

    public void removeTabAt(int position) {
        mTabLayout.removeViewAt(position);
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }

    public void removeAllTabs() {
        mTabLayout.removeAllViews();
        if (mTabSpinner != null) {
            ((TabAdapter) mTabSpinner.getAdapter()).notifyDataSetChanged();
        }
        if (mAllowCollapse) {
            requestLayout();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        TabView tabView = (TabView) view;
        tabView.getTab().select();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public boolean onTouchEvent(MotionEvent arg0) {
    	// TODO Auto-generated method stub
    	return false;
    }
    
    public void twsSetPageSelected(int position) {
    	final TabView beforeTabView = (TabView)(mTabLayout.getChildAt(mBeforeTabPostion));
    	final TabView afterTabView = (TabView)(mTabLayout.getChildAt(position));
    	if (!mHasPageScrolled && !ActionBar.mIsInActionMode) {
    		if (!mFirstBuildTab) {
    			mWaveView.setAmplitude(5);
    		}
    		beforeTabView.twsHideIndicatorView();
   			afterTabView.twsShowIndicatorView();
    		mHasPageSelected = true;
    		mPageSelectedNeedReset = true;
    	}
    	mBeforeTabPostion = position;
	}
    
	// tws-start add smooth scroll feature to tab::2014-11-18	
    public void twsSetPageScroll(int position, float positionOffset) {

    	if (mTabLayout == null)
    		return;
    	
    	final int tabCount = mTabLayout.getChildCount();

		//check illege sate
		if (positionOffset < 0 || positionOffset > 1)
			return;
		
		if( mSelectedTabIndex < 0 || mSelectedTabIndex > tabCount - 1) 
			return;
		
    	if( position < 0 || position > tabCount - 1 ) 
			return;
		//illege state end

		if (positionOffset == 0.0 || positionOffset == 1.0 || position == tabCount - 1) {
			cancelScrollState();
			if (mFirstBuildTab) {
				mWaveView.setAmplitude(0);
				mFirstBuildTab = false;
			}
			else {
				if (mWScrollCnt != 0 || mPageSelectedNeedReset) {
					if (!mAMAnimator.isRunning())
						mAMAnimator.start();
					mPageSelectedNeedReset = false;
				}
			}
			mWScrollCnt = 0;
			return;
		}
		
		mWScrollCnt++;
	
       	final TabView selectedTabView = (TabView)(mTabLayout.getChildAt(mSelectedTabIndex));

		int normalIndex = 0;
		if (normalIndex == mSelectedTabIndex)
    		normalIndex = 1;

		
		final TabView normalTabView = (TabView)(mTabLayout.getChildAt(normalIndex));
       
       
       	if ( mInit == false) {
    		//mIndicator = getResources().getDrawable(R.drawable.tab_selected_holo);
    		if (selectedTabView.getBackground() != null) {
				if ( selectedTabView.getBackground() instanceof StateListDrawable ) {
					//mIndicator = selectedTabView.getBackground().getCurrent();
					int index = ((StateListDrawable)selectedTabView.getBackground()).getStateDrawableIndex(new int[]{android.R.attr.state_selected});
					mIndicator = ((StateListDrawable)selectedTabView.getBackground()).getStateDrawable(index);
				}
				else
					mIndicator = selectedTabView.getBackground();
			}
    		
    		if (mTabTextSelectChange) {
    			mSelectedColor = selectedTabView.twsGetTextColor();
    			mNormalColor = normalTabView.twsGetTextColor();
    			mNormalColors = normalTabView.twsGetTextColors();
    		}

            mInit = true;
    	}
    	
    	final TabView leftTabView = (TabView)(mTabLayout.getChildAt(position));
    	final TabView rightTabView = (TabView)(mTabLayout.getChildAt(position + 1));

		if( mDrawableDepot.isEmpty() || tabCount != mDrawableDepot.size() ) {
			mDrawableDepot.clear();
			for (int i = 0; i < tabCount; i++) {
            	TabView child = (TabView)mTabLayout.getChildAt(i);
				mDrawableDepot.add(child.getBackground());
			}
		}
    	
		if (mTabTextSelectChange) {
			float la = ((float)( (mSelectedColor >> 24) & 0xFF )) * (1 - positionOffset) + ((float)((mNormalColor >> 24) & 0xFF)) * positionOffset;
			float lr = ((float) ( (mSelectedColor >> 16) & 0xFF )) * (1 - positionOffset) + ((float)((mNormalColor >> 16) & 0xFF)) * positionOffset;
			float lg = ((float)( (mSelectedColor >> 8) & 0xFF )) * (1 - positionOffset) + ((float)((mNormalColor >> 8) & 0xFF)) * positionOffset;
			float lb = ((float)( (mSelectedColor >> 0) & 0xFF )) * (1 - positionOffset) + ((float)((mNormalColor >> 0) & 0xFF)) * positionOffset;
			
			float ra = ((float)( (mSelectedColor >> 24) & 0xFF )) * positionOffset + ((float)((mNormalColor >> 24) & 0xFF)) * (1 - positionOffset) ;
			float rr = ((float)( (mSelectedColor >> 16) & 0xFF )) * positionOffset + ((float)((mNormalColor >> 16) & 0xFF)) * (1 - positionOffset) ;
			float rg = ((float)( (mSelectedColor >> 8) & 0xFF )) * positionOffset + ((float)((mNormalColor >> 8) & 0xFF)) * (1 - positionOffset) ;
			float rb = ((float)( (mSelectedColor >> 0) & 0xFF )) * positionOffset + ((float)((mNormalColor >> 0) & 0xFF)) * (1 - positionOffset) ;
			
			leftTabView.twsSetTextColor(android.graphics.Color.argb((int)la, (int)lr, (int)lg, (int)lb));
			rightTabView.twsSetTextColor(android.graphics.Color.argb((int)ra, (int)rr, (int)rg, (int)rb));
		}
    	
    	if (!mHasPageSelected) {
    		if (mWScrollCnt < 16 && mWScrollCnt % 3 == 0) {
    			mWaveView.setAmplitude(mAmplitude++);
    		}
    		leftTabView.twsSetIndicatorAlpha(1.0f-positionOffset);
    		rightTabView.twsSetIndicatorAlpha(positionOffset);
    		leftTabView.twsSetIndicatorTransY(mAnimEnd+mIndicatorViewOffset*positionOffset);
    		rightTabView.twsSetIndicatorTransY(mAnimStart-mIndicatorViewOffset*positionOffset);
    		mHasPageScrolled = true;
    	}
    	
    	float leftX = leftTabView.getX();
    	float leftY = leftTabView.getY();
    	int leftWidth = leftTabView.getWidth();
    	int leftHeight = leftTabView.getHeight();
    	
    	float rightX = rightTabView.getX();
    	float rightY = rightTabView.getY();
    	int rightWidth = rightTabView.getWidth();
    	int rightHeight = rightTabView.getHeight();
    	
    	mIndicatorRect.left = (int)(leftX * (1.0 - positionOffset) + rightX * positionOffset);
    	mIndicatorRect.top = (int)(leftY + rightY) / 2;
    	mIndicatorRect.right = (int)((leftX + (float)leftWidth) * (1.0 - positionOffset) + (rightX + (float)rightWidth) * positionOffset);
    	mIndicatorRect.bottom = mIndicatorRect.top + (leftHeight + rightHeight) / 2;
    	
    	mTabLayout.invalidate();
    	if (android.os.Build.VERSION.SDK_INT > 15) {
    		leftTabView.setBackground(null);
    		rightTabView.setBackground(null);
    	}
    	else {
    		leftTabView.setBackgroundDrawable(null);
    		rightTabView.setBackgroundDrawable(null);
    	}

		mDrawIndicator = true;
    	
    }
	public void twsSetScrollEnd() {
		cancelScrollState();
	}

	private void cancelScrollState( ) {
		final int tabCount = mTabLayout.getChildCount();
		
        for (int i = 0; i < tabCount; i++) {
            final TabView child = (TabView)mTabLayout.getChildAt(i);
			if (child != null) {
				if (mTabTextSelectChange) {
					child.twsSetTextColors(mNormalColors);
				}
				if ( i < mDrawableDepot.size() ) {
					if (android.os.Build.VERSION.SDK_INT > 15) {
						child.setBackground((Drawable)mDrawableDepot.get(i));
					}
					else {
						child.setBackgroundDrawable((Drawable)mDrawableDepot.get(i));
					}
					child.refreshDrawableState();
					
					if (i == mSelectedTabIndex) {
						child.setSelected(false);
						child.setSelected(true);
						if (!mHasPageSelected) {
							child.twsSetIndicatorTransY(mAnimEnd);
							child.twsSetIndicatorAlpha(1.0f);
						}
					}
					else {
						child.setSelected(true);
						child.setSelected(false);
						if (!mHasPageSelected) {
							child.twsSetIndicatorTransY(mAnimStart);
							child.twsSetIndicatorAlpha(0.0f);
						}
					}
					
				}
			}
        }

		mDrawableDepot.clear();
		mDrawIndicator = false;
		mHasPageScrolled = false;
		mHasPageSelected = false;
		mAmplitude = 0;
		mTabLayout.invalidate();
	}
	
    // tws-end add smooth scroll feature to tab ::2014-11-18
    
    private class TabView extends FrameLayout {
        private ActionBar.Tab mTab;
        private TextView mTextView;
        private ImageView mIconView;
        private View mCustomView;
        private ImageView mIndicatorView;
        
        private AnimatorSet mShowIndicatorAnim, mHideIndicatorAnim;
        
        private AnimatorListener mShowIndicatorListener = new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				enableTabClick(true);
				if (mIndicatorView != null) {
					mIndicatorView.setTranslationY(mAnimEnd);
					mIndicatorView.setAlpha(1.0f);
					if (!mAMAnimator.isRunning() && !mFirstBuildTab && mWaveView.getAmplitude() == 5) {
						mAMAnimator.start();
					}
				}
			}
        };
        
        private AnimatorListener mHideIndicatorListener = new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				enableTabClick(true);
				if (mIndicatorView != null) {
					mIndicatorView.setTranslationY(mAnimStart);
					mIndicatorView.setAlpha(0.0f);
					if (!mAMAnimator.isRunning() && !mFirstBuildTab && mWaveView.getAmplitude() == 5) {
						mAMAnimator.start();
					}
				}
			}
		};
        
        public TabView(Context context, ActionBar.Tab tab, boolean forList) {
            super(context, null, ((mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND ||mTabMode == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) ? 
            		R.attr.actionBarSubTabStyle : R.attr.actionBarTabStyle));
            mTab = tab;
            
            if (forList) {
//                setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            }

            update();
        }
        
        public int twsGetTextColor() {
        	if (mTextView == null)
        		return 0;
        	return mTextView.getCurrentTextColor(); 
        }
        
        public ColorStateList twsGetTextColors() {
        	if (mTextView == null)
        		return null;
        	return mTextView.getTextColors(); 
        }
        
        public void twsSetTextColor(int color) {
        	if (mTextView == null)
        		return;
        	mTextView.setTextColor(color); 
        }
        
        public void twsSetTextColors(ColorStateList colors) {
        	if (mTextView == null || colors == null)
        		return;
        	mTextView.setTextColor(colors); 
        }
        
        public void twsShowIndicatorView() {
        	if (mIndicatorView == null) {
        		return;
        	}
        	if (mIndicatorView.getTranslationY() < mAnimStart && mIndicatorView.getTranslationY() > mAnimEnd) {
        		return;
        	}
        	if (mShowIndicatorAnim != null && mShowIndicatorAnim.isRunning() && !mFirstBuildTab) {
        		enableTabClick(false);
        		return;
        	}
        	if (mShowIndicatorAnim == null) {
        		mShowIndicatorAnim = new AnimatorSet();
        	}
        	AnimatorSet.Builder b = mShowIndicatorAnim.play(ObjectAnimator.ofFloat(mIndicatorView, "alpha", 0, 1));
        	b.with(ObjectAnimator.ofFloat(mIndicatorView, "translationY", mAnimStart, mAnimEnd));
        	mShowIndicatorAnim.setDuration(300);
        	mShowIndicatorAnim.start();
        	mShowIndicatorAnim.addListener(mShowIndicatorListener);
        }
        
        public void twsHideIndicatorView() {
        	if (mIndicatorView == null) {
        		return;
        	}
        	if (mIndicatorView.getTranslationY() < mAnimStart && mIndicatorView.getTranslationY() > mAnimEnd) {
        		return;
        	}
        	if (mHideIndicatorAnim != null && mHideIndicatorAnim.isRunning() && !mFirstBuildTab) {
        		enableTabClick(false);
        		return;
        	}
        	if (mHideIndicatorAnim == null) {
        		mHideIndicatorAnim = new AnimatorSet();
        	}
        	AnimatorSet.Builder b = mHideIndicatorAnim.play(ObjectAnimator.ofFloat(mIndicatorView, "alpha",1, 0));
        	b.with(ObjectAnimator.ofFloat(mIndicatorView, "translationY",mAnimEnd, mAnimStart));
        	mHideIndicatorAnim.setDuration(300);
        	mHideIndicatorAnim.start();
        	mHideIndicatorAnim.addListener(mHideIndicatorListener);
        }
        
        public void twsShowIndicatorViewEnd() {
        	mShowIndicatorListener.onAnimationEnd(null);
        }
        
        public void twsHideIndicatorViewEnd() {
        	mHideIndicatorListener.onAnimationEnd(null);
        }
        
        public void twsSetIndicatorTransY(float transY) {
        	if (mIndicatorView == null)
        		return;
        	mIndicatorView.setTranslationY(transY);
        }
        
        public void twsSetIndicatorAlpha(float alpha) {
        	if (mIndicatorView == null)
        		return;
        	mIndicatorView.setAlpha(alpha);
        }
        
        public void bindTab(ActionBar.Tab tab) {
            mTab = tab;
            update();
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Re-measure if we went beyond our maximum size.
            if (mMaxTabWidth > 0 && getMeasuredWidth() > mMaxTabWidth) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(mMaxTabWidth, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            }
        }

        public void update() {
            final ActionBar.Tab tab = mTab;
            final View custom = tab.getCustomView();
            if (custom != null) {
                final ViewParent customParent = custom.getParent();
                if (customParent != this) {
                    if (customParent != null) ((ViewGroup) customParent).removeView(custom);
                    addView(custom);
                }
                mCustomView = custom;
                if (mTextView != null) mTextView.setVisibility(GONE);
                if (mIconView != null) {
                    mIconView.setVisibility(GONE);
                    mIconView.setImageDrawable(null);
                }
            } else {
                if (mCustomView != null) {
                    removeView(mCustomView);
                    mCustomView = null;
                }

                final Drawable icon = tab.getIcon();
                final CharSequence text = tab.getText();

                if (icon != null) {
                    if (mIconView == null) {
                        ImageView iconView = new ImageView(getContext());
                        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                        lp.gravity = Gravity.CENTER_VERTICAL;
                        iconView.setLayoutParams(lp);
                        addView(iconView, 0);
                        mIconView = iconView;
                    }
                    mIconView.setImageDrawable(icon);
                    mIconView.setVisibility(VISIBLE);
                } else if (mIconView != null) {
                    mIconView.setVisibility(GONE);
                    mIconView.setImageDrawable(null);
                }

                if (text != null) {
                    if (mTextView == null) {
                    	TextView textView = null;
                    	if (mTabMode == ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND 
                        		||mTabMode == ActionBar.ACTIONBAR_TAB_STANDARD_SECOND) {
                    		textView = new TextView(getContext(), null,
                    				R.attr.actionBarSubTabTextStyle);
                    	}
                    	else {
                    		textView = new TextView(getContext(), null, 
                    				R.attr.actionBarTabTextStyle);
                    	}
                        addView(textView);
                        mTextView = textView;
                    }
                    mTextView.setText(text);
                    mTextView.setVisibility(VISIBLE);
                } else if (mTextView != null) {
                    mTextView.setVisibility(GONE);
                    mTextView.setText(null);
                }
                
                	ImageView indicator = new ImageView(getContext());
                	LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                	lp.gravity = Gravity.CENTER_HORIZONTAL;
                	indicator.setLayoutParams(lp);
                	indicator.setImageDrawable(getResources().getDrawable(R.drawable.tab_wave_indicator));
                	indicator.setTranslationY(mAnimStart);
                	indicator.setAlpha(0.0f);
                	addView(indicator);
                	if (mActionBarTabThemeWave && mActionBarTabTheme == ThemeUtils.ACTIONBARTAB_THEME_WAVE) {
                		mIndicatorView = indicator;
                	}

                if (mIconView != null) {
                    mIconView.setContentDescription(tab.getContentDescription());
                }
            }
        }

        public ActionBar.Tab getTab() {
            return mTab;
        }
    }

    private class TabAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mTabLayout.getChildCount();
        }

        @Override
        public Object getItem(int position) {
            return ((TabView) mTabLayout.getChildAt(position)).getTab();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createTabView((ActionBar.Tab) getItem(position), true);
            } else {
                ((TabView) convertView).bindTab((ActionBar.Tab) getItem(position));
            }
            return convertView;
        }
    }

    private class TabClickListener implements OnClickListener {
        public void onClick(View view) {
            TabView tabView = (TabView) view;
            tabView.getTab().select();
            final int tabCount = mTabLayout.getChildCount();
            for (int i = 0; i < tabCount; i++) {
                final View child = mTabLayout.getChildAt(i);
                child.setSelected(child == view);
            }
        }
    }

    protected class VisibilityAnimListener implements Animator.AnimatorListener {
        private boolean mCanceled = false;
        private int mFinalVisibility;

        public VisibilityAnimListener withFinalVisibility(int visibility) {
            mFinalVisibility = visibility;
            return this;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            setVisibility(VISIBLE);
            mVisibilityAnim = animation;
            mCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mCanceled) return;

            mVisibilityAnim = null;
            setVisibility(mFinalVisibility);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    public void setTabContentHeight(int tabMode) {
    	int height = 0;
        switch (tabMode) {
            case ActionBar.ACTIONBAR_TAB_STANDARD:
            	if (getResources().getBoolean(R.bool.config_statusbar_state)) {
            		if (mTabWaveEnable) {
            			height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_tab_height);
            		}
            		else {
            			height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_height) + TwsActivity.getStatusBarHeight();
            		}
            	}
            	else {
            		height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_height);
            	}
                setContentHeight(height);
                break;
            case ActionBar.ACTIONBAR_TAB_OVERLAY:
            	if (mTabWaveEnable) {
            		height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_tab_height);
            	}
            	else {
            		height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_height);
            	}
                setContentHeight(height);
                break;
            case ActionBar.ACTIONBAR_TAB_OVERLAY_SECOND:
            case ActionBar.ACTIONBAR_TAB_STANDARD_SECOND:
            	height = (int) getContext().getResources().getDimension(R.dimen.tws_action_bar_tab_second_height);
                setContentHeight(height);
                break;

            default:
                break;
        }
    }

    public int getTabMode(){
        return mTabMode;
    }

    public void setStackedDrawable(Drawable drawable) {
		mWaveView.setStackedDrawable(drawable);
	}
    
    public boolean getTabWaveEnable() {
    	return mTabWaveEnable;
    }
    
    public void twsSetTabTextSelectChange(boolean change) {
    	mTabTextSelectChange = change;
    }
    
    public void twsSetTabLeftView(View view) {
    	mLeftView = view;
    }
    
    public void twsSetTabRightView(View view) {
    	mRightView = view;
    }
    
    public void twsSetTabCustomEnd() {
    	if (mTabButtonEnable && mTabOutLayout.getChildCount() == 0) {
			addTabLayout();
		}
    }
    
    private static final IntProperty<HorizontalWaveView> AM_VALUE = new IntProperty<HorizontalWaveView>("amValue") {

		@Override
		public void setValue(HorizontalWaveView object, int value) {
			object.setAmplitude(value);
		}

		@Override
		public Integer get(HorizontalWaveView object) {
			return null;
		}
    };
}
