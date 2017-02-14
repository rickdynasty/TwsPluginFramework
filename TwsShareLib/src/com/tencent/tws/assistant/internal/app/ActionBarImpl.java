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

package com.tencent.tws.assistant.internal.app;

import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.internal.view.menu.MenuBuilder;
import com.tencent.tws.assistant.internal.view.menu.MenuPopupHelper;
import com.tencent.tws.assistant.internal.view.menu.SubMenuBuilder;
import com.tencent.tws.assistant.internal.widget.ActionBarContainer;
import com.tencent.tws.assistant.internal.widget.ActionBarContextView;
import com.tencent.tws.assistant.internal.widget.ActionBarView;
import com.tencent.tws.assistant.internal.widget.ScrollingTabContainerView;
import com.tencent.tws.assistant.widget.FloatView;
import com.tencent.tws.sharelib.R;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.FragmentTransaction;
import android.app.TwsActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import android.content.res.TypedArray;

/**  
 * ActionBarImpl is the ActionBar implementation used
 * by devices of all screen sizes. If it detects a compatible decor,
 * it will split contextual modes across both the ActionBarView at
 * the top of the screen and a horizontal LinearLayout at the bottom
 * which is normally hidden.
 */
public class ActionBarImpl extends ActionBar {
    private static final String TAG = "ActionBarImpl";

    private Context mContext;
    private Context mThemedContext;
    private TwsActivity mActivity=null;
    private boolean mStatusBarOverlay = false;
    private TwsDialog mDialog;
    
    private FloatView mFloatView;

    private ActionBarContainer mContainerView;
    private ActionBarView mActionView;
    private ActionBarContextView mContextView;
    private ActionBarContainer mSplitView;
    private View mContentView;
    private ScrollingTabContainerView mTabScrollView;

    private ArrayList<TabImpl> mTabs = new ArrayList<TabImpl>();

    private TabImpl mSelectedTab;
    private int mSavedTabPosition = INVALID_POSITION;
    
    ActionModeImpl mActionMode;
    ActionMode mDeferredDestroyActionMode;
    ActionMode.Callback mDeferredModeDestroyCallback;
    
    private boolean mLastMenuVisibility;
    private ArrayList<OnMenuVisibilityListener> mMenuVisibilityListeners =
            new ArrayList<OnMenuVisibilityListener>();

    private static final int CONTEXT_DISPLAY_NORMAL = 0;
    private static final int CONTEXT_DISPLAY_SPLIT = 1;
    
    private static final int INVALID_POSITION = -1;

    private int mContextDisplayMode;
    private boolean mHasEmbeddedTabs;

    final Handler mHandler = new Handler();
    Runnable mTabSelector;

    private Animator mCurrentShowAnim;
    private Animator mCurrentModeAnim;
    private boolean mShowHideAnimationEnabled;
    boolean mWasHiddenBeforeMode;
    
    public boolean mOverflowButtonState;
    
    private ActionBar.SplitBlurListener mSplitBlurListener;
    private ActionBar.ActionBarHideListener mBarHideListener;
    private ActionBar.ActionBarShowListener mBarShowListener;
    private ActionBar.ActionModeFinishListener mModeFinishListener;
    //tws-start add for action tab::2015-1-7
    private boolean mTabHasTitle = false;
    //tws-end add for action tab::2015-1-7
    
    private boolean mTabButtonEnable;
    private boolean mTabWaveEnable;

    private final AnimatorListener mSplitShowAnimListener = new AnimatorListenerAdapter() {
        
        @Override
        public void onAnimationEnd(Animator animation) {
            mSplitView.setVisibility(View.VISIBLE);
            mContextView.setVisibility(View.VISIBLE);
            mActionView.setVisibility(View.GONE);
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            // tws-start modify ActionMode Blur Listener::2014-12-12
            if (mSplitBlurListener != null)
            	mSplitBlurListener.doBlur();
            // tws-end modify ActionMode Blur Listener::2014-12-12
        }
    };
    private final AnimatorListener mSplitHideAnimListener = new AnimatorListenerAdapter() {
        
        @Override
        public void onAnimationEnd(Animator animation) {
            mSplitView.setVisibility(View.GONE);
            mContextView.setVisibility(View.GONE);
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            if(mCallBack!=null)
                mCallBack.onDestroyActionMode();
            if (mModeFinishListener != null)
            	mModeFinishListener.doFinish();
        }
        @Override
        public void onAnimationStart(Animator animation) {
            mActionView.setVisibility(View.VISIBLE);
        }
    };
    private final AnimatorListener mSplitContextHideAnimListener = new AnimatorListenerAdapter() {
        
        @Override
        public void onAnimationEnd(Animator animation) {
            mContextView.setVisibility(View.GONE);
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            if(mCallBack!=null)
                mCallBack.onDestroyActionMode();
        }
        @Override
        public void onAnimationStart(Animator animation) {
            mActionView.setVisibility(View.VISIBLE);
        }
    };
    
    ActionBar.Callback mCallBack=null;
    @Override
    public void setActionModeCallback(ActionBar.Callback callback)
    {
        mCallBack=callback;
    }
    
    final AnimatorListener mHideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mContentView != null) {
                mContentView.setTranslationY(0);
                mContainerView.setTranslationY(0);
            }
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setVisibility(View.GONE);
            }
            mActionView.setVisibility(View.GONE);
            mContainerView.setVisibility(View.GONE);
            mContainerView.setTransitioning(false);
            mCurrentShowAnim = null;
            completeDeferredDestroyActionMode();
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarHideListener != null)
            	mBarHideListener.doHideBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };

    final AnimatorListener mShowListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setVisibility(View.VISIBLE);
            }
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarShowListener != null)
            	mBarShowListener.doShowBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };
    
    final AnimatorListener mHideTopListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mContentView != null) {
                mContentView.setTranslationY(0);
                mContainerView.setTranslationY(0);
            }
            mContainerView.setVisibility(View.GONE);
            mContainerView.setTransitioning(false);
            mCurrentShowAnim = null;
            completeDeferredDestroyActionMode();
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarHideListener != null)
            	mBarHideListener.doHideBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };
    
    final AnimatorListener mShowTopListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarShowListener != null)
            	mBarShowListener.doShowBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };
    
    
    final AnimatorListener mAlphahideTopListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mContainerView.setVisibility(View.GONE);
            mCurrentShowAnim = null;
            completeDeferredDestroyActionMode();
            if (mBarHideListener != null)
            	mBarHideListener.doHideBar();
        }
    };
    
    final AnimatorListener mAlphashowTopListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            if (mBarShowListener != null)
            	mBarShowListener.doShowBar();
        }
    };
    
    final AnimatorListener mAlphahideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setVisibility(View.GONE);
            }
            mContainerView.setVisibility(View.GONE);
            mCurrentShowAnim = null;
            completeDeferredDestroyActionMode();
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarHideListener != null)
            	mBarHideListener.doHideBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };

    final AnimatorListener mAlphashowListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
            mContainerView.requestLayout();
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setVisibility(View.VISIBLE);
            }
            // tws-start add showhide actionbar listener::2015-1-14
            if (mBarShowListener != null)
            	mBarShowListener.doShowBar();
            // tws-end add showhide actionbar listener::2015-1-14
        }
    };

    public ActionBarImpl(TwsActivity activity)
    {
        this(activity, false);
    }
    
    public ActionBarImpl(TwsActivity activity, boolean isStatusBarOverlay) {
    	mStatusBarOverlay = isStatusBarOverlay;
    	mActivity = activity;
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        init(decor);
        if (!mActivity.getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY)) {
            mContentView = decor.findViewById(android.R.id.content);
        }
        mOverflowButtonState = true;
        mTabHasTitle = false;
    }
    
    public ActionBarImpl(TwsDialog dialog) {
        mDialog = dialog;
        init(dialog.getWindow().getDecorView());
        //tws-start modify TwsDialog ActionBar style::2014-02-10
        if (mActionView != null) {
        	mActionView.setPadding(0, (Build.VERSION.SDK_INT > 18 && mContext.getResources().getBoolean(R.bool.config_statusbar_state)) ? TwsActivity.getStatusBarHeight() : 0, 0, 0);
        }
        //tws-end modify TwsDialog ActionBar style::2014-02-10
    }
    
    //tws-start add global float view::2014-09-13
    public ActionBarImpl(FloatView floatView) {
    	mFloatView = floatView;
        init(floatView.getWindow().getDecorView());
    }
    //tws-end add global float view::2014-09-13

    private int init(View decor) {
        mContext = decor.getContext();
        /*NANJISTART::modified com.internal to tws 20121011*/
        mActionView = (ActionBarView) decor.findViewById(R.id.tws_action_bar);
        mContextView = (ActionBarContextView) decor.findViewById(
                R.id.tws_action_context_bar);
        mContainerView = (ActionBarContainer) decor.findViewById(
                R.id.tws_action_bar_container);
        mSplitView = (ActionBarContainer) decor.findViewById(
                R.id.tws_split_action_bar);
        /*NANJISTART::modified com.internal to tws 20121011*/
        if (mActionView == null || mContextView == null || mContainerView == null) {
            //throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
            //        "with a compatible window decor layout");
            return 0;
        }
		mActionView.setActionbarViewDialog(mDialog);
        mActionView.setContextView(mContextView);
        mActionView.setSplitView(mSplitView);
        mContextView.setSplitView(mSplitView);
        mContextDisplayMode = mActionView.isSplitActionBar() ?
                CONTEXT_DISPLAY_SPLIT : CONTEXT_DISPLAY_NORMAL;

        // Older apps get the home button interaction enabled by default.
        // Newer apps need to enable it explicitly.
        setHomeButtonEnabled(mContext.getApplicationInfo().targetSdkVersion <
                Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        setHasEmbeddedTabs(mContext.getResources().getBoolean(R.bool.action_bar_embed_tabs));
        return 1;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        /*NANJISTART::modified com.internal to tws 20121011*/
        setHasEmbeddedTabs(mContext.getResources().getBoolean(R.bool.action_bar_embed_tabs));
        /*NANJIEND::modified com.internal to tws 20121011*/
    }

    private void setHasEmbeddedTabs(boolean hasEmbeddedTabs) {
        mHasEmbeddedTabs = hasEmbeddedTabs;
        // Switch tab layout configuration if needed
        if (!mHasEmbeddedTabs) {
            mActionView.setEmbeddedTabView(null);
            mContainerView.setTabContainer(mTabScrollView);
        } else {
            mContainerView.setTabContainer(null);
            mActionView.setEmbeddedTabView(mTabScrollView);
        }
        final boolean isInTabMode = getNavigationMode() == NAVIGATION_MODE_TABS;
        if (mTabScrollView != null) {
            mTabScrollView.setVisibility(isInTabMode ? View.VISIBLE : View.GONE);
        }
        mActionView.setCollapsable(!mHasEmbeddedTabs && isInTabMode);
    }

    private void ensureTabsExist() {
        if (mTabScrollView != null) {
            return;
        }
        //tws-start modify for actionbar tab::2015-1-7
        int tabMode = ACTIONBAR_TAB_STANDARD;
        if (mStatusBarOverlay) {
            if (mTabHasTitle) {
                tabMode = ACTIONBAR_TAB_OVERLAY_SECOND;
            } else {
                tabMode = ACTIONBAR_TAB_OVERLAY;
            }
        } else {
            if (mTabHasTitle) {
                tabMode = ACTIONBAR_TAB_STANDARD_SECOND;
            } else {
                tabMode = ACTIONBAR_TAB_STANDARD;
            }
        }

        ScrollingTabContainerView tabScroller = new ScrollingTabContainerView(mContext, tabMode, mTabButtonEnable, mTabWaveEnable);
        //tws-end modify for actionbar tab::2015-1-7

        if (mHasEmbeddedTabs) {
            tabScroller.setVisibility(View.VISIBLE);
            mActionView.setEmbeddedTabView(tabScroller);
        } else {
            tabScroller.setVisibility(getNavigationMode() == NAVIGATION_MODE_TABS ?
                    View.VISIBLE : View.GONE);
            mContainerView.setTabContainer(tabScroller);
        }
        tabScroller.setContextView(mContextView);
        mTabScrollView = tabScroller;
    }

    void completeDeferredDestroyActionMode() {
        if (mDeferredModeDestroyCallback != null) {
            mDeferredModeDestroyCallback.onDestroyActionMode(mDeferredDestroyActionMode);
            mDeferredDestroyActionMode = null;
            mDeferredModeDestroyCallback = null;
        }
    }

    /**
     * Enables or disables animation between show/hide states.
     * If animation is disabled using this method, animations in progress
     * will be finished.
     *
     * @param enabled true to animate, false to not animate.
     */
    public void setShowHideAnimationEnabled(boolean enabled) {
        mShowHideAnimationEnabled = enabled;
        if (!enabled && mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
    }

    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.add(listener);
    }

    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.remove(listener);
    }

    public void dispatchMenuVisibilityChanged(boolean isVisible) {
        if (isVisible == mLastMenuVisibility) {
            return;
        }
        mLastMenuVisibility = isVisible;

        final int count = mMenuVisibilityListeners.size();
        for (int i = 0; i < count; i++) {
            mMenuVisibilityListeners.get(i).onMenuVisibilityChanged(isVisible);
        }
    }

    @Override
    public void setCustomView(int resId) {
        setCustomView(LayoutInflater.from(getThemedContext()).inflate(resId, mActionView, false));
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo) {
        setDisplayOptions(useLogo ? DISPLAY_USE_LOGO : 0, DISPLAY_USE_LOGO);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        setDisplayOptions(showHome ? DISPLAY_SHOW_HOME : 0, DISPLAY_SHOW_HOME);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        setDisplayOptions(showHomeAsUp ? DISPLAY_HOME_AS_UP : 0, DISPLAY_HOME_AS_UP);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        setDisplayOptions(showTitle ? DISPLAY_SHOW_TITLE : 0, DISPLAY_SHOW_TITLE);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        setDisplayOptions(showCustom ? DISPLAY_SHOW_CUSTOM : 0, DISPLAY_SHOW_CUSTOM);
    }

    @Override
    public void setHomeButtonEnabled(boolean enable) {
    /*NANJISTART::add::geofffeng::20120229*/
    mActionView.setActionbarViewActivity(mActivity,false);
		mActionView.setActionbarViewDialog(mDialog);
    /*NANJIEND::add::geofffeng::20120229*/
        mActionView.setHomeButtonEnabled(enable);
    }


	// tws-start add smooth scroll feature to tab::2014-11-18
    public void twsSetPageScroll(int position, float positionOffset)
	{
    	if (mTabScrollView != null) {
    		mTabScrollView.twsSetPageScroll(position, positionOffset);
    	}
	}

	public void twsSetScrollEnd()
	{
		if (mTabScrollView != null) {
    		mTabScrollView.twsSetScrollEnd();
    	}
	};
    // tws-end add smooth scroll feature to tab ::2014-11-18

    // tws-start add actionbar0.2 feature::2014-09-28
	
	@Override
	public void setSplitBlurListener(ActionBar.SplitBlurListener listener) {
		mSplitBlurListener = listener;
	}
	
	// tws-start add overflow click listener::2014-12-18
	@Override
	public void setOverflowClickListener(ActionBar.OverflowClickListener listener, boolean isActionMode) {
		if (isActionMode) {
			mContextView.setOverflowClickListener(listener);
		} else {
			mActionView.setOverflowClickListener(listener);
		}
	}
	// tws-end add overflow click listener::2014-12-18
	
	// tws-start add Overflow interface::2015-2-9
	@Override
	public View getOverflowButton(boolean isActionMode) {
		if (isActionMode) {
			return mContextView.getOverflowButton();
		}
		else {
			return mActionView.getOverflowButton();
		}
	}
	
	// tws-end add Overflow interface::2015-2-9
	
	// tws-start add transPopup interface::2015-3-10
	@Override
	public void setIsTransPopup(boolean isActionMode, boolean isTransPopup) {
		if (isActionMode) {
			mContextView.setIsTransPopup(isTransPopup);
		}
		else {
			mActionView.setIsTransPopup(isTransPopup);
		}
	}
	// tws-end add transPopup interface::2015-3-10
	
	// tws-start add PopupMenuRedPoint interface::2015-3-12
	@Override
	public void setPopupMenuMarks(boolean isActionMode, boolean[] isMarks) {
		if (isActionMode) {
			mContextView.setPopupMenuMarks(isMarks);
		}
		else {
			mActionView.setPopupMenuMarks(isMarks);
		}
	}
	// tws-end add PopupMenuRedPoint interface::2015-3-12
	
	// tws-start add PopupMenuText interface::2015-5-18
	@Override
	public void setPopupTextColors(boolean isActionMode, int[] textColors) {
		if (isActionMode) {
			mContextView.setPopupTextColors(textColors);
		}
		else {
			mActionView.setPopupTextColors(textColors);
		}
	}
	// tws-end add PopupMenuText interface::2015-5-18
	
	// tws-start add Overflow clickDelay interface::2015-3-19
	@Override
	public void setOverflowDelay(boolean isActionMode, boolean isDelay) {
		if (isActionMode) {
			mContextView.setOverflowDelay(isDelay);
		}
		else {
			mActionView.setOverflowDelay(isDelay);
		}
	}
	
	@Override
	public void setTopOverflowDelay(boolean isDelay) {
		mActionView.setTopOverflowDelay(isDelay);
	}
	// tws-end add Overflow clickDelay interface::2015-3-19

	// tws-start add Menu Config flag::2015-3-31
	@Override
	public void setMenuConfigFlag(boolean isConfig) {
		mActionView.setMenuConfigFlag(isConfig);
	}
	// tws-end add Menu Config flag::2015-3-31
	
	@Override
	public void enableTabClick(boolean isEnable) {
		mTabScrollView.enableTabClick(isEnable);
	}
	
	// tws-start add showhide actionbar listener::2015-1-14
	@Override
	public void setActionBarShowListener(ActionBarShowListener listener) {
		mBarShowListener = listener;
	}
	
	@Override
	public void setActionBarHideListener(ActionBarHideListener listener) {
		mBarHideListener = listener;
	}
	
	@Override
	public void setActionModeFinishListener(ActionModeFinishListener listener) {
		mModeFinishListener = listener;
	}
	
	@Override
	public void setActionBarAnimTrans(boolean animTrans) {
		mActionBarAnimTrans = animTrans;
	}

	@Override
	public void setActionBarHideNoAnim(boolean flag) {
		mActionBarHideNoAnim = flag;
	}
	// tws-end add showhide actionbar listener::2015-1-14
	
    @Override
    public View getMultiChoiceView() {
        return getMultiChoiceView(true);
    }
    
    @Override
	public ImageView getRightButtonView() {
        
		return mActionView.getRighButtonView();
	}

	@Override
    public View getMultiChoiceView(boolean isActionMode) {
        if (isActionMode) {
            return mContextView.getMultiChoiceView();
        } else {
            return mActionView.getMultiChoiceView();
        }
    }

    @Override
    public View getCloseView() {
        return getCloseView(true);
    }

    @Override
    public View getCloseView(boolean isActionMode) {
        if (isActionMode) {
            return mContextView.getCloseView();
        } else {
            return mActionView.getCloseView();
        }
    }
    
    @Override
    public EditText getEditView() {
        return getEditView(true);
    }
    
    @Override
    public EditText getEditView(boolean isActionMode) {
    	if (isActionMode)
    		return mContextView.getEditView();
    	else
    		return mActionView.getEditView();
    }
    
    @Override
    public void setActionBarBGColor(int resId) {
    	mActionView.setBackgroundResource(resId);
    }
    
    @Override
    public void setActionBarBGColor2(int color){
    	mActionView.setBackgroundColor(color);
    }
    
    @Override
    public void setActionModeBGColor(int resId) {
    	mContextView.setBackgroundResource(resId);
    }
	
	@Override
    public TextView getTitleView(boolean isActionMode) {
    	if (isActionMode)
    		return mContextView.getTitleView();
    	else
    		return mActionView.getTitleView();
    }
    
    @Override
    public TextView getSubtitleView(boolean isActionMode) {
    	if (isActionMode)
    		return mContextView.getSubtitleView();
    	else
    		return mActionView.getSubtitleView();
    }
    
	//tws-start ActionBar homeLayout interface::2014-12-26
    @Override
    public ImageButton getActionBarHome() {
    	return mActionView.getActionBarHome();
    }
    
    @Override
    public ActionBarView getActionBarView() {
    	return mActionView;
    }
	//tws-end ActionBar homeLayout interface::2014-12-26
    
    @Override
    public boolean startMiniMode() {
    	return mActionView.startMiniMode();
    }
    
    @Override
    public boolean exitMiniMode() {
		return mActionView.exitMiniMode();
    }
    
    @Override
    public void setOverflowButtonState(boolean isActionMode, boolean enable) {
    	if (isActionMode)
    		mContextView.setOverflowButtonState(enable);
    	else
    		mActionView.setOverflowButtonState(enable);
    	if (enable)
    		mOverflowButtonState = true;
    	else
    		mOverflowButtonState = false;
    }
    
    @Override
    public void setTopOverflowButtonState(boolean enable) {
    	mActionView.setTopOverflowButtonState(enable);
    	if (enable)
    		mOverflowButtonState = true;
    	else
    		mOverflowButtonState = false;
    }
    
    // tws-end add actionbar0.2 feature::2014-09-28
    
    // tws-start add actionbar tab wave interface::2015-9-28
    @Override
    public void twsSetPageSelected(int position) {
    	if (mTabScrollView != null) {
    		mTabScrollView.twsSetPageSelected(position);
    	}
    }
    
    
    @Override
    public void twsSetTabTextSelectChange(boolean change) {
    	if (mContainerView != null) {
    		mContainerView.twsSetTabTextSelectChange(change);
    	}
    }
    
    @Override
    public void twsSetTabWaveEnable(boolean enable) {
    	mTabWaveEnable = enable;
    }
    
    @Override
    public void twsSetTabCustomEnable(boolean enable) {
    	mTabButtonEnable = enable;
    }
    
    @Override
    public void twsSetTabLeftView(View view) {
    	if (mContainerView != null) {
    		mContainerView.twsSetTabLeftView(view);
    	}
    }
    
    @Override
    public void twsSetTabRightView(View view) {
    	if (mContainerView != null) {
    		mContainerView.twsSetTabRightView(view);
    	}
    }
    
    @Override
    public void twsSetTabCustomEnd() {
    	if (mContainerView != null) {
    		mContainerView.twsSetTabCustomEnd();
    	}
    }
    // tws-end add actionbar tab wave interface::2015-9-28
        
    @Override
    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    @Override
    public void setSubtitle(int resId) {
        setSubtitle(mContext.getString(resId));
    }

    public void setSelectedNavigationItem(int position) {
        switch (mActionView.getNavigationMode()) {
        case NAVIGATION_MODE_TABS:
            selectTab(mTabs.get(position));
            break;
        case NAVIGATION_MODE_STANDARD:
            Log.d(TAG,"the current mode is NAVIGATION_MODE_STANDARD");
            break;
        default:
            throw new IllegalStateException(
                    "setSelectedNavigationIndex not valid for current navigation mode");
        }
    }

    public void removeAllTabs() {
        cleanupTabs();
    }

    private void cleanupTabs() {
        if (mSelectedTab != null) {
            selectTab(null);
        }
        mTabs.clear();
        if (mTabScrollView != null) {
            mTabScrollView.removeAllTabs();
        }
        mSavedTabPosition = INVALID_POSITION;
    }

    public void setTitle(CharSequence title) {
        mActionView.setTitle(title);
    }

    public void setSubtitle(CharSequence subtitle) {
        mActionView.setSubtitle(subtitle);
    }

    public void setDisplayOptions(int options) {
        mActionView.setDisplayOptions(options);
    }

    public void setDisplayOptions(int options, int mask) {
        final int current = mActionView.getDisplayOptions(); 
        mActionView.setDisplayOptions((options & mask) | (current & ~mask));
    }

    public void setBackgroundDrawable(Drawable d) {
        mContainerView.setPrimaryBackground(d);
    }
	
	public int getBackgroundResId() {
        return mContainerView.getPrimaryBackgroundResId();
    }
    public void setStackedBackgroundDrawable(Drawable d) {
        mContainerView.setStackedBackground(d);
    }

    public void setSplitBackgroundDrawable(Drawable d) {
        if (mSplitView != null) {
            mSplitView.setSplitBackground(d);
        }
    }

    public View getCustomView() {
        return mActionView.getCustomNavigationView();
    }

    public CharSequence getTitle() {
        return mActionView.getTitle();
    }

    public CharSequence getSubtitle() {
        return mActionView.getSubtitle();
    }

    public int getNavigationMode() {
        return mActionView.getNavigationMode();
    }

    public int getDisplayOptions() {
        return mActionView.getDisplayOptions();
    }

    /*NANJISTART::add::geofffeng::20120223*/
    public ActionMode startActionMode(ActionMode.Callback callback) {
        boolean wasHidden = false;
        if (mActionMode != null) {
            wasHidden = mWasHiddenBeforeMode;
            mActionMode.finish();
        }

        //Log.d("actionbarImpl", "startActionMode ="+mActionModeSimple);
        //Log.d("actionbarImpl", "tws startActionMode 0="+mActionModeSimple);
        if(mActionModeSimple!=ACTIONMODE_DEFAULT)
        {
            ActionModeImpl mode = new ActionModeImpl(callback);
            if (mode.dispatchOnCreate()) {
                mWasHiddenBeforeMode=false;
                mode.invalidate();
                mActionMode = mode;
                return mode;
            }
        }
        else
        {
            mContextView.killMode();
            ActionModeImpl mode = new ActionModeImpl(callback);
            if (mode.dispatchOnCreate()) {
                mWasHiddenBeforeMode = !isShowing() || wasHidden;
                mode.invalidate();
                mContextView.initForMode(mode,mImageType);
                animateToMode(true);
                if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                    // TODO animate this
                    mSplitView.setVisibility(View.VISIBLE);
                }
                mContextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                mActionMode = mode;
                mIsInActionMode = true;
                return mode;
            }
        }
        return null;
    }
    private int mImageType=DISPLAY_DEFAULT_IMAGE;
    private int mActionModeSimple=ACTIONMODE_DEFAULT;
    @Override
    public void setUserCloseImageType(int imageType)
    {
        mImageType=imageType;
    }
    @Override
    public void setActionModeSimple(int modeType)
    {
        mActionModeSimple=modeType;
    }
    @Override
    public void actionModeManualFinish()
    {
        if(mActionMode != null)
            mActionMode.finish();
    }
    /*NANJIEND::add::geofffeng::20120223*/

    private void configureTab(Tab tab, int position) {
        final TabImpl tabi = (TabImpl) tab;
        final ActionBar.TabListener callback = tabi.getCallback();

        if (callback == null) {
            throw new IllegalStateException("Action Bar Tab must have a Callback");
        }

        tabi.setPosition(position);
        mTabs.add(position, tabi);

        final int count = mTabs.size();
        for (int i = position + 1; i < count; i++) {
            mTabs.get(i).setPosition(i);
        }
    }

    @Override
    public void addTab(Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, boolean setSelected) {
        ensureTabsExist();
        if (mTabButtonEnable) {
        	mTabScrollView.addTabInternal(tab, setSelected);
        }
        else {
        	mTabScrollView.addTab(tab, setSelected);
        }
        configureTab(tab, mTabs.size());
        if (setSelected) {
            selectTab(tab);
        }
    }

    @Override
    public void addTab(Tab tab, int position, boolean setSelected) {
        ensureTabsExist();
        mTabScrollView.addTab(tab, position, setSelected);
        configureTab(tab, position);
        if (setSelected) {
            selectTab(tab);
        }
    }

    @Override
    public Tab newTab() {
        return new TabImpl();
    }

    @Override
    public void removeTab(Tab tab) {
        removeTabAt(tab.getPosition());
    }

    @Override
    public void removeTabAt(int position) {
        if (mTabScrollView == null) {
            // No tabs around to remove
            return;
        }

        int selectedTabPosition = mSelectedTab != null
                ? mSelectedTab.getPosition() : mSavedTabPosition;
        mTabScrollView.removeTabAt(position);
        TabImpl removedTab = mTabs.remove(position);
        if (removedTab != null) {
            removedTab.setPosition(-1);
        }

        final int newTabCount = mTabs.size();
        for (int i = position; i < newTabCount; i++) {
            mTabs.get(i).setPosition(i);
        }

        if (selectedTabPosition == position) {
            selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
        }
    }

    @Override
    public void selectTab(Tab tab) {
        if (getNavigationMode() != NAVIGATION_MODE_TABS) {
            mSavedTabPosition = tab != null ? tab.getPosition() : INVALID_POSITION;
            return;
        }

        final FragmentTransaction trans = mActivity.getFragmentManager().beginTransaction()
                .disallowAddToBackStack();

        if (mSelectedTab == tab) {
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabReselected(mSelectedTab, trans);
                mTabScrollView.animateToTab(tab.getPosition());
            }
        } else {
            mTabScrollView.setTabSelected(tab != null ? tab.getPosition() : Tab.INVALID_POSITION);
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabUnselected(mSelectedTab, trans);
            }
            mSelectedTab = (TabImpl) tab;
            if (mSelectedTab != null) {
                mSelectedTab.getCallback().onTabSelected(mSelectedTab, trans);
            }
        }

        if (!trans.isEmpty()) {
            trans.commit();
        }
    }

    @Override
    public Tab getSelectedTab() {
        return mSelectedTab;
    }

    @Override
    public int getHeight() {
        return mContainerView.getHeight();
    }

    @Override
    public void show() {
        show(true);
    }

    void show(boolean markHiddenBeforeMode) {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mContainerView.getVisibility() == View.VISIBLE) {
            if (markHiddenBeforeMode) mWasHiddenBeforeMode = false;
            return;
        }
        mActionView.setVisibility(View.VISIBLE);
        mContainerView.setVisibility(View.VISIBLE);

        if (mShowHideAnimationEnabled) {
            mContainerView.setAlpha(0);
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 1));
            //tws-start ActionBar Overlay Animation::2014-8-18
            if (mContentView != null) {
                b.with(ObjectAnimator.ofFloat(mContentView, "translationY",
                        -mContainerView.getHeight(), 0));
            }
            // tws-start add showhide actionbar listener::2015-1-14
            if (mActionBarAnimTrans) {
            	mContentView = mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
				b.with(ObjectAnimator.ofFloat(mContentView, "translationY", 0, mContainerView.getHeight() - TwsActivity.getStatusBarHeight()));
            }
            // tws-end add showhide actionbar listener::2015-1-14
            mContainerView.setTranslationY(-mContainerView.getHeight());
            b.with(ObjectAnimator.ofFloat(mContainerView, "translationY", 0));
            //tws-end ActionBar Overlay Animation::2014-8-18
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setAlpha(0);
                mSplitView.setVisibility(View.VISIBLE);
                mSplitView.setTranslationY(mSplitView.getHeight());
                b.with(ObjectAnimator.ofFloat(mSplitView, "alpha", 1));
                b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", 0));
            }
            anim.addListener(mShowListener);
            mCurrentShowAnim = anim;
            anim.start();
        } else {
            mContainerView.setAlpha(1);
            mContainerView.setTranslationY(0);
            mShowListener.onAnimationEnd(null);
        }
    }

    @Override
    public void hide() {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mContainerView.getVisibility() == View.GONE) {
            return;
        }
        if (mContextView.getVisibility() == View.VISIBLE) {
            return;
        }

        if (mShowHideAnimationEnabled) {
            mContainerView.setAlpha(1);
            if (mActionBarHideNoAnim) {
            	mContainerView.setAlpha(0);
            }
            mContainerView.setTransitioning(true);
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 0));
            //tws-start ActionBar Overlay Animation::2014-8-18
            if (mContentView != null) {
                b.with(ObjectAnimator.ofFloat(mContentView, "translationY",
                        0, -mContainerView.getHeight()));
            }
            // tws-start add showhide actionbar listener::2015-1-14
            if (mActionBarAnimTrans) {
				mContentView = mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
				b.with(ObjectAnimator.ofFloat(mContentView, "translationY", 0, -(mContainerView.getHeight() - TwsActivity.getStatusBarHeight())));
            }
            // tws-end add showhide actionbar listener::2015-1-14
            b.with(ObjectAnimator.ofFloat(mContainerView, "translationY",
                    -mContainerView.getHeight()));
            //tws-end ActionBar Overlay Animation::2014-8-18
            if (mSplitView != null && mSplitView.getVisibility() == View.VISIBLE) {
                mSplitView.setAlpha(1);
                if (mActionBarHideNoAnim) {
                	mSplitView.setAlpha(0);
                	mActionBarHideNoAnim = false;
                }
                b.with(ObjectAnimator.ofFloat(mSplitView, "alpha", 0));
                mSplitView.setTranslationY(0);
                b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", mSplitView.getHeight()));
            }
            anim.addListener(mHideListener);
            mCurrentShowAnim = anim;
            anim.start();
        } else {
            mHideListener.onAnimationEnd(null);
        }
    }
    
    
    @Override
    public void topActionbar_show() {
    	if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        mContainerView.setVisibility(View.VISIBLE);

        if (mShowHideAnimationEnabled) {
        	mContainerView.setAlpha(0);
        	mContainerView.setTransitioning(true);
        	AnimatorSet anim = new AnimatorSet();
        	AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 1));
        	mContainerView.setTranslationY(-mContainerView.getHeight());
        	b.with(ObjectAnimator.ofFloat(mContainerView, "translationY", 0));
        	anim.addListener(mShowTopListener);
        	mCurrentShowAnim = anim;
        	anim.start();
        } else {
            mContainerView.setAlpha(1);
            mContainerView.setTranslationY(0);
            mShowTopListener.onAnimationEnd(null);
        }
    }
    
    @Override
    public void topActionbar_hide() {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }

        if (mShowHideAnimationEnabled) {
        	mContainerView.setAlpha(1);
        	mContainerView.setTransitioning(true);
        	AnimatorSet anim = new AnimatorSet();
        	AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 0));
        	b.with(ObjectAnimator.ofFloat(mContainerView, "translationY", -mContainerView.getHeight()));
        	anim.addListener(mHideTopListener);
        	mCurrentShowAnim = anim;
        	anim.start();
        } else {
        	mHideTopListener.onAnimationEnd(null);
        }
    }
    
    @Override
    public void topActionbar_alphashow() {
    	if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        mContainerView.setVisibility(View.VISIBLE);

        if (mShowHideAnimationEnabled) {
        	mContainerView.setAlpha(0);
        	AnimatorSet anim = new AnimatorSet();
        	AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 1));
        	anim.addListener(mAlphashowTopListener);
        	mCurrentShowAnim = anim;
        	anim.start();
        } else {
            mContainerView.setAlpha(1);
            mAlphashowTopListener.onAnimationEnd(null);
        }
    }
    
    @Override
    public void topActionbar_alphahide() {
    	if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }

        if (mShowHideAnimationEnabled) {
        	mContainerView.setAlpha(1);
        	AnimatorSet anim = new AnimatorSet();
        	AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 0));
        	anim.addListener(mAlphahideTopListener);
        	mCurrentShowAnim = anim;
        	anim.start();
        } else {
        	mAlphahideTopListener.onAnimationEnd(null);
        }
    }
    
    @Override
    public void actionbar_alphashow() {
    	if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        mContainerView.setVisibility(View.VISIBLE);

        if (mShowHideAnimationEnabled) {
        	mContainerView.setAlpha(0);
        	AnimatorSet anim = new AnimatorSet();
        	AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 1));
        	if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setAlpha(0);
                mSplitView.setVisibility(View.VISIBLE);
                b.with(ObjectAnimator.ofFloat(mSplitView, "alpha", 1));
            }
        	anim.addListener(mAlphashowListener);
        	mCurrentShowAnim = anim;
        	anim.start();
        } else {
            mContainerView.setAlpha(1);
            mAlphashowListener.onAnimationEnd(null);
        }
    }
    
    @Override
    public void actionbar_alphahide() {
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }

        if (mShowHideAnimationEnabled) {
            mContainerView.setAlpha(1);
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContainerView, "alpha", 0));
            if (mSplitView != null && mSplitView.getVisibility() == View.VISIBLE) {
                mSplitView.setAlpha(1);
                b.with(ObjectAnimator.ofFloat(mSplitView, "alpha", 0));
            }
            anim.addListener(mAlphahideListener);
            mCurrentShowAnim = anim;
            anim.start();
        } else {
        	mAlphahideListener.onAnimationEnd(null);
        }
    }
    
    /*NANJISTART::add::geofffeng::20120208*/
    final AnimatorListener twsSplitShowListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mCurrentShowAnim = null;
        mSplitView.setVisibility(View.VISIBLE);
            mContainerView.requestLayout();
        }
    };

    @Override
    public void setHomeButtonEnabledQQ(boolean enable) {
        setHomeButtonIconEnabled(enable);
    mActionView.setActionbarViewActivity(mActivity,false);
		mActionView.setActionbarViewDialog(mDialog);
    }
    @Override
    public void setHomeButtonIconEnabled(boolean enable) 
    {
        if(enable)
            {
            setDisplayShowHomeEnabled(true);
           setHomeButtonEnabled(true);
        setDisplayHomeAsUpEnabled(false);
            }
        mActionView.setActionbarViewActivity(mActivity,true);
		mActionView.setActionbarViewDialog(mDialog);
    }
    /*@Override
    public void setActionMenuItemCount(int itemCount) 
    {
        if(mActionView!=null)
            mActionView.setActionMenuItemCount(itemCount);
        if(mContextView!=null)
            mContextView.setActionMenuItemCount(itemCount);
    }*/
    
    @Override
    public void setEmbededTabEnable(boolean tabEnable)
    {
        setHasEmbeddedTabs(tabEnable);
    }
    @Override
    public void splitActionbar_Init(boolean isShow)
    {
        //default show the actionbar
        if(!isShow)
        {
            if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT) {
                mSplitView.setVisibility(View.GONE);
            }
            mCurrentShowAnim = null;
            completeDeferredDestroyActionMode();
        }
    }
    @Override
    public boolean splitActionbarIsHide()
    {
        if (mSplitView != null && mContextDisplayMode == CONTEXT_DISPLAY_SPLIT
                && mSplitView.getVisibility()==View.GONE)
        {
            return true;
        }
        return false;
    }
    @Override
    public void splitActionbar_show() {
        // Log.d("actionbarImpl","splitActionbar_show-----------------------------------1");
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mSplitView.getVisibility() == View.VISIBLE) {
            return;
        }

        /*NANJI-START::change::haoranma::2012-11-13*/
        // Log.d("actionbarImpl","splitActionbar_show-----------------------------------");
        final int animaHeight = (int) mContext.getResources().getDimension(
                R.dimen.tws_actionbar_split_height);
        if (mSplitView != null && mSplitView.getVisibility() == View.GONE) {
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mSplitView, "translationY",
                    -animaHeight, 0));

            anim.addListener(twsSplitShowListener);
            anim.setDuration(200);
            mCurrentShowAnim = anim;
            anim.start();
        }
    }
    @Override
    public void splitActionbar_hide() {
        // Log.d("actionbarImpl","splitActionbar_hide-----------------------------------1");
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        if (mContainerView.getVisibility() == View.GONE) {
            return;
        }
        /*NANJI-START::change::haoranma::2012-11-13*/
        // Log.d("actionbarImpl","splitActionbar_hide-----------------------------------");
        final int animaHeight = (int) mContext.getResources().getDimension(
                R.dimen.tws_actionbar_split_height);
        if (mSplitView != null && mSplitView.getVisibility() == View.VISIBLE) {
            AnimatorSet anim = new AnimatorSet();
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mSplitView, "translationY", 0,
                    animaHeight));

            anim.addListener(mSplitHideAnimListener);
            anim.setDuration(200);
            mCurrentShowAnim = anim;
            anim.start();
        }
    }
    /*NANJIEND::add::geofffeng::20120208*/
    
    public boolean isShowing() {
        return mContainerView.getVisibility() == View.VISIBLE;
    }

    void animateToMode(boolean toActionMode) {
        //QROM-START::do not show animation if actionbar is not shown::hendysu::2013-04-24
        // otherwise, actionbar may be displayed
        /*TypedArray a = mContext.obtainStyledAttributes(android.R.styleable.Theme);
        boolean withActionBar = a.getBoolean(android.R.styleable.Theme_windowActionBar, true);
        if(!withActionBar) {
            return;
        }
        //QROM-END::do not show animation if actionbar is not shown::hendysu::2013-04-24

        final int animaHeight=(int)mContext.getResources().getDimension(R.dimen.tws_actionbar_height);
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
        
        AnimatorSet anim = new AnimatorSet();
        if (toActionMode) {
           //show(false);
           
           mContextView.setVisibility(View.VISIBLE);
           
           AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContextView, "translationY",
                        -animaHeight, 0));
           if (mSplitView != null) {
                mSplitView.setVisibility(View.VISIBLE);
                mSplitView.setTranslationY(animaHeight);
                b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", 0));
           }
            anim.addListener(mSplitShowAnimListener);
            
        }
        else
        {
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContextView, "translationY",
                        0,-animaHeight));
           if (mSplitView != null) {
                mSplitView.setTranslationY(0);
                b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", animaHeight));
           }
            anim.addListener(mSplitHideAnimListener);
        }
        anim.setDuration(200);
        mCurrentShowAnim = anim;
        anim.start();*/
        if (mCurrentShowAnim != null) {
            mCurrentShowAnim.end();
        }
    	AnimatorSet anim = new AnimatorSet();
    	final int animaHeight=(int)mContext.getResources().getDimension(R.dimen.tws_actionbar_split_height);
       if (toActionMode) {
            show(false);
            mContextView.setVisibility(View.VISIBLE);
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContextView, "translationY",
                         -animaHeight, 0));
            if (mSplitView != null) {
                if (mContextDisplayMode != CONTEXT_DISPLAY_SPLIT) {
                    mSplitView.setVisibility(View.VISIBLE);
                    mSplitView.setTranslationY(animaHeight);
                    b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", 0));
                } else {
                    mContextView.playSplitMenuInAnimation();
                }
            }
             anim.addListener(mSplitShowAnimListener);
        } else {
            AnimatorSet.Builder b = anim.play(ObjectAnimator.ofFloat(mContextView, "translationY", 0,
                    -animaHeight));
            if (mSplitView != null) {
                if (mContextDisplayMode != CONTEXT_DISPLAY_SPLIT) {
                    mSplitView.setTranslationY(0);
                    b.with(ObjectAnimator.ofFloat(mSplitView, "translationY", animaHeight));
                    anim.addListener(mSplitHideAnimListener);
                } else {
                    anim.addListener(mSplitContextHideAnimListener);
                    if (splitActionbarIsHide()) {
                        splitActionbar_show();
                    }
                }
            }
        }

        mActionView.animateToVisibility(toActionMode ? View.GONE : View.VISIBLE);
        mContextView.animateToVisibility(toActionMode ? View.VISIBLE : View.GONE);
        if (mTabScrollView != null && !mActionView.hasEmbeddedTabs() && mActionView.isCollapsed()) {
            mTabScrollView.animateToVisibility(toActionMode ? View.GONE : View.VISIBLE);
        }
        anim.setDuration(200);
        mCurrentShowAnim = anim;
        anim.start();
    }

    public Context getThemedContext() {
        if (mThemedContext == null) {
            TypedValue outValue = new TypedValue();
            Resources.Theme currentTheme = mContext.getTheme();
            currentTheme.resolveAttribute(R.attr.actionBarWidgetTheme,
                    outValue, true);
            final int targetThemeRes = outValue.resourceId;
            
            if (targetThemeRes != 0 && mContext.getThemeResId() != targetThemeRes) {
                mThemedContext = new ContextThemeWrapper(mContext, targetThemeRes);
            } else {
                mThemedContext = mContext;
            }
        }
        return mThemedContext;
    }
    
    /**
     * @hide 
     */
    public class ActionModeImpl extends ActionMode implements MenuBuilder.Callback {
        private ActionMode.Callback mCallback;
        private MenuBuilder mMenu;
        private WeakReference<View> mCustomView;
        
        public ActionModeImpl(ActionMode.Callback callback) {
            mCallback = callback;
            mMenu = new MenuBuilder(getThemedContext())
                    .setDefaultShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            mMenu.setCallback(this);
        }

        @Override
        public MenuInflater getMenuInflater() {
            return new MenuInflater(getThemedContext());
        }

        @Override
        public Menu getMenu() {
            return mMenu;
        }

        @Override
        public void finish() {
            if (mActionMode != this) {
                // Not the active action mode - no-op
                return;
            }

            // If we were hidden before the mode was shown, defer the onDestroy
            // callback until the animation is finished and associated relayout
            // is about to happen. This lets apps better anticipate visibility
            // and layout behavior.
            if (mWasHiddenBeforeMode) {
                mDeferredDestroyActionMode = this;
                mDeferredModeDestroyCallback = mCallback;
            } else {
                mCallback.onDestroyActionMode(this);
            }
            mCallback = null;
            animateToMode(false);

            // Clear out the context mode views after the animation finishes
            mContextView.closeMode();
            mActionView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

            mActionMode = null;

            if (mWasHiddenBeforeMode) {
                hide();
            }
            mIsInActionMode = false;
        }

        @Override
        public void invalidate() {
            mMenu.stopDispatchingItemsChanged();
            try {
                mCallback.onPrepareActionMode(this, mMenu);
            } finally {
                mMenu.startDispatchingItemsChanged();
            }
        }

        public boolean dispatchOnCreate() {
            mMenu.stopDispatchingItemsChanged();
            try {
                return mCallback.onCreateActionMode(this, mMenu);
            } finally {
                mMenu.startDispatchingItemsChanged();
            }
        }

        @Override
        public void setCustomView(View view) {
            mContextView.setCustomView(view);
            mCustomView = new WeakReference<View>(view);
        }

        @Override
        public void setSubtitle(CharSequence subtitle) {
            mContextView.setSubtitle(subtitle);
        }

        @Override
        public void setTitle(CharSequence title) {
            mContextView.setTitle(title);
        }

        @Override
        public void setTitle(int resId) {
            setTitle(mContext.getResources().getString(resId));
        }

        @Override
        public void setSubtitle(int resId) {
            setSubtitle(mContext.getResources().getString(resId));
        }

        @Override
        public CharSequence getTitle() {
            return mContextView.getTitle();
        }

        @Override
        public CharSequence getSubtitle() {
            return mContextView.getSubtitle();
        }
        
        @Override
        public View getCustomView() {
            return mCustomView != null ? mCustomView.get() : null;
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            if (mCallback != null) {
                return mCallback.onActionItemClicked(this, item);
            } else {
                return false;
            }
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            if (mCallback == null) {
                return false;
            }

            if (!subMenu.hasVisibleItems()) {
                return true;
            }

            new MenuPopupHelper(getThemedContext(), subMenu).show();
            return true;
        }

        public void onCloseSubMenu(SubMenuBuilder menu) {
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (mCallback == null) {
                return;
            }
            invalidate();
            mContextView.showOverflowMenu();
        }
    }

    /**
     * @hide
     */
    public class TabImpl extends ActionBar.Tab {
        private ActionBar.TabListener mCallback;
        private Object mTag;
        private Drawable mIcon;
        private CharSequence mText;
        private CharSequence mContentDesc;
        private int mPosition = -1;
        private View mCustomView;

        @Override
        public Object getTag() {
            return mTag;
        }

        @Override
        public Tab setTag(Object tag) {
            mTag = tag;
            return this;
        }

        public ActionBar.TabListener getCallback() {
            return mCallback;
        }

        @Override
        public Tab setTabListener(ActionBar.TabListener callback) {
            mCallback = callback;
            return this;
        }

        @Override
        public View getCustomView() {
            return mCustomView;
        }

        @Override
        public Tab setCustomView(View view) {
            mCustomView = view;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public Tab setCustomView(int layoutResId) {
            return setCustomView(LayoutInflater.from(getThemedContext())
                    .inflate(layoutResId, null));
        }

        @Override
        public Drawable getIcon() {
            return mIcon;
        }

        @Override
        public int getPosition() {
            return mPosition;
        }

        public void setPosition(int position) {
            mPosition = position;
        }

        @Override
        public CharSequence getText() {
            return mText;
        }

        @Override
        public Tab setIcon(Drawable icon) {
            mIcon = icon;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public Tab setIcon(int resId) {
            return setIcon(mContext.getResources().getDrawable(resId));
        }

        @Override
        public Tab setText(CharSequence text) {
            mText = text;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public Tab setText(int resId) {
            return setText(mContext.getResources().getText(resId));
        }

        @Override
        public void select() {
            selectTab(this);
        }

        @Override
        public Tab setContentDescription(int resId) {
            return setContentDescription(mContext.getResources().getText(resId));
        }

        @Override
        public Tab setContentDescription(CharSequence contentDesc) {
            mContentDesc = contentDesc;
            if (mPosition >= 0) {
                mTabScrollView.updateTab(mPosition);
            }
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return mContentDesc;
        }
    }

    @Override
    public void setCustomView(View view) {
        mActionView.setCustomNavigationView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        view.setLayoutParams(layoutParams);
        mActionView.setCustomNavigationView(view);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
    }

    @Override
    public int getSelectedNavigationIndex() {
        switch (mActionView.getNavigationMode()) {
            case NAVIGATION_MODE_TABS:
                return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
            default:
                return -1;
        }
    }

    @Override
    public int getNavigationItemCount() {
        switch (mActionView.getNavigationMode()) {
            case NAVIGATION_MODE_TABS:
                return mTabs.size();
            default:
                return 0;
        }
    }

    @Override
    public int getTabCount() {
        return mTabs.size();
    }

    @Override
    public void setNavigationMode(int mode) {
        final int oldMode = mActionView.getNavigationMode();
        switch (oldMode) {
            case NAVIGATION_MODE_TABS:
                mSavedTabPosition = getSelectedNavigationIndex();
                selectTab(null);
                mTabScrollView.setVisibility(View.GONE);
                break;
        }
        mActionView.setNavigationMode(mode);
        switch (mode) {
            case NAVIGATION_MODE_TABS:
                ensureTabsExist();
                mTabScrollView.setVisibility(View.VISIBLE);
                if (mSavedTabPosition != INVALID_POSITION) {
                    setSelectedNavigationItem(mSavedTabPosition);
                    mSavedTabPosition = INVALID_POSITION;
                }
                break;
        }
        mActionView.setCollapsable(mode == NAVIGATION_MODE_TABS && !mHasEmbeddedTabs);
    }

    @Override
    public Tab getTabAt(int index) {
        return mTabs.get(index);
    }


    @Override
    public void setIcon(int resId) {
    }

    @Override
    public void setIcon(Drawable icon) {
    }

    @Override
    public void setLogo(int resId) {
    }

    @Override
    public void setLogo(Drawable logo) {
    }
    
    public boolean isMultiMode(){
        return mContextView.getVisibility() == View.VISIBLE;
    }
    
    //tws-start BottomBar Height::2014-7-23
    public int getBottomBarHeight() {
        int height = mContext.getResources().getDimensionPixelSize(R.dimen.tws_actionbar_split_height);
        return height;
    }
    //tws-end BottomBar Height::2014-7-23
    //tws-start ActionBar Back Button::2014-7-30
    public void twsSetBackOnclickEnabled(boolean enabled){
        mActionView.twsSetBackOnclickEnabled(enabled);
    }
    public boolean twsGetBackOnclickEnabled(){
        return mActionView.twsGetBackOnclickEnabled();
    }
    //tws-end ActionBar Back Button::2014-7-30
    //tws-start ActionMode BackBtn OnClickListener::2014-8-7
    public void twsSetActionModeBackOnClickListener(OnClickListener clickListener){
        mContextView.twsSetActionModeBackOnClickListener(clickListener);
    }
    //tws-end ActionMode BackBtn OnClickListener::2014-8-7
    //tws-start add for actionbar tab::2015-1-7
    public void twsSetActionBarTabHasTitle(boolean hasTitle) {
        mTabHasTitle = hasTitle;
    }
    //tws-end add for actionbar tab::2015-1-7
}
