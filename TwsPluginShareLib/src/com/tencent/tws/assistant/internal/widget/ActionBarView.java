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

import android.app.Activity;
import android.app.TwsActivity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuItem;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.ActionMenuView;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuItemImpl;
import com.tencent.tws.assistant.internal.view.menu.MenuPresenter;
import com.tencent.tws.assistant.internal.view.menu.MenuView;
import com.tencent.tws.assistant.internal.view.menu.SubMenuBuilder;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.ToggleButton;
import com.tencent.tws.sharelib.R;

/**
 * @hide
 */
public class ActionBarView extends AbsActionBarView {
    private static final String TAG = "ActionBarView";

    /**
     * Display options applied by default
     * yongchen modify for plugin theme
     */
    public static final int DISPLAY_DEFAULT = 0xb;

    /**
     * Display options that require re-layout as opposed to a simple invalidate
     */
    private static final int DISPLAY_RELAYOUT_MASK =
            ActionBar.DISPLAY_SHOW_HOME |
            ActionBar.DISPLAY_USE_LOGO |
            ActionBar.DISPLAY_HOME_AS_UP |
            ActionBar.DISPLAY_SHOW_CUSTOM |
            ActionBar.DISPLAY_SHOW_TITLE;

    private static final int DEFAULT_CUSTOM_GRAVITY = Gravity.LEFT | Gravity.CENTER_VERTICAL;
    
    private int mNavigationMode;
    private int mDisplayOptions = -1;
    private CharSequence mTitle;
    private CharSequence mSubtitle;

    private ImageButton mHomeLayout;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;

    private ScrollingTabContainerView mTabScrollView;
    private View mCustomNavView;

    private int mItemPadding;
    
    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private int mMultiStyleRes;
    private int mCloseStyleRes;
    
    private int mHomeBGStyleRes;
    private int mHomeSrcStyleRes;

    private boolean mUserTitle;
    private boolean mIncludeTabs;
    private boolean mIsCollapsable;
    private boolean mIsCollapsed;

    private MenuBuilder mOptionsMenu;
    
    private ActionBarContextView mContextView;

    private Runnable mTabSelector;

    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;

    Window.Callback mWindowCallback;
    
    // tws-start add actionbar0.2 feature::2014-09-28
    private boolean firstMeasureTitle = true;
    
    private Button mClose;
    private ToggleButton mMulti;
    private ImageButton mRightButton;  //rigth button
    
    private ActionMenuItem mLogoNavItem;
    // tws-end add actionbar0.2 feature::2014-09-28
    
    //tws-start ActionBar Back Button::2014-7-30
    private boolean mIsBackClick = true;
    //tws-end ActionBar Back Button::2014-7-30
    /*tws-start::add::geofffeng::20120219*/
    private Activity mActivity=null;
	private TwsDialog mTwsDialog = null;
    private boolean homeSendMessage=false;
	
	//tws-start ActionBar mini mode::2014-10-14
	private boolean isMiniMode;
	//tws-end ActionBar mini mode::2014-10-14
	
	public boolean mIsMarksPointFlag;
	private boolean mIsMenuConfigFlag;
	private boolean mIsRunInPlugins;
	
    public void setActionbarViewActivity(Activity fatherActivity,boolean sendMessage) {
    	mActivity=fatherActivity;
    	homeSendMessage=sendMessage;
    	/*tws-start::modified home Background 20121114*/
    	if(mActivity != null){
    		TypedValue out = new TypedValue();
    	    mContext.getTheme().resolveAttribute(R.attr.twsHomeAsUp, out, true);
    		if(out.resourceId>0) {
	            //mHomeLayout.setBackgroundDrawable(mContext.getResources().getDrawable(out.resourceId));
                // tws-start add for ripple::2014-12-21
                boolean bRipple = ThemeUtils.isShowRipple(mContext);
                if (bRipple) {
                	if (android.os.Build.VERSION.SDK_INT > 15) {
                		mHomeLayout.setBackground(TwsRippleUtils.getHasContentDrawable(getContext(),
                				mHomeBGStyleRes));
                	}
                	else {
                		mHomeLayout.setBackgroundDrawable(TwsRippleUtils.getHasContentDrawable(getContext(),
                				mHomeBGStyleRes));
                	}
                } else {
                    mHomeLayout.setBackgroundResource(mHomeBGStyleRes);
                }
                // tws-end add for ripple::2014-12-21
    		}		
    	}
    	/*tws-end::modified home Background 20121114*/
    }
    /*tws-end::add::geofffeng::20120219*/

	
	public void setActionbarViewDialog(TwsDialog dialog) {
		mTwsDialog = dialog;
	}
    private final OnClickListener mUpClickListener = new OnClickListener() {
        public void onClick(View v) {
            //tws-start ActionBar Back Button::2014-7-30
            if (mIsBackClick) {
				if(mActivity != null){
                	mActivity.onBackPressed();
				}
				if(mTwsDialog != null){
					mTwsDialog.dismiss();
				}
            } else {
                mWindowCallback.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, mLogoNavItem);
            }
            //tws-end ActionBar Back Button::2014-7-30
        }
    };

    public ActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);

        mNavigationMode = a.getInt(R.styleable.ActionBar_navigationMode,
                ActionBar.NAVIGATION_MODE_STANDARD);
        
		// yongchen modify for plugin theme
		mIsRunInPlugins = false;
		mTitleStyleRes = a.getResourceId(R.styleable.ActionBar_titleTextStyle, 0);
		mIsRunInPlugins = 0 == mTitleStyleRes ? true : false;

		if (mIsRunInPlugins) {
			mTitleStyleRes = R.style.TextAppearance_tws_Second_twsTextLargerLightTitle_ActionBarTitle;
			mSubtitleStyleRes = R.style.TextAppearance_tws_Second_twsTextSmallLightTitle;
			mItemPadding = getResources().getDimensionPixelSize(R.dimen.actionbar_itemPadding);
			mMultiStyleRes = R.style.TextAppearance_tws_Second_twsTextLargerLightTitleRightButton;
			mCloseStyleRes = R.style.TextAppearance_tws_Second_twsTextLargerLightTitleLeftButton;
			mHomeBGStyleRes = R.color.transparent;
			mHomeSrcStyleRes = R.drawable.ic_ab_back;
		} else {
			mSubtitleStyleRes = a.getResourceId(R.styleable.ActionBar_subtitleTextStyle, 0);
			mItemPadding = a.getDimensionPixelOffset(R.styleable.ActionBar_itemPadding, 0);
			mMultiStyleRes = a.getResourceId(R.styleable.ActionBar_actionbarrightbtnstyle, 0);
			mCloseStyleRes = a.getResourceId(R.styleable.ActionBar_actionbarleftbtnstyle, 0);
			mHomeBGStyleRes = a.getResourceId(R.styleable.ActionBar_homebackground, 0);
			mHomeSrcStyleRes = a.getResourceId(R.styleable.ActionBar_homebutton, 0);
		}
        
        initHome(context);

        setDisplayOptions(a.getInt(R.styleable.ActionBar_displayOptions, DISPLAY_DEFAULT));

        final int customNavId = a.getResourceId(R.styleable.ActionBar_customNavigationLayout, 0);
        if (customNavId != 0) {
        	final LayoutInflater inflater = LayoutInflater.from(context);
            mCustomNavView = (View) inflater.inflate(customNavId, this, false);
            mNavigationMode = ActionBar.NAVIGATION_MODE_STANDARD;
            setDisplayOptions(mDisplayOptions | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        a.recycle();
        
        mLogoNavItem = new ActionMenuItem(context, 0, android.R.id.home, 0, 0, mTitle);
        //tws-start add for ripple::2014-12-21
        if (android.os.Build.VERSION.SDK_INT > 17) {
        	setClipChildren(false);
        	setClipToPadding(true);
        }
        //tws-end add for ripple::2014-12-21
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mTitleView = null;
        mSubtitleView = null;
        firstMeasureTitle = true;
        
        if (mTitleLayout != null && mTitleLayout.getParent() == this) {
            removeView(mTitleLayout);
        }
        
        mTitleLayout = null;
        
        if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
            initTitle();
            initSubTitle();
        }

        if (mTabScrollView != null && mIncludeTabs) {
            ViewGroup.LayoutParams lp = mTabScrollView.getLayoutParams();
            if (lp != null) {
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = LayoutParams.MATCH_PARENT;
            }
            mTabScrollView.setAllowCollapse(true);
        }
    }

    /**
     * Set the window callback used to invoke menu items; used for dispatching home button presses.
     * @param cb Window callback to dispatch to
     */
    public void setWindowCallback(Window.Callback cb) {
        mWindowCallback = cb;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mTabSelector);
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void setSplitActionBar(boolean splitActionBar) {
        if (mSplitActionBar != splitActionBar) {
            if (mMenuView != null) {
                final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
                if (oldParent != null) {
                    oldParent.removeView(mMenuView);
                }
                if (splitActionBar) {
                    if (mSplitView != null) {
                        mSplitView.addView(mMenuView);
                    }
                } else {
                    addView(mMenuView);
                }
            }
            if (mSplitView != null) {
                mSplitView.setVisibility(splitActionBar ? VISIBLE : GONE);
            }
            super.setSplitActionBar(splitActionBar);
        }
    }

    public boolean isSplitActionBar() {
        return mSplitActionBar;
    }

    public boolean hasEmbeddedTabs() {
        return mIncludeTabs;
    }

    public void setEmbeddedTabView(ScrollingTabContainerView tabs) {
        if (mTabScrollView != null) {
            removeView(mTabScrollView);
        }
        mTabScrollView = tabs;
        mIncludeTabs = tabs != null;
        if (mIncludeTabs && mNavigationMode == ActionBar.NAVIGATION_MODE_TABS) {
            addView(mTabScrollView);
            ViewGroup.LayoutParams lp = mTabScrollView.getLayoutParams();
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.height = LayoutParams.MATCH_PARENT;
            tabs.setAllowCollapse(true);
        }
    }

    public void setMenu(Menu menu, MenuPresenter.Callback cb) {
		//mSplitActionBar = true;
        if (menu == mOptionsMenu) return;

        if (mOptionsMenu != null) {
            mOptionsMenu.removeMenuPresenter(mActionMenuPresenter);
            mOptionsMenu.removeMenuPresenter(mExpandedMenuPresenter);
        }

        MenuBuilder builder = (MenuBuilder) menu;
        mOptionsMenu = builder;
        if (mOptionsMenu.size() == 0 && !mIsMenuConfigFlag)
        	return;
        if (mMenuView != null) {
            final ViewGroup oldParent = (ViewGroup) mMenuView.getParent();
            if (oldParent != null) {
                oldParent.removeView(mMenuView);
            }
        }
        if (mActionMenuPresenter == null) {
            mActionMenuPresenter = new ActionMenuPresenter(mContext);//geofffeng
            mActionMenuPresenter.setCallback(cb);
			/*tws-start::modified com.internal to tws 20121011*/
			//mActionMenuPresenter.setId(android.R.id.action_menu_presenter);
            mActionMenuPresenter.setId(R.id.action_menu_presenter);
			/*tws-end::modified com.internal to tws 20121011*/
            mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
        }

        ActionMenuView menuView;
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        if (!mSplitActionBar) {
			/*tws-start::modified com.internal to tws 20121011*/
            mActionMenuPresenter.setExpandedActionViewsExclusive(
                    getResources().getBoolean(
                    R.bool.action_bar_expanded_action_views_exclusive));
			/*tws-end::modified com.internal to tws 20121011*/
            configPresenters(builder);
            menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            final ViewGroup oldParent = (ViewGroup) menuView.getParent();
            if (oldParent != null && oldParent != this) {
                oldParent.removeView(menuView);
            }
            //mSplitView.addView(menuView, layoutParams);
			//mSplitView.setVisibility(VISIBLE);
            addView(menuView, layoutParams);
        } else {
            mActionMenuPresenter.setExpandedActionViewsExclusive(false);
            // Allow full screen width in split mode.
            mActionMenuPresenter.setWidthLimit(
                    getContext().getResources().getDisplayMetrics().widthPixels, true);
            // No limit to the item count; use whatever will fit.
            mActionMenuPresenter.setItemLimit(Integer.MAX_VALUE);
            // Span the whole width
            layoutParams.width = LayoutParams.MATCH_PARENT;
            configPresenters(builder);
            menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
            if (mSplitView != null) {
                final ViewGroup oldParent = (ViewGroup) menuView.getParent();
                if (oldParent != null && oldParent != mSplitView) {
                    oldParent.removeView(menuView);
                }
                menuView.setVisibility(getAnimatedVisibility());
            	layoutParams.height = mContext.getResources().getDimensionPixelSize(R.dimen.tws_actionbar_split_height);
            	if (menuView != null && menuView.getChildCount() != 0) {
            		mSplitView.addView(menuView, layoutParams);
            		mSplitView.setVisibility(VISIBLE);
            	}
            	else {
            		mSplitView.setVisibility(GONE);
            	}
            } else {
                // We'll add this later if we missed it this time.
                menuView.setLayoutParams(layoutParams);
            }
        }
        mMenuView = menuView;
    }

    private void configPresenters(MenuBuilder builder) {
		
        if (builder != null) {
            builder.addMenuPresenter(mActionMenuPresenter);
            builder.addMenuPresenter(mExpandedMenuPresenter);
        } else {
            mActionMenuPresenter.initForMenu(mContext, null);
            mExpandedMenuPresenter.initForMenu(mContext, null);
            mActionMenuPresenter.updateMenuView(true);
            mExpandedMenuPresenter.updateMenuView(true);
        }
    }

    public boolean hasExpandedActionView() {
        return mExpandedMenuPresenter != null &&
                mExpandedMenuPresenter.mCurrentExpandedItem != null;
    }

    public void collapseActionView() {
        final MenuItemImpl item = mExpandedMenuPresenter == null ? null :
                mExpandedMenuPresenter.mCurrentExpandedItem;
        if (item != null) {
            item.collapseActionView();
        }
    }

    public void setCustomNavigationView(View view) {
        final boolean showCustom = (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0;
        //tws-start geofffeng 20130327 
		if(showCustom) {
			mUserTitle = true;
		}
		//tws-end geofffeng 20130327 

		if (mCustomNavView != null && showCustom) {
            removeView(mCustomNavView);
        }
        mCustomNavView = view;
        if (mCustomNavView != null && showCustom) {
            addView(mCustomNavView);
        }
        
        //tws-start alanhuang 20130401
        if(mCustomNavView != null){
	        final ViewGroup.LayoutParams lp = mCustomNavView.getLayoutParams();
	        if(lp != null){
	        	int height = lp.height;
	        	if(height > 0){
	        		mContentHeight = height;
	        	}
	        }
        }
		//tws-end alanhuang 20130401
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Set the action bar title. This will always replace or override window titles.
     * @param title Title to set
     *
     * @see #setWindowTitle(CharSequence)
     */
    public void setTitle(CharSequence title) {
        mUserTitle = true;
        setTitleImpl(title);
        firstMeasureTitle = true;
        requestLayout();
    }
    
    /**
     * Set the window title. A window title will always be replaced or overridden by a user title.
     * @param title Title to set
     *
     * @see #setTitle(CharSequence)
     */
    public void setWindowTitle(CharSequence title) {
        if (!mUserTitle) {
            setTitleImpl(title);
        }
    }

    private void setTitleImpl(CharSequence title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setText(title);
            final boolean visible = (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0 &&
                    (!TextUtils.isEmpty(mTitle) || !TextUtils.isEmpty(mSubtitle));
            mTitleLayout.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        initSubTitle();
        if (mSubtitleView != null) {
            mSubtitleView.setText(subtitle);
            mSubtitleView.setVisibility(subtitle != null ? VISIBLE : GONE);
            final boolean visible = (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0 &&
                    (!TextUtils.isEmpty(mTitle) || !TextUtils.isEmpty(mSubtitle));
            mTitleLayout.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public void setHomeButtonEnabled(boolean enable) {
        mHomeLayout.setFocusable(enable);
        // Make sure the home button has an accurate content description for accessibility.
        /*tws-start::modified com.internal to tws 20121011*/
        if (!enable) {
            mHomeLayout.setContentDescription(null);
        } else if ((mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.action_bar_up_description));
        } else {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.action_bar_home_description));
        }
		/*tws-end::modified com.internal to tws 20121011*/
    }

    public void setDisplayOptions(int options) {
        final int flagsChanged = mDisplayOptions == -1 ? -1 : options ^ mDisplayOptions;
        mDisplayOptions = options;

        if ((flagsChanged & DISPLAY_RELAYOUT_MASK) != 0) {
            final boolean showHome = (options & ActionBar.DISPLAY_SHOW_HOME) != 0;
            if(showHome){
            	mHomeLayout.setVisibility(VISIBLE);
            	if(mClose != null){
            		mClose.setVisibility(GONE);
            	}
            }
            else{
            	mHomeLayout.setVisibility(GONE);
            }

            if ((flagsChanged & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
                final boolean setUp = (options & ActionBar.DISPLAY_HOME_AS_UP) != 0;

                // Showing home as up implicitly enables interaction with it.
                // In honeycomb it was always enabled, so make this transition
                // a bit easier for developers in the common case.
                // (It would be silly to show it as up without responding to it.)
                if (setUp) {
                    setHomeButtonEnabled(true);
                }
            }

            if ((flagsChanged & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                if ((options & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                    initTitle();
                } else {
                    removeView(mTitleLayout);
                }
            }

            if (mTitleLayout != null && (flagsChanged &
                    (ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME)) != 0) {
                final boolean homeAsUp = (mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;
                mTitleLayout.setEnabled(!showHome && homeAsUp);
            }

            if ((flagsChanged & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 && mCustomNavView != null) {
                if ((options & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
                    addView(mCustomNavView);
                } else {
                    removeView(mCustomNavView);
                }
            }
            requestLayout();
        } else {
            invalidate();
        }

        // Make sure the home button has an accurate content description for accessibility.
        /*tws-start::modified com.internal to tws 20121011*/
        if (!mHomeLayout.isEnabled()) {
            mHomeLayout.setContentDescription(null);
        } else if ((options & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.action_bar_up_description));
        } else {
            mHomeLayout.setContentDescription(mContext.getResources().getText(
                    R.string.action_bar_home_description));
        }
		/*tws-end::modified com.internal to tws 20121011*/
    }

    public void setNavigationMode(int mode) {
        final int oldMode = mNavigationMode;
        if (mode != oldMode) {
            switch (oldMode) {
            case ActionBar.NAVIGATION_MODE_TABS:
                if (mTabScrollView != null && mIncludeTabs) {
                    removeView(mTabScrollView);
                }
            }
            
            switch (mode) {
            case ActionBar.NAVIGATION_MODE_TABS:
                if (mTabScrollView != null && mIncludeTabs) {
                    addView(mTabScrollView);
                }
                break;
            }
            mNavigationMode = mode;
            requestLayout();
        }
    }

    public View getCustomNavigationView() {
        return mCustomNavView;
    }
    
    public int getNavigationMode() {
        return mNavigationMode;
    }
    
    public int getDisplayOptions() {
        return mDisplayOptions;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom nav views if they don't supply layout params. Everything else
        // added to an ActionBarView should have them already.
        return new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT,DEFAULT_CUSTOM_GRAVITY);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        addView(mHomeLayout);

        if (mCustomNavView != null && (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
            final ViewParent parent = mCustomNavView.getParent();
            if (parent != this) {
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mCustomNavView);
                }
                addView(mCustomNavView);
            }
        }
    }

    private void initTitle() {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
			/*tws-start::modified com.internal to tws 20121011*/
            mTitleLayout = (LinearLayout) inflater.inflate(R.layout.action_bar_title_item,
                    this, false);
            mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
			/*tws-end::modified com.internal to tws 20121011*/
            mTitleLayout.setOnClickListener(mUpClickListener);
			
            if (mTitleStyleRes != 0) {
                mTitleView.setTextAppearance(mContext, mTitleStyleRes);
				// yongchen add for plugin theme
				if (mIsRunInPlugins) {
					mTitleView.setTextColor(getResources().getColor(R.color.tws_light_title_actionBar));
					// <dimen name="tws_Large_TextSize_Title">18sp</dimen>
					mTitleView.setTextSize(18.0f);
				}
            }
            if (mTitle != null) {
                mTitleView.setText(mTitle);
            }

            final boolean homeAsUp = (mDisplayOptions & ActionBar.DISPLAY_HOME_AS_UP) != 0;
            final boolean showHome = (mDisplayOptions & ActionBar.DISPLAY_SHOW_HOME) != 0;
            mTitleLayout.setEnabled(homeAsUp && !showHome);
        }

        addView(mTitleLayout);
        if (TextUtils.isEmpty(mTitle) && TextUtils.isEmpty(mSubtitle)) {
            // Don't show while in expanded mode or with empty text
            mTitleLayout.setVisibility(GONE);
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
			// yongchen add for plugin theme
			if (mIsRunInPlugins) {
				mSubtitleView.setTextColor(getResources().getColor(R.color.tws_light_subtitle_actionBar));
				mSubtitleView.setHintTextColor(getResources().getColor(R.color.tws_second_Hint));
				// mSubtitleView.setTextSize(getResources().getDimension(R.dimen.tws_Micro_TextSize));
				// <dimen name="tws_Micro_TextSize">12sp</dimen>
				mSubtitleView.setTextSize(12.0f);
			}
        }
        if (mSubtitle != null) {
            mSubtitleView.setText(mSubtitle);
            mSubtitleView.setVisibility(VISIBLE);
        }
    }
    
    private void initHome(Context context) {
        mHomeLayout = new ImageButton(context);
        mHomeLayout.setId(android.R.id.home);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) mContext.getResources()
                .getDimension(R.dimen.action_bar_back_btn_width), LayoutParams.MATCH_PARENT);//TODO gordonbi
        params.gravity = Gravity.CENTER;
        mHomeLayout.setLayoutParams(params);
        // tws-start add for ripple::2014-12-21

        boolean bRipple = ThemeUtils.isShowRipple(context);
        if (bRipple) {
        	if (android.os.Build.VERSION.SDK_INT > 15) {
        		mHomeLayout.setBackground(TwsRippleUtils.getDefaultDarkDrawable(mContext));
        	}
        	else {
        		mHomeLayout.setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(mContext));
        	}
        } else {
            mHomeLayout.setBackgroundResource(mHomeBGStyleRes);
        }
        // tws-end add for ripple::2014-12-21
        mHomeLayout.setFocusable(false);
        mHomeLayout.setImageResource(mHomeSrcStyleRes);
        mHomeLayout.setOnClickListener(mUpClickListener);
        mHomeLayout.setClickable(true);
        mHomeLayout.setEnabled(true);
    }
    
    // tws-start add actionbar0.2 feature::2014-09-28
    public ToggleButton getMultiChoiceView() {
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
//    		mMulti.setMinWidth((int)mContext.getResources().getDimension(R.dimen.actionbar_btn_width));
            addView(mMulti);
        } else if (mMulti.getParent() == null) {
            addView(mMulti);
        }
        mSplitActionBar = true;
        if (mMulti != null && mMulti.getVisibility() != VISIBLE) {
            mMulti.setVisibility(VISIBLE);
        }
        return mMulti;
    }
    
    //get rigth button add by jackymli
    public ImageView getRighButtonView(){
        if (mRightButton == null) {
        	mRightButton = new ImageButton(mContext);
            // tws-start add for ripple::2014-12-21
            boolean bRipple = ThemeUtils.isShowRipple(mContext);
            if (bRipple) {
            	if (android.os.Build.VERSION.SDK_INT > 15) {
            		mRightButton.setBackground(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            	else {
            		mRightButton.setBackgroundDrawable(TwsRippleUtils.getDefaultDarkDrawable(getContext()));
            	}
            } else {
            	mRightButton.setBackgroundResource(mHomeBGStyleRes);
            }
            // tws-end add for ripple::2014-12-21
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) mContext.getResources().getDimension(R.dimen.action_bar_right_btn_width),LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            mRightButton.setLayoutParams(params);
            mRightButton.setFocusable(false);
//            mRightButton.setPadding(
//                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0,
//                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0);
            addView(mRightButton);
        } else if (mRightButton.getParent() == null) {
            addView(mRightButton);
        }
        mSplitActionBar = true;
        if (mRightButton != null && mRightButton.getVisibility() != VISIBLE) {
        	mRightButton.setVisibility(VISIBLE);
        }
        return mRightButton;
    
    }

    public View getCloseView() {
        if (mClose == null) {
            mClose = new Button(mContext);
            mClose.setId(R.id.action_mode_close_button);
            mClose.setTextAppearance(mContext, mCloseStyleRes);
            mClose.setVisibility(GONE);
            // tws-start add for ripple::2014-12-21
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
            // tws-end add for ripple::2014-12-21
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    (int) mContext.getResources().getDimension(R.dimen.actionbar_btn_height));
            params.gravity = Gravity.CENTER;
            mClose.setLayoutParams(params);
            mClose.setEllipsize(TruncateAt.END);
            mClose.setGravity(Gravity.CENTER);
            mClose.setFocusable(false);
            mClose.setSingleLine(true);
            mClose.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0,
                    getResources().getDimensionPixelSize(R.dimen.actionbar_mode_padding), 0);
            addView(mClose);
        } else if (mClose.getParent() == null) {
            addView(mClose);
        }

        if (mHomeLayout != null) {
            mHomeLayout.setVisibility(GONE);
        }
        if (mClose != null && mClose.getVisibility() != VISIBLE) {
            mClose.setVisibility(VISIBLE);
        }
        return mClose;
    }

    public EditText getEditView() {
    	initEdit();
    	if (mTitleLayout != null) {
    		mTitleView.setVisibility(GONE);
    		mTitleLayout.setVisibility(GONE);
    	}
    	if (mEdit != null && mEdit.getVisibility() != VISIBLE) {
    		mEdit.setVisibility(VISIBLE);
    	}
    	return mEdit;
    }
	
	//tws-start ActionBar mini mode::2014-10-14
	public TextView getTitleView() {
    	return mTitleView;
    }
    
    public TextView getSubtitleView() {
    	initSubTitle();
    	return mSubtitleView;
    }
    
	//tws-start ActionBar homeLayout interface::2014-12-26
    public ImageButton getActionBarHome() {
    	return mHomeLayout;
    }
    //tws-end ActionBar homeLayout interface::2014-12-26
    public boolean startMiniMode() {
    	mMenuView.setVisibility(GONE);
    	for (int i = 0; i < mMenuView.getChildCount(); i++) {
    		mMenuView.getChildAt(i).setVisibility(GONE);
    	}
    	if (mSplitActionBar) {
    		if (mSplitView != null) {
                mSplitView.removeView(mMenuView);
            }
    	}
		isMiniMode = true;
    	return true;
    }
    
    public boolean exitMiniMode() {
    	mMenuView.setVisibility(VISIBLE);
    	for (int i = 0; i < mMenuView.getChildCount(); i++) {
    		mMenuView.getChildAt(i).setVisibility(VISIBLE);
    	}
    	if (mSplitActionBar) {
    		if (mSplitView != null) {
    			mSplitView.removeView(mMenuView);
                mSplitView.addView(mMenuView);
            }
    	}
		isMiniMode = false;
    	return false;
    }
	//tws-end ActionBar mini mode::2014-10-14
    // tws-end add actionbar0.2 feature::2014-09-28

    public void setContextView(ActionBarContextView view) {
        mContextView = view;
    }

    public void setCollapsable(boolean collapsable) {
        mIsCollapsable = collapsable;
    }

    public boolean isCollapsed() {
        return mIsCollapsed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //tws-start modify for ripple::2014-12-23
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getResources()
                .getDimensionPixelSize(R.dimen.tws_action_bar_shadow_height));
        //tws-end modify for ripple::2014-12-23
        final int childCount = getChildCount();
        if (mIsCollapsable) {
            int visibleChildren = 0;
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE &&
                        !(child == mMenuView && mMenuView.getChildCount() == 0)) {
                    visibleChildren++;
                }
            }

            if (visibleChildren == 0) {
                setMeasuredDimension(0, 0);
                mIsCollapsed = true;
                return;
            }
        }
        mIsCollapsed = false;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        int contentWidth = MeasureSpec.getSize(widthMeasureSpec);

        int maxHeight = mContentHeight > 0 ?
                mContentHeight : MeasureSpec.getSize(heightMeasureSpec);
        
        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

        int availableWidth = contentWidth - paddingLeft - paddingRight;
        int leftOfCenter = availableWidth / 2;
        int rightOfCenter = leftOfCenter;

        if (mHomeLayout.getVisibility() != GONE) {
            final ViewGroup.LayoutParams lp = mHomeLayout.getLayoutParams();
            int homeWidthSpec;
            if (lp.width < 0) {
                homeWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
            } else {
                homeWidthSpec = MeasureSpec.makeMeasureSpec((int)getResources().getDimension(R.dimen.actionbar_home_width), MeasureSpec.EXACTLY);
            }
            mHomeLayout.measure(homeWidthSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            final int homeWidth = mHomeLayout.getMeasuredWidth();
       		availableWidth = Math.max(0, availableWidth - homeWidth);
       		leftOfCenter = Math.max(0, availableWidth - homeWidth);
        }
        
        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth, childSpecHeight, 0);
            rightOfCenter = Math.max(0, rightOfCenter - mMenuView.getMeasuredWidth());
        }
        
        if (mClose != null && mClose.getVisibility() == VISIBLE) {
    		mClose.measure(
    				MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED),
    				MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
        }
        
        if (mMulti != null && mMulti.getVisibility() == VISIBLE) {
        	mMulti.measure(
        			MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED), 
        			MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
        }
        
        if (mRightButton != null && mRightButton.getVisibility() == VISIBLE) {
        	final ViewGroup.LayoutParams lp = mRightButton.getLayoutParams();
            int RightButtonWidthSpec;
            if (lp.width < 0) {
            	RightButtonWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
            } else {
            	RightButtonWidthSpec = MeasureSpec.makeMeasureSpec((int)getResources().getDimension(R.dimen.action_bar_right_btn_width), MeasureSpec.EXACTLY);
            }
            mRightButton.measure(RightButtonWidthSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
//        	mRightButton.measure(
//        			MeasureSpec.makeMeasureSpec((int)getResources().getDimension(R.dimen.actionbar_home_width), MeasureSpec.EXACTLY), 
//        			MeasureSpec.makeMeasureSpec((int)getResources().getDimension(R.dimen.actionbar_rightbuuton_heigt), MeasureSpec.EXACTLY));
        }
        
        if (mEdit != null && mEdit.getVisibility() == VISIBLE) {
        	if (mHomeLayout != null && mMenuView != null && !mSplitActionBar) {
        		int homeWidth = mHomeLayout.getMeasuredWidth();
        		int menuWidth = mMenuView.getMeasuredWidth();
        		mEdit.setMaxWidth((contentWidth / 2 - Math.max(homeWidth, menuWidth) - getPaddingLeft()) * 2);
        	}
        	else if (mClose != null && mMulti != null) {
        		int closeWidth = mClose != null ? mClose.getMeasuredWidth() : 0;
        		int multiWidth = mMulti != null ? mMulti.getMeasuredWidth() : 0;
        		mEdit.setMaxWidth((contentWidth / 2 - Math.max(closeWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingLeft()) * 2);
        	}
        	else if (mHomeLayout != null && mMenuView != null && mSplitActionBar && mClose == null && mMulti == null) {
        		int homeWidth = mHomeLayout.getMeasuredWidth();
        		mEdit.setMaxWidth((contentWidth / 2 - homeWidth - getPaddingLeft()) * 2);
        	}
    		mEdit.measure(
    				MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED), 
    				MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED));
        }

        final boolean showTitle = mTitleLayout != null && mTitleLayout.getVisibility() != GONE &&
                (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0;

            switch (mNavigationMode) {
            case ActionBar.NAVIGATION_MODE_TABS:
                if (mTabScrollView != null) {
                    final int itemPaddingSize = showTitle ? mItemPadding * 2 : mItemPadding;
                    availableWidth = Math.max(0, availableWidth - itemPaddingSize);
                    leftOfCenter = Math.max(0, leftOfCenter - itemPaddingSize);
                    mTabScrollView.measure(
                            MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    final int tabWidth = mTabScrollView.getMeasuredWidth();
                    availableWidth = Math.max(0, availableWidth - tabWidth);
                    leftOfCenter = Math.max(0, leftOfCenter - tabWidth);
                }
                break;
            }

        View customView = null;
        if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 &&
                mCustomNavView != null) {
            customView = mCustomNavView;
        }

        if (customView != null) {
            final ViewGroup.LayoutParams lp = generateLayoutParams(customView.getLayoutParams());
            final ActionBar.LayoutParams ablp = lp instanceof ActionBar.LayoutParams ?
                    (ActionBar.LayoutParams) lp : null;

            int horizontalMargin = 0;
            int verticalMargin = 0;
            if (ablp != null) {
                horizontalMargin = ablp.leftMargin + ablp.rightMargin;
                verticalMargin = ablp.topMargin + ablp.bottomMargin;
            }

            // If the action bar is wrapping to its content height, don't allow a custom
            // view to MATCH_PARENT.
            int customNavHeightMode;
            if (mContentHeight <= 0) {
                customNavHeightMode = MeasureSpec.AT_MOST;
            } else {
                customNavHeightMode = lp.height != LayoutParams.WRAP_CONTENT ?
                        MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            }
            final int customNavHeight = Math.max(0,
                    (lp.height >= 0 ? Math.min(lp.height, height) : height) - verticalMargin);

            final int customNavWidthMode = lp.width != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            int customNavWidth = Math.max(0,
                    (lp.width >= 0 ? Math.min(lp.width, availableWidth) : availableWidth)
                    - horizontalMargin);
            final int hgrav = (ablp != null ? ablp.gravity : DEFAULT_CUSTOM_GRAVITY) &
                    Gravity.HORIZONTAL_GRAVITY_MASK;

            // Centering a custom view is treated specially; we try to center within the whole
            // action bar rather than in the available space.
            if (hgrav == Gravity.CENTER_HORIZONTAL && lp.width == LayoutParams.MATCH_PARENT) {
                customNavWidth = Math.min(leftOfCenter, rightOfCenter) * 2;
            }

			//tws-start ActionBar mini mode::2014-10-14
            if (!isMiniMode) {
            	customView.measure(
            			MeasureSpec.makeMeasureSpec(customNavWidth, customNavWidthMode),
            			MeasureSpec.makeMeasureSpec(customNavHeight, customNavHeightMode));
            }
            else {
            	customView.measure(
            			MeasureSpec.makeMeasureSpec(contentWidth, customNavWidthMode),
            			MeasureSpec.makeMeasureSpec(customNavHeight, customNavHeightMode));
            }
			//tws-end ActionBar mini mode::2014-10-14
            availableWidth -= horizontalMargin + customView.getMeasuredWidth();
        }

		if (showTitle) {
			// tws-start add actionbar0.2 feature::2014-09-28
			if (firstMeasureTitle) {
				if (mTitleView != null)
					mTitleView.requestLayout();
				if (mSubtitleView != null)
					mSubtitleView.requestLayout();
				mTitleLayout.measure(
						MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.AT_MOST), 
						MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
				firstMeasureTitle = false;
			}
			int tagWidth = mTitleLayout.getMeasuredWidth();
			
			int maxWidth = tagWidth;
			
			if (mMenuView != null) {
				if (!mSplitActionBar) {
					int temp = Math.max(mMenuView.getMeasuredWidth(), mHomeLayout.getMeasuredWidth());
					maxWidth = (contentWidth / 2 - temp - paddingLeft - paddingRight) * 2;
				}
				else {
					maxWidth = (contentWidth / 2 - ((mHomeLayout != null && mHomeLayout.getVisibility() != View.GONE) ? mHomeLayout.getMeasuredWidth() : 0) - paddingLeft - paddingRight) * 2;
				}
				tagWidth = Math.min(tagWidth, maxWidth);
			}
			else if(mHomeLayout != null && mHomeLayout.getVisibility() != View.GONE){
				tagWidth = (contentWidth / 2 - mHomeLayout.getMeasuredWidth() - paddingLeft - paddingRight) * 2;
			}else if(mRightButton != null && mRightButton.getVisibility() != View.GONE){
				tagWidth = (contentWidth / 2 - mRightButton.getMeasuredWidth() - paddingLeft - paddingRight) * 2;
			}else{
				tagWidth = (contentWidth / 2 - 0 - paddingLeft - paddingRight) * 2;
			}
			
			if (mClose != null) {
				int closeWidth = mClose.getVisibility() == VISIBLE ? mClose.getMeasuredWidth() : 0;
				if (tagWidth > (contentWidth / 2 - Math.max(closeWidth, closeWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingLeft()) * 2) {
					tagWidth = (contentWidth / 2 - Math.max(closeWidth, closeWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingLeft()) * 2;
				}
			}
			if (mMulti != null) {
				int multiWidth = mMulti.getVisibility() == VISIBLE ? mMulti.getMeasuredWidth() : 0;
				if (tagWidth > (contentWidth / 2 - Math.max(multiWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingRight()) * 2) {
					tagWidth = (contentWidth / 2 - Math.max(multiWidth, multiWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingRight()) * 2;
				}
			}
			if (mClose != null && mMulti != null) {
				int closeWidth = mClose.getVisibility() == VISIBLE ? mClose.getMeasuredWidth() : 0;
				int multiWidth = mMulti.getVisibility() == VISIBLE ? mMulti.getMeasuredWidth() : 0;
				int buttonWidth = Math.max(closeWidth, multiWidth);
				if (tagWidth > (contentWidth / 2 - Math.max(buttonWidth, buttonWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingRight()) * 2) {
					tagWidth = (contentWidth / 2 - Math.max(buttonWidth, buttonWidth+(int)mContext.getResources().getDimension(R.dimen.actionbar_mode_padding)) - getPaddingRight()) * 2;
				}
			}
			
			
			mTitleLayout.measure(
					MeasureSpec.makeMeasureSpec(tagWidth, MeasureSpec.EXACTLY), 
					MeasureSpec.makeMeasureSpec(mContentHeight, MeasureSpec.EXACTLY));
			// tws-end add actionbar0.2 feature::2014-09-28
		}
        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            for (int i = 0; i < childCount; i++) {
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

        if (mContextView != null) {
            mContextView.setContentHeight(mContentHeight);
            mContextView.setPadding(0, getPaddingTop(), 0, 0);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (contentHeight <= 0) {
            // Nothing to do if we can't see anything.
            return;
        }

        if (mHomeLayout.getVisibility() != GONE) {
            x += positionChild(mHomeLayout, x, y, contentHeight);
            //tws-start ActionBar BackBtn Width::2014-8-20
            int paddingRight = 0;
            x -= paddingRight;
            //tws-end ActionBar BackBtn Width::2014-8-20
        }

            final boolean showTitle = mTitleLayout != null && mTitleLayout.getVisibility() != GONE &&
                    (mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0;
            if (showTitle) {
            	positionChild(mTitleLayout, mContext.getResources().getDisplayMetrics().widthPixels / 2 - mTitleLayout.getMeasuredWidth() / 2, y, contentHeight);
//                x += positionChild(mTitleLayout, x, y, contentHeight);
            }

            switch (mNavigationMode) {
            case ActionBar.NAVIGATION_MODE_TABS:
                if (mTabScrollView != null) {
                    if (showTitle) x += mItemPadding;
                    x += positionChild(mTabScrollView, x, y, contentHeight) + mItemPadding;
                }
                break;
            }

        int menuLeft = r - l - getPaddingRight();
        if (mMenuView != null && mMenuView.getParent() == this) {
            positionChildInverse(mMenuView, menuLeft, y, contentHeight);
            menuLeft -= mMenuView.getMeasuredWidth();
        }

        View customView = null;
        if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0 &&
                mCustomNavView != null) {
            customView = mCustomNavView;
        }
        if (customView != null) {
            ViewGroup.LayoutParams lp = customView.getLayoutParams();
            final ActionBar.LayoutParams ablp = lp instanceof ActionBar.LayoutParams ?
                    (ActionBar.LayoutParams) lp : null;

            final int gravity = ablp != null ? ablp.gravity : DEFAULT_CUSTOM_GRAVITY;
            final int navWidth = customView.getMeasuredWidth();

            int topMargin = 0;
            int bottomMargin = 0;
            if (ablp != null) {
                x += ablp.leftMargin;
                menuLeft -= ablp.rightMargin;
                topMargin = ablp.topMargin;
                bottomMargin = ablp.bottomMargin;
            }

            int hgravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            // See if we actually have room to truly center; if not push against left or right.
            if (hgravity == Gravity.CENTER_HORIZONTAL) {
                final int centeredLeft = ((mRight - mLeft) - navWidth) / 2;
                if (centeredLeft < x) {
                    hgravity = Gravity.LEFT;
                } else if (centeredLeft + navWidth > menuLeft) {
                    hgravity = Gravity.RIGHT;
                }
            } else if (gravity == -1) {
                hgravity = Gravity.LEFT;
            }

            int xpos = 0;
            switch (hgravity) {
                case Gravity.CENTER_HORIZONTAL:
                    xpos = ((mRight - mLeft) - navWidth) / 2;
                    break;
                case Gravity.LEFT:
                    xpos = x;
                    break;
                case Gravity.RIGHT:
                    xpos = menuLeft - navWidth;
                    break;
            }

            int vgravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

            if (gravity == -1) {
                vgravity = Gravity.CENTER_VERTICAL;
            }

            int ypos = 0;
            switch (vgravity) {
                case Gravity.CENTER_VERTICAL:
                    final int paddedTop = getPaddingTop();
                    final int paddedBottom = mBottom - mTop - getPaddingBottom();
                    ypos = ((paddedBottom - paddedTop) - customView.getMeasuredHeight()) / 2;
                    break;
                case Gravity.TOP:
                    ypos = getPaddingTop() + topMargin;
                    break;
                case Gravity.BOTTOM:
                    ypos = getHeight() - getPaddingBottom() - customView.getMeasuredHeight()
                            - bottomMargin;
                    break;
            }
            final int customWidth = customView.getMeasuredWidth();
            if (android.os.Build.VERSION.SDK_INT > 18  && getResources().getBoolean(R.bool.config_statusbar_state)) {
            	customView.layout(xpos, ypos + TwsActivity.getStatusBarHeight(), xpos + customWidth,
            			ypos + TwsActivity.getStatusBarHeight() + customView.getMeasuredHeight());
            }
            else {
            	customView.layout(xpos, ypos, xpos + customWidth, ypos + customView.getMeasuredHeight());
            }
            x += customWidth;
        }

        // tws-start add actionbar0.2 feature::2014-09-28
        if (mClose != null && mClose.getVisibility() != GONE) {
        	positionChild(mClose, getPaddingLeft(), y, contentHeight);
        }

        if (mMulti != null && mMulti.getVisibility() != GONE) {
            positionChild(mMulti,
                    mContext.getResources().getDisplayMetrics().widthPixels - mMulti.getMeasuredWidth()
                            - getPaddingRight(), y, contentHeight);
        }
        
        if (mRightButton != null && mRightButton.getVisibility() != GONE) {
            positionChild(mRightButton,
                    mContext.getResources().getDisplayMetrics().widthPixels - mRightButton.getMeasuredWidth()
                            - mRightButton.getPaddingRight(), y, contentHeight);
        }

        if (mEdit != null && mEdit.getVisibility() != GONE) {
        	positionChild(mEdit, mContext.getResources().getDisplayMetrics().widthPixels / 2 - mEdit.getMeasuredWidth() / 2, y, contentHeight);
        }
        // tws-end add actionbar0.2 feature::2014-09-28
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ActionBar.LayoutParams(getContext(), attrs);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp == null) {
            lp = generateDefaultLayoutParams();
        }
        return lp;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        if (mExpandedMenuPresenter != null && mExpandedMenuPresenter.mCurrentExpandedItem != null) {
            state.expandedMenuItemId = mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }

        state.isOverflowOpen = isOverflowMenuShowing();

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable p) {
        SavedState state = (SavedState) p;

        super.onRestoreInstanceState(state.getSuperState());

        if (state.expandedMenuItemId != 0 &&
                mExpandedMenuPresenter != null && mOptionsMenu != null) {
            final MenuItem item = mOptionsMenu.findItem(state.expandedMenuItemId);
            if (item != null) {
                item.expandActionView();
            }
        }

        if (state.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    static class SavedState extends BaseSavedState {
        int expandedMenuItemId;
        boolean isOverflowOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            expandedMenuItemId = in.readInt();
            isOverflowOpen = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expandedMenuItemId);
            out.writeInt(isOverflowOpen ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuBuilder mMenu;
        MenuItemImpl mCurrentExpandedItem;

        @Override
        public void initForMenu(Context context, MenuBuilder menu) {
            // Clear the expanded action view when menus change.
            if (mMenu != null && mCurrentExpandedItem != null) {
                mMenu.collapseItemActionView(mCurrentExpandedItem);
            }
            mMenu = menu;
        }

        @Override
        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        @Override
        public void updateMenuView(boolean cleared) {
            // Make sure the expanded item we have is still there.
            if (mCurrentExpandedItem != null) {
                boolean found = false;

                if (mMenu != null) {
                    final int count = mMenu.size();
                    for (int i = 0; i < count; i++) {
                        final MenuItem item = mMenu.getItem(i);
                        if (item == mCurrentExpandedItem) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    // The item we had expanded disappeared. Collapse.
                    collapseItemActionView(mMenu, mCurrentExpandedItem);
                }
            }
        }

        @Override
        public void setCallback(Callback cb) {
        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            mCurrentExpandedItem = item;
            mHomeLayout.setVisibility(GONE);
            if (mTitleLayout != null) mTitleLayout.setVisibility(GONE);
            if (mTabScrollView != null) mTabScrollView.setVisibility(GONE);
            if (mCustomNavView != null) mCustomNavView.setVisibility(GONE);
            requestLayout();
            item.setActionViewExpanded(true);

            return true;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            // Do this before detaching the actionview from the hierarchy, in case
            // it needs to dismiss the soft keyboard, etc.
            if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_HOME) != 0) {
                mHomeLayout.setVisibility(VISIBLE);
                if(mClose != null){
                	mClose.setVisibility(GONE);
                }
            }
            if ((mDisplayOptions & ActionBar.DISPLAY_SHOW_TITLE) != 0) {
                if (mTitleLayout == null) {
                    initTitle();
                } else {
                    mTitleLayout.setVisibility(VISIBLE);
                }
            }
            if (mTabScrollView != null && mNavigationMode == ActionBar.NAVIGATION_MODE_TABS) {
                mTabScrollView.setVisibility(VISIBLE);
            }
            if (mCustomNavView != null && (mDisplayOptions & ActionBar.DISPLAY_SHOW_CUSTOM) != 0) {
                mCustomNavView.setVisibility(VISIBLE);
            }
            mCurrentExpandedItem = null;
            requestLayout();
            item.setActionViewExpanded(false);

            return true;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
        }
    }

    //tws-start ActionBar Back Button::2014-7-30
    public void twsSetBackOnclickEnabled(boolean enable){
        mIsBackClick = enable;
    }
    public boolean twsGetBackOnclickEnabled(){
        return mIsBackClick;
    }
    //tws-end ActionBar Back Button::2014-7-30
    
    public void setOverflowButtonState(boolean enable) {
    	if (mActionMenuPresenter == null)
            mActionMenuPresenter = new ActionMenuPresenter(mContext);
        mActionMenuPresenter.setOverflowButtonState(enable);
    }
    
    public void setTopOverflowButtonState(boolean enable) {
    	if (mActionMenuPresenter == null)
            mActionMenuPresenter = new ActionMenuPresenter(mContext);
        mActionMenuPresenter.setTopOverflowButtonState(enable);
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
    		mIsMarksPointFlag = true;
    		mActionMenuPresenter.setPopupMenuMarks(isMarks);
    	}
    }
    // tws-end add PopupMenuRedPoint interface::2015-3-12
    
    // tws-start add PopupMenuText interface::2015-5-18
    public void setPopupTextColors(int[] textColors) {
    	if (mActionMenuPresenter != null) {
    		mIsMarksPointFlag = true;
    		mActionMenuPresenter.setPopupTextColors(textColors);
    	}
    }
    // tws-end add PopupMenuText interface::2015-5-18
    
    // tws-start add Overflow clickDelay interface::2015-3-19
    public void setOverflowDelay(boolean isDelay) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setOverflowDelay(isDelay);
    	}
    }
    
    public void setTopOverflowDelay(boolean isDelay) {
    	if (mActionMenuPresenter != null) {
    		mActionMenuPresenter.setTopOverflowDelay(isDelay);
    	}
    }
    // tws-end add Overflow clickDelay interface::2015-3-19
    
    // tws-start add Menu Config flag::2015-3-31
    public void setMenuConfigFlag(boolean isConfig) {
    	mIsMenuConfigFlag = isConfig;
    }
    // tws-end add Menu Config flag::2015-3-31
}
