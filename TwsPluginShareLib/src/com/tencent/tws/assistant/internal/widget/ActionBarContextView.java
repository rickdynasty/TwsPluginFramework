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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuView;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.ToggleButton;
import com.tencent.tws.sharelib.R;


/**
 * @hide
 */
public class ActionBarContextView extends AbsActionBarView implements AnimatorListener {
    private static final String TAG = "ActionBarContextView";

    private CharSequence mTitle;
    private CharSequence mSubtitle;

    private Button mClose;
    private ToggleButton mMulti;
    private View mCustomView;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private ActionMode mActionMode = null;
    private int mContextSplitHeight = 0;
    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private int mMultiStyleRes;
    private int mCloseStyleRes;
    private Drawable mCloseDrawable;
    private Drawable mSplitBackground;

    private Animator mCurrentAnimation;
    private boolean mAnimateInOnLayout;
    private int mAnimationMode;

    private static final int ANIMATE_IDLE = 0;
    private static final int ANIMATE_IN = 1;
    private static final int ANIMATE_OUT = 2;

    //tws-start ActionMode BackBtn OnClickListener::2014-8-7
    private OnClickListener mActionModeBackOnClickListener = null;
    //tws-end ActionMode BackBtn OnClickListener::2014-8-7
    
    private boolean firstMeasureTitle = true;
    
    public ActionBarContextView(Context context) {
        this(context, null);
    }
    
    public ActionBarContextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.actionModeStyle);
    }
    
    public ActionBarContextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionMode, defStyle, 0);
        Drawable background = a.getDrawable(R.styleable.ActionMode_backgroundActionMode);
        int isActionBarTheme = a.getInt(R.styleable.ActionMode_backgroundMode, ThemeUtils.ACTIONBAR_BACKGROUND_NORMAL);
        boolean isActionBarBgGradient = ThemeUtils.isActionBarBackgroundGradient(context);
        if (isActionBarBgGradient && isActionBarTheme == ThemeUtils.ACTIONBAR_BACKGROUND_GRADIENT) {
            final int startColor = a.getColor(R.styleable.ActionMode_gradientBackgroundStartColor,
                    R.color.actionbar_gradient_background_start_color);
            final int endColor = a.getColor(R.styleable.ActionMode_gradientBackgroundEndColor,
                    R.color.actionbar_gradient_background_end_color);
            int gradientColors[] = new int[]{startColor, endColor};
            background = new GradientDrawable(Orientation.LEFT_RIGHT, gradientColors);
        }
        setBackgroundDrawable(background);
        mTitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_titleTextStyle, 0);
        mSubtitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_subtitleTextStyle, 0);
        
        TypedArray aTyped = context.obtainStyledAttributes(attrs, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        mMultiStyleRes = aTyped.getResourceId(R.styleable.ActionBar_actionbarrightbtnstyle, 0);
        mCloseStyleRes = aTyped.getResourceId(R.styleable.ActionBar_actionbarleftbtnstyle, 0);
        mCloseDrawable = aTyped.getDrawable(R.styleable.ActionBar_homebutton);
        if (mCloseDrawable == null) {
            mCloseDrawable = getResources().getDrawable(R.drawable.ic_ab_back);
        }

        mContentHeight = a.getLayoutDimension(
                R.styleable.ActionMode_height, 0);

        mSplitBackground = a.getDrawable(
                R.styleable.ActionMode_backgroundSplit);

        mContextSplitHeight = context.getResources().getDimensionPixelSize(R.dimen.tws_actionbar_split_height);
        a.recycle();
        //tws-start add for ripple::2014-12-21
        if (android.os.Build.VERSION.SDK_INT > 17) {
        	setClipChildren(false);
        	setClipToPadding(true);
        }
        //tws-end add for ripple::2014-12-21
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

    // tws-start add actionbar0.2 feature::2014-09-28
    public ToggleButton getMultiChoiceView() {
        initTitle();
        
        if (mMulti == null) {
            mMulti = new ToggleButton(mContext);
            mMulti.setId(R.id.actionbar_multichoice);
            mMulti.setTextAppearance(mContext, mMultiStyleRes);
            // tws-start add for ripple::2014-12-21
            boolean bRipple = ThemeUtils.isShowRipple(mContext);
            if (bRipple) {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		mMulti.setBackground(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            	else {
            		mMulti.setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            } else {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		mMulti.setBackground(null);
            	}
            	else {
            		mMulti.setBackgroundDrawable(null);
            	}
            }
            // tws-end add for ripple::2014-12-21
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    (int) mContext.getResources().getDimension(R.dimen.actionbar_btn_height));
            params.gravity = Gravity.CENTER;
            mMulti.setLayoutParams(params);
            mMulti.setEllipsize(TruncateAt.END);
            mMulti.setGravity(Gravity.CENTER);
            mMulti.setFocusable(false);
            mMulti.setSingleLine(true);
            mMulti.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0,
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0);
//          mMulti.setMinWidth((int)mContext.getResources().getDimension(R.dimen.actionbar_btn_width));
            addView(mMulti);
        } else if (mMulti.getParent() == null) {
        	addView(mMulti);
        }
        
        if (mMulti != null && mMulti.getVisibility() != VISIBLE) {
        	mMulti.setVisibility(VISIBLE);
        }
        return mMulti;
    }

    public View getCloseView() {
        initTitle();
        if (mClose != null) {
            mClose.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mClose.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0,
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0);

            if (mActionModeBackOnClickListener != null) {
                mClose.setOnClickListener(mActionModeBackOnClickListener);
            } else {
                mClose.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mActionMode.finish();
                    }
                });
            }
        }
    	return mClose;
    }
    
    public EditText getEditView() {
    	initEdit();
    	if (mTitleLayout != null) {
    		mTitleLayout.setVisibility(GONE);
    	}
    	if (mEdit != null && mEdit.getVisibility() != VISIBLE) {
    		mEdit.setVisibility(VISIBLE);
    	}
    	
    	return mEdit;
    }
	
	public TextView getTitleView() {
    	return mTitleView;
    }
    
    public TextView getSubtitleView() {
    	initSubTitle();
    	return mSubtitleView;
    }
    
    public boolean startMiniMode() {
    	return false;
    }
    
    public boolean exitMiniMode() {
    	return false;
    }
    // tws-end add actionbar0.2 feature::2014-09-28
    
    @Override
    public void setSplitActionBar(boolean split) {
        if (mSplitActionBar != split) {
            if (mActionMenuPresenter != null) {
                // Mode is already active; move everything over and adjust the menu itself.
                final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT);
                if (!split) {
                    mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
                    mMenuView.setBackgroundDrawable(null);
                    final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                    if (oldParent != null) oldParent.removeView(mMenuView);
                    addView(mMenuView, layoutParams);
                } else {
                    // Allow full screen width in split mode.
                    mActionMenuPresenter.setWidthLimit(
                            getContext().getResources().getDisplayMetrics().widthPixels, true);
                    // No limit to the item count; use whatever will fit.
                    mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
                    // Span the whole width
                    layoutParams.width = LayoutParams.MATCH_PARENT;
//                    layoutParams.height = mContentHeight;
                    layoutParams.height = mContextSplitHeight;
                    mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
                    mMenuView.setBackgroundDrawable(null);
                    final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                    if (oldParent != null) oldParent.removeView(mMenuView);
                    mSplitView.addView(mMenuView, layoutParams);
                }
            }
            super.setSplitActionBar(split);
        }
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
    }

    public void setCustomView(View view) {
        if (mCustomView != null) {
            removeView(mCustomView);
        }
        mCustomView = view;
        if (mTitleLayout != null) {
            removeView(mTitleLayout);
            mTitleLayout = null;
        }
        if (view != null) {
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence title) {
    	mTitle = title;
        initTitle();
        firstMeasureTitle = true;
        requestLayout();
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        initTitle();
        initSubTitle();
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    private void initTitle() {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
			/*tws-start::modified com.internal to tws 20121011*/
			TypedValue res = new TypedValue();
    		mContext.getTheme().resolveAttribute(R.attr.twsActionModMenuFontColor, res, true);
			inflater.inflate(R.layout.action_bar_title_item, this);
           	mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
           	mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
            if (mTitleStyleRes != 0) {
                mTitleView.setTextAppearance(mContext, mTitleStyleRes);
            }
        }

        mTitleView.setText(mTitle);

        final boolean hasTitle = !TextUtils.isEmpty(mTitle);
        
        mTitleLayout.setVisibility(hasTitle/* || hasSubtitle*/ ? VISIBLE : GONE);
        if (mTitleLayout.getParent() == null) {
        	// tws-start add actionbar0.2 feature::2014-09-28
        	final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
        	// tws-end add actionbar0.2 feature::2014-09-28
        	mTitleLayout.setLayoutParams(layoutParams);
            addView(mTitleLayout);
        }
    }
    
    private void initSubTitle() {
    	if (mSubtitleView == null && mTitleLayout != null) {
    		mSubtitleView = new TextView(mContext);
    		mSubtitleView.setId(R.id.action_bar_subtitle);
    		mSubtitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    		mSubtitleView.setEllipsize(TruncateAt.END);
    		mSubtitleView.setSingleLine(true);
    		mSubtitleView.setGravity(Gravity.CENTER);
    		mSubtitleView.setVisibility(View.GONE);
    		mTitleLayout.addView(mSubtitleView);
    	}
    		
		if (mSubtitleStyleRes != 0) {
            mSubtitleView.setTextAppearance(mContext, mSubtitleStyleRes);
        }
		
		mSubtitleView.setText(mSubtitle);
		
		final boolean hasSubtitle = !TextUtils.isEmpty(mSubtitle);
        mSubtitleView.setVisibility(hasSubtitle ? VISIBLE : GONE);
    }

    /*tws-start::add::geofffeng::20120224*/
    public void initForMode(final ActionMode mode)
    {
    	initForMode(mode,ActionBar.DISPLAY_DEFAULT_IMAGE);
    }
    public void initForMode(final ActionMode mode,int imageType) {
	    TypedValue res = new TypedValue();
    	mContext.getTheme().resolveAttribute(R.attr.twsActionModMenuFontColor, res, true);

        if (mClose == null) {
            /*tws-start::modified com.internal to tws 20121011*/
//          mClose = (ViewGroup)inflater.inflate(R.layout.action_mode_close_item, this, false);
            mClose = new Button(mContext);
            mClose.setId(R.id.action_mode_close_button);
            mClose.setTextAppearance(mContext, mCloseStyleRes);
            mClose.setCompoundDrawablesWithIntrinsicBounds(mCloseDrawable, null, null, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    (int) mContext.getResources().getDimension(R.dimen.actionbar_btn_height));
            params.gravity = Gravity.CENTER;
            mClose.setLayoutParams(params);
            mClose.setEllipsize(TruncateAt.END);
            mClose.setGravity(Gravity.CENTER);
            mClose.setFocusable(false);
            mClose.setSingleLine(true);
            mClose.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding_backbtn), 0,
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding_backbtn), 0);
            boolean bRipple = ThemeUtils.isShowRipple(mContext);
            if (bRipple) {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		mClose.setBackground(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            	else {
            		mClose.setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            } else {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		mClose.setBackground(null);
            	}
            	else {
            		mClose.setBackgroundDrawable(null);
            	}
            }
            addView(mClose);
        } else if (mClose.getParent() == null) {
            addView(mClose);
        }
        // tws-start ActionMode BackBtn OnClickListener::2014-8-7
        if (mActionModeBackOnClickListener != null) {
            mClose.setOnClickListener(mActionModeBackOnClickListener);
        } else {
            mClose.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mode.finish();
                }
            });
        }
        //tws-end ActionMode BackBtn OnClickListener::2014-8-7

        final MenuBuilder menu = (MenuBuilder) mode.getMenu();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
        /*tws-start::add::geofffeng::20120605 set menucolor to white*/
    	//Log.d("actionbarContextView", "initForMode 2="+res.resourceId);
	    if(res.resourceId>0) {
	    	mActionMenuPresenter = new ActionMenuPresenter(mContext,0);//tws actionmod
    	} else {
            mActionMenuPresenter = new ActionMenuPresenter(mContext);//other actionmod
    	}
        mActionMenuPresenter.setReserveOverflow(true);

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        if (!mSplitActionBar && imageType != 0) {
            menu.addMenuPresenter(mActionMenuPresenter);
            mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            mMenuView.setBackgroundDrawable(null);
            addView(mMenuView, layoutParams);
        } else {
            // Allow full screen width in split mode.
            mActionMenuPresenter.setWidthLimit(
                    getContext().getResources().getDisplayMetrics().widthPixels, true);
            // No limit to the item count; use whatever will fit.
            mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            // Span the whole width
            layoutParams.width = LayoutParams.MATCH_PARENT;
//            layoutParams.height = mContentHeight;
            layoutParams.height = mContextSplitHeight;
            menu.addMenuPresenter(mActionMenuPresenter);
            mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            mMenuView.setBackgroundDrawable(null);
            mSplitView.addView(mMenuView, layoutParams);
        }

        mAnimateInOnLayout = true;
       //tws-start alanhuang 20130509
        mActionMode = mode;
		//tws-end alanhuang 20130509
    }
    /*tws-end::add::geofffeng::20120224*/

    public void closeMode() {
        if (mAnimationMode == ANIMATE_OUT) {
            // Called again during close; just finish what we were doing.
            return;
        }
        if (mClose == null) {
            killMode();
            return;
        }

        finishAnimation();
	  /*tws-start::delete::geofffeng::20121103*/
        //mAnimationMode = ANIMATE_OUT;
        //mCurrentAnimation = makeOutAnimation();
        //mCurrentAnimation.start();
        mAnimationMode = ANIMATE_OUT;
        mCurrentAnimation = makeSplitMenuOutAnimation();
        if (mCurrentAnimation != null) {
            mCurrentAnimation.start();
        }
	 /*tws-end::delete::geofffeng::20121103*/
    }

    private void finishAnimation() {
        final Animator a = mCurrentAnimation;
        if (a != null) {
            mCurrentAnimation = null;
            a.end();
        }
    }

    public void killMode() {
        finishAnimation();
        removeAllViews();
        if (mSplitView != null) {
            mSplitView.removeView(mMenuView);
        }
        mCustomView = null;
        mMenuView = null;
        mAnimateInOnLayout = false;
    }

    @Override
    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom views if they don't supply layout params. Everything else
        // added to an ActionBarContextView should have them already.
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //tws-start modify for ripple::2014-12-23
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                getResources().getDimensionPixelSize(R.dimen.tws_action_bar_shadow_height));
        //tws-end modify for ripple::2014-12-23
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"match_parent\" (or fill_parent)");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }
        
        final int contentWidth = MeasureSpec.getSize(widthMeasureSpec);

        int maxHeight = mContentHeight > 0 ?
                mContentHeight : MeasureSpec.getSize(heightMeasureSpec);

        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        
        if (mClose != null) {
//        	if (mCloseButton != null && mCloseButton.getVisibility() == GONE) {
//        		mClose.removeView(mCloseButton);
//        	}
            availableWidth = measureChildView(mClose, availableWidth, childSpecHeight, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            availableWidth -= lp.leftMargin + lp.rightMargin;
        }
        
        // tws-start add actionbar0.2 feature::2014-09-28
        if (mMulti != null) {
        	mMulti.measure(MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED), 
        			MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
        }
        
        if (mEdit != null) {
        	int closeWidth = mClose != null ? mClose.getMeasuredWidth() : 0;
        	int multiWidth = mMulti != null && !mMulti.getText().equals("") ? mMulti.getMeasuredWidth() : 0;
        	mEdit.setMaxWidth((contentWidth / 2 - Math.max(closeWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding))) * 2);
        	mEdit.measure(MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT,
        			MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(
        					LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED));
        }
        // tws-end add actionbar0.2 feature::2014-09-28

        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth, childSpecHeight, 0);
        }

        // tws-start add actionbar0.2 feature::2014-09-28
        if (mTitleLayout != null && mCustomView == null) {    
//            availableWidth = measureChildView(mTitleLayout, availableWidth, childSpecHeight, 0);
        	int closeWidth = mClose != null ? mClose.getMeasuredWidth() : 0;
        	int multiWidth = mMulti != null && !mMulti.getText().equals("") ? mMulti.getMeasuredWidth() : 0;
    		if (firstMeasureTitle) {
    			if (mTitleView != null)
    				mTitleView.requestLayout();
    			if (mSubtitleView != null)
    				mSubtitleView.requestLayout();
    			mTitleLayout.measure(
    					MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED), 
    					MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
    			firstMeasureTitle = false;
    		}
    		int tagWidth = mTitleLayout.getMeasuredWidth();
    		if (tagWidth > (contentWidth / 2 - Math.max(closeWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding))) * 2) {
    			tagWidth = (contentWidth / 2 - Math.max(closeWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding))) * 2;
    		}
    		mTitleLayout.measure(
    				MeasureSpec.makeMeasureSpec(tagWidth, MeasureSpec.EXACTLY), 
    				MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
        }

        if (mCustomView != null) {
            ViewGroup.LayoutParams lp = mCustomView.getLayoutParams();
            final int customWidthMode = lp.width != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customWidth = lp.width >= 0 ?
                    Math.min(lp.width, availableWidth) : availableWidth;
            final int customHeightMode = lp.height != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customHeight = lp.height >= 0 ?
                    Math.min(lp.height, height) : height;
            mCustomView.measure(MeasureSpec.makeMeasureSpec(customWidth, customWidthMode),
                    MeasureSpec.makeMeasureSpec(customHeight, customHeightMode));
        }
        // tws-end add actionbar0.2 feature::2014-09-28

        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getChildAt(i);
                int paddedViewHeight = v.getMeasuredHeight() + verticalPadding;
                if (paddedViewHeight > measuredHeight) {
                    measuredHeight = paddedViewHeight;
                }
            }
            setMeasuredDimension(contentWidth, measuredHeight);
        } else {
            setMeasuredDimension(contentWidth, maxHeight);
        }
    }

    private Animator makeInAnimation() {
	/*tws-start::delete::geofffeng::20121106*/
        /*mClose.setTranslationX(-mClose.getWidth() -
                ((MarginLayoutParams) mClose.getLayoutParams()).leftMargin);
        ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(mClose, "translationX", 0);
        buttonAnimator.setDuration(200);
        buttonAnimator.addListener(this);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        AnimatorSet.Builder b = set.play(buttonAnimator);

        if (mMenuView != null) {
            final int count = mMenuView.getChildCount();
            if (count > 0) {
                for (int i = count - 1, j = 0; i >= 0; i--, j++) {
                    View child = mMenuView.getChildAt(i);
                    child.setScaleY(0);
                    ObjectAnimator a = ObjectAnimator.ofFloat(child, "scaleY", 0, 1);
                    a.setDuration(300);
                    b.with(a);
                }
            }
        }*/
	 /*tws-end::delete::geofffeng::20121106*/
        return null;
    }

    private Animator makeOutAnimation() {
	/*tws-start::delete::geofffeng::20121106*/
        /*ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(mClose, "translationX",
                -mClose.getWidth() - ((MarginLayoutParams) mClose.getLayoutParams()).leftMargin);
        buttonAnimator.setDuration(200);
        buttonAnimator.addListener(this);
        buttonAnimator.setInterpolator(new DecelerateInterpolator());

        AnimatorSet set = new AnimatorSet();
        AnimatorSet.Builder b = set.play(buttonAnimator);
        if (mMenuView != null) {
            final int count = mMenuView.getChildCount();
            if (count > 0) {
                for (int i = 0; i < 0; i++) {
                    View child = mMenuView.getChildAt(i);
                    child.setScaleY(0);
                    ObjectAnimator a = ObjectAnimator.ofFloat(child, "scaleY", 0);
                    a.setDuration(300);
                    b.with(a);
                }
            }
        }*/
	/*tws-end::delete::geofffeng::20121106*/
        return null;
    }

    private Animator makeSplitMenuInAnimation() {
        ObjectAnimator animator = null;
        if (mMenuView != null) {
            animator = ObjectAnimator.ofFloat(mMenuView, "translationY", mContextSplitHeight,
                    0);
            animator.setDuration(200);
            animator.addListener(this);
        }
        return animator;
    }

    private Animator makeSplitMenuOutAnimation() {
        ObjectAnimator animator = null;
        if (mMenuView != null) {
            animator = ObjectAnimator.ofFloat(mMenuView, "translationY", 0,
                    mContextSplitHeight);
            animator.setDuration(200);
            animator.addListener(this);
        }
        return animator;
    }

    public void playSplitMenuInAnimation() {
        if (mAnimateInOnLayout) {
            mAnimationMode = ANIMATE_IN;
            mCurrentAnimation = makeSplitMenuInAnimation();
            if (mCurrentAnimation != null) {
                mCurrentAnimation.start();
            }
            mAnimateInOnLayout = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();
        
        /*if (mClose != null && mClose.getVisibility() != GONE) {
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            x += lp.leftMargin;
            x += positionChild(mClose, x, y, contentHeight);
            x += lp.rightMargin;

            if (mAnimateInOnLayout) {
                mAnimationMode = ANIMATE_IN;
                mCurrentAnimation = makeInAnimation();
                mCurrentAnimation.start();
                mAnimateInOnLayout = false;
            }
        }*/
        if (mClose != null && mClose.getVisibility() != GONE) {
//            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
//            x += lp.leftMargin;
//            // tws-start add actionbar0.2 feature::2014-09-28
//            x += positionChild(mClose, x, y, contentHeight);
//            // tws-end add actionbar0.2 feature::2014-09-28
//            x += lp.rightMargin;
            positionChild(mClose, getPaddingLeft(), y, contentHeight);
        }

        // tws-start add actionbar0.2 feature::2014-09-28
        if (mTitleLayout != null && mCustomView == null) {
        	positionChild(mTitleLayout, mContext.getResources().getDisplayMetrics().widthPixels / 2 - mTitleLayout.getMeasuredWidth() / 2, y, contentHeight);
        }
        
//        if (mMulti != null && mMulti.getVisibility() != GONE) {
//        	positionChild(mMulti, mContext.getResources().getDisplayMetrics().widthPixels 
//        							- mMulti.getMeasuredWidth()
//        								- (int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding), y, contentHeight);
//        }
        if (mMulti != null && mMulti.getVisibility() != GONE) {
            positionChild(mMulti,
                    mContext.getResources().getDisplayMetrics().widthPixels
                            - mMulti.getMeasuredWidth(), y, contentHeight);
        }
        
        if (mEdit != null && mEdit.getVisibility() != GONE) {
        	positionChild(mEdit, mContext.getResources().getDisplayMetrics().widthPixels / 2 - mEdit.getMeasuredWidth() / 2, y, contentHeight);
        }
        // tws-end add actionbar0.2 feature::2014-09-28
        
        if (mCustomView != null) {
            x += positionChild(mCustomView, x, y, contentHeight);
        }
        
        x = r - l - getPaddingRight();

        if (mMenuView != null) {
            //x -= positionChildInverse(mMenuView, x, y, contentHeight);
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mAnimationMode == ANIMATE_OUT) {
            killMode();
        }
        mAnimationMode = ANIMATE_IDLE;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Action mode started
            event.setSource(this);
            event.setClassName(getClass().getName());
            event.setPackageName(getContext().getPackageName());
            event.setContentDescription(mTitle);
        } else {
            super.onInitializeAccessibilityEvent(event);
        }
    }
    //tws-start ActionMode BackBtn OnClickListener::2014-8-7
    public void twsSetActionModeBackOnClickListener(OnClickListener clickListener){
        mActionModeBackOnClickListener = clickListener;
    }
    //tws-end ActionMode BackBtn OnClickListener::2014-8-7
    
    public void setOverflowButtonState(boolean enable) {
    	if (mActionMenuPresenter == null)
            mActionMenuPresenter = new ActionMenuPresenter(mContext);
    	mActionMenuPresenter.setOverflowButtonState(enable);
    }

    // tws-start add overflow click listener::2014-12-18
    public void setOverflowClickListener(ActionBar.OverflowClickListener listener) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setOverflowClickListener(listener);
		}
    }
    // tws-end add overflow click listener::2014-12-18
    
    // tws-start add Overflow interface::2015-2-9
    public View getOverflowButton() {
    	if (mActionMenuPresenter != null) {
    		return mActionMenuPresenter.getOverflowButton();
    	}
    	return null;
    }
    // tws-end add Overflow interface::2015-2-9
    
    // tws-start add transPopup interface::2015-3-10
    public void setIsTransPopup(boolean isTransPopup) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setIsTransPopup(isTransPopup);
    	}
    }
    // tws-end add transPopup interface::2015-3-10
    
    // tws-start add PopupMenuRedPoint interface::2015-3-12
    public void setPopupMenuMarks(boolean[] isMarks) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setPopupMenuMarks(isMarks);
    	}
    }
    // tws-end add PopupMenuRedPoint interface::2015-3-12
    
    public void setPopupTextColors(int[] textColors) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setPopupTextColors(textColors);
    	}
    }
    
    // tws-start add Overflow clickDelay interface::2015-3-19
    public void setOverflowDelay(boolean isDelay) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setOverflowDelay(isDelay);
    	}
    }
    // tws-end add Overflow clickDelay interface::2015-3-19
}
