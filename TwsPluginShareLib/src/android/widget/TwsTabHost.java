/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.widget;

import java.util.ArrayList;
import java.util.List;

import tws.component.log.TwsLog;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.assistant.support.v4.app.FragmentManager;
import com.tencent.tws.assistant.support.v4.app.FragmentTransaction;
import com.tencent.tws.assistant.support.v4.view.PagerAdapter;
import com.tencent.tws.assistant.support.v4.view.ViewPager;
import com.tencent.tws.assistant.widget.TabIndicator;
import com.tencent.tws.framework.HostProxy;
import com.tencent.tws.sharelib.R;

/**
 * Container for a tabbed window view. This object holds two children: a set of
 * tab labels that the user clicks to select a specific tab, and a FrameLayout
 * object that displays the contents of that page. The individual elements are
 * typically controlled using this container object, rather than setting values
 * on the child elements themselves.
 * 
 * <p>
 * See the <a href="{@docRoot}
 * resources/tutorials/views/hello-tabwidget.html">Tab Layout tutorial</a>.
 * </p>
 */
public class TwsTabHost extends FrameLayout implements ViewTreeObserver.OnTouchModeChangeListener {

	private static final String TAG = "TabHost";

	private TwsTabWidget mTabWidget;
	private FrameLayout mTabContent;
	private List<TabSpec> mTabSpecs = new ArrayList<TabSpec>(2);
	/**
	 * This field should be made private, so it is hidden from the SDK. {@hide
	 * 
	 * 
	 * }
	 */
	protected int mCurrentTab = -1;
	private View mCurrentView = null;
	/**
	 * This field should be made private, so it is hidden from the SDK. {@hide
	 * 
	 * 
	 * }
	 */
	protected LocalActivityManager mLocalActivityManager = null;
	private OnTabChangeListener mOnTabChangeListener;
	private OnKeyListener mTabKeyListener;

	private int mTabLayoutId;

	// //tws-start add tabIndicator::2014-9-23
	private TabIndicator mTabIndicator = null;
	// tws-end add tabIndicator::2014-9-23

	// QROM-START::support slide to switch tabs::hendysu::2013-05-06
	private FragmentManager mFragmentManager;
	private ViewPager mViewPager;
	private ViewPagerAdapter mViewPagerAdapter;
	private ViewPager.OnPageChangeListener mPagerListener;

	// tws-start add smooth scroll feature to tab::2014-11-18
	private boolean mInit = false;
	private int mSelectedColor = 0;
	private int mNormalColor = 0;
	private ColorStateList mTextColors = null;
	// private ColorStateList mNormalColors;
	// tws-end add smooth scroll feature to tab ::2014-11-18
	
	public static TwsTabHost inflateFromHost() {
		final LayoutInflater li = (LayoutInflater) HostProxy.getApplication().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		return (TwsTabHost) li.inflate(R.layout.tab_content_v4, null);
	}
	
	public static TwsTabHost inflateFromHost(String layoutResName) {
		final LayoutInflater li = (LayoutInflater) HostProxy.getApplication().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		final int id = HostProxy.getShareLayoutId(layoutResName);
		if (0 == id)
			return null;
		else {
			return (TwsTabHost) li.inflate(id, null);
		}
	}

	private ViewPager.OnPageChangeListener mViewPagerListener = new ViewPager.OnPageChangeListener() {
		public void onPageScrollStateChanged(int state) {
			if (mPagerListener != null) {
				mPagerListener.onPageScrollStateChanged(state);
			}
		}

		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			if (mPagerListener != null) {
				mPagerListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}

			// determin all tab indicator suppport text fade animation

			for (TabSpec tabSpec : mTabSpecs) {
				if (tabSpec.mIndicatorStrategy == null && tabSpec.mIndicatorStrategy.supportTextFade() == false)
					return;
			}

			if (position < 0 || position >= mTabSpecs.size() - 1)
				return;

			if (positionOffset < 0 || positionOffset > 1)
				return;

			if (positionOffset == 0.0 || positionOffset == 1.0 || position == mTabSpecs.size() - 1) {
				cancelScrollState();
				return;
			}

			if (mInit == false) {
				mInit = true;

				int normalIndex = 0;
				int selectIndex = mCurrentTab;

				if (normalIndex == selectIndex)
					normalIndex = 1;

				mNormalColor = mTabSpecs.get(normalIndex).mIndicatorStrategy.getTextColor(mTabWidget.getChildTabViewAt(normalIndex));
				mSelectedColor = mTabSpecs.get(selectIndex).mIndicatorStrategy.getTextColor(mTabWidget.getChildTabViewAt(selectIndex));
				mTextColors = mTabSpecs.get(normalIndex).mIndicatorStrategy.getTextColors(mTabWidget.getChildTabViewAt(normalIndex));
			}

			float la = ((float) ((mSelectedColor >> 24) & 0xFF)) * (1 - positionOffset) + ((float) ((mNormalColor >> 24) & 0xFF)) * positionOffset;
			float lr = ((float) ((mSelectedColor >> 16) & 0xFF)) * (1 - positionOffset) + ((float) ((mNormalColor >> 16) & 0xFF)) * positionOffset;
			float lg = ((float) ((mSelectedColor >> 8) & 0xFF)) * (1 - positionOffset) + ((float) ((mNormalColor >> 8) & 0xFF)) * positionOffset;
			float lb = ((float) ((mSelectedColor >> 0) & 0xFF)) * (1 - positionOffset) + ((float) ((mNormalColor >> 0) & 0xFF)) * positionOffset;

			float ra = ((float) ((mSelectedColor >> 24) & 0xFF)) * positionOffset + ((float) ((mNormalColor >> 24) & 0xFF)) * (1 - positionOffset);
			float rr = ((float) ((mSelectedColor >> 16) & 0xFF)) * positionOffset + ((float) ((mNormalColor >> 16) & 0xFF)) * (1 - positionOffset);
			float rg = ((float) ((mSelectedColor >> 8) & 0xFF)) * positionOffset + ((float) ((mNormalColor >> 8) & 0xFF)) * (1 - positionOffset);
			float rb = ((float) ((mSelectedColor >> 0) & 0xFF)) * positionOffset + ((float) ((mNormalColor >> 0) & 0xFF)) * (1 - positionOffset);

			int leftColor = android.graphics.Color.argb((int) la, (int) lr, (int) lg, (int) lb);
			int rightColor = android.graphics.Color.argb((int) ra, (int) rr, (int) rg, (int) rb);

			mTabSpecs.get(position).mIndicatorStrategy.setTextColor(mTabWidget.getChildTabViewAt(position), leftColor);
			mTabSpecs.get(position + 1).mIndicatorStrategy.setTextColor(mTabWidget.getChildTabViewAt(position + 1), rightColor);

		}

		public void onPageSelected(int position) {
			if (mPagerListener != null) {
				mPagerListener.onPageSelected(position);
			}
			setCurrentTab(position);
		}
	};

	private void cancelScrollState() {
		final int tabCount = mTabSpecs.size();

		for (int i = 0; i < tabCount; i++) {
			mTabSpecs.get(i).mIndicatorStrategy.setTextColors(mTabWidget.getChildTabViewAt(i), mTextColors);
		}
	}

	// QROM-END::support slide to switch tabs::hendysu::2013-05-06

	private int mInitialTabIndex = 0;

	//DM 固定逻辑 两个tab之间的间距固定16dp
	boolean needMarginLeft = false;
	private int mTabspecSpace = 0;

	public TwsTabHost(Context context) {
		super(context);
		initTabHost();
	}

	public TwsTabHost(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabWidget, R.attr.tabWidgetStyle, 0);

		mTabLayoutId = a.getResourceId(R.styleable.TabWidget_tabLayout, R.layout.tws_tab_indicator_holo);
		a.recycle();

		mTabspecSpace = getResources().getDimensionPixelSize(R.dimen.tws_tabspec_space);
		initTabHost();
	}

	private void initTabHost() {
		setFocusableInTouchMode(true);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

		mCurrentTab = -1;
		mCurrentView = null;
	}

	/**
	 * Get a new {@link TabSpec} associated with this tab host.
	 * 
	 * @param tag
	 *            required tag of tab.
	 */
	public TabSpec newTabSpec(String tag) {
		return new TabSpec(tag, this);
	}

	/**
	 * <p>
	 * Call setup() before adding tabs if loading TabHost using findViewById().
	 * <i><b>However</i></b>: You do not need to call setup() after getTabHost()
	 * in {@link android.app.TabActivity TabActivity}. Example:
	 * </p>
	 * 
	 * <pre>
	 * mTabHost = (TabHost) findViewById(R.id.tabhost);
	 * mTabHost.setup();
	 * mTabHost.addTab(TAB_TAG_1, "Hello, world!", "Tab 1");
	 */
	public void setup() {
		mTabWidget = (TwsTabWidget) findViewById(R.id.tabs);
		if (mTabWidget == null) {
			throw new RuntimeException("Your TabHost must have a TabWidget whose id attribute is 'R.id.tabs'");
		}

		// KeyListener to attach to all tabs. Detects non-navigation keys
		// and relays them to the tab content.
		mTabKeyListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_ENTER:
					return false;

				}
				mTabContent.requestFocus(View.FOCUS_FORWARD);
				return mTabContent.dispatchKeyEvent(event);
			}

		};

		mTabWidget.setTabSelectionListener(new TwsTabWidget.OnTabSelectionChanged() {
			public void onTabSelectionChanged(int tabIndex, boolean clicked) {
				setCurrentTab(tabIndex);
				if (clicked) {
					mTabContent.requestFocus(View.FOCUS_FORWARD);
				}
			}
		});

		mTabContent = (FrameLayout) findViewById(R.id.tabcontent);
		if (mTabContent == null) {
			throw new RuntimeException("Your TabHost must have a FrameLayout whose id attribute is " + "'R.id.tabcontent'");
		}

		// if FragmentManager is not passed in when setup, then not support
		// slide tab switch
		TwsLog.d(TAG, "mFragmentManager:"+mFragmentManager);
		if (mFragmentManager != null) {
			// R.id.tabviewpager is a ViewPager used for slide tab switch
			mViewPager = (ViewPager) findViewById(R.id.tabviewpager);
			if (mViewPager != null) {
				mViewPagerAdapter = new ViewPagerAdapter();
				mViewPager.setAdapter(mViewPagerAdapter);
				mViewPager.setOffscreenPageLimit(2);
				// tws-start add tabIndicator::2014-9-23
				mTabIndicator = (TabIndicator) findViewById(R.id.tab_indicator);
				if (mTabIndicator != null) {
					mTabIndicator.setViewPager(mViewPager);
					mTabIndicator.setOnPageChangeListener(mViewPagerListener);
					// 当前Tab是不等分居中的，这样indicator就需要知道tab的位置信息
					mTabWidget.setOnTabItemCenterPosListener(mTabIndicator);
				} else {
					mViewPager.setOnPageChangeListener(mViewPagerListener);
				}
				// tws-end add tabIndicator::2014-9-23
			} else {
				Log.w(TAG, "cannot find a ViewPager named R.id.tabviewpager, " + "slide to switch tab will be N/A");
			}
		}
	}

	@Override
	public void sendAccessibilityEvent(int eventType) {
		/* avoid super class behavior - TabWidget sends the right events */
	}

	/**
	 * If you are using {@link TabSpec#setContent(android.content.Intent)}, this
	 * must be called since the activityGroup is needed to launch the local
	 * activity.
	 * 
	 * This is done for you if you extend {@link android.app.TabActivity}.
	 * 
	 * @param activityGroup
	 *            Used to launch activities for tab content.
	 */
//	public void setup(LocalActivityManager activityGroup) {
//		mLocalActivityManager = activityGroup;
//		Activity curActivity = mLocalActivityManager.getCurrentActivity();
//		setup(curActivity != null ? curActivity.getFragmentManager() : null);
//	}

	// if you want to enable slide tab switch, pass in a valid FragmentManager
	public void setup(LocalActivityManager activityGroup, FragmentManager fragmentManager) {
		setup(fragmentManager);
		mLocalActivityManager = activityGroup;
	}

	// if you want to enable slide tab switch, pass in a valid FragmentManager
	public void setup(FragmentManager fragmentManager) {
		if (fragmentManager == null) {
			Log.w(TAG, "fragment manager not provided, slide switch will be N/A");
		} else {
			mFragmentManager = fragmentManager;
		}

		setup();
	}

	// setup methods with initial index specified
	public void setup(int initialTab) {
		mInitialTabIndex = initialTab;
		setup();
	}

//	public void setup(LocalActivityManager activityGroup, int initialTab) {
//		mInitialTabIndex = initialTab;
//		setup(activityGroup);
//	}

	public void setup(LocalActivityManager activityGroup, FragmentManager fragmentManager, int initialTab) {
		mInitialTabIndex = initialTab;
		setup(activityGroup, fragmentManager);
	}

	public void setup(FragmentManager fragmentManager, int initialTab) {
		mInitialTabIndex = initialTab;
		setup(fragmentManager);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		final ViewTreeObserver treeObserver = getViewTreeObserver();
		treeObserver.addOnTouchModeChangeListener(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		final ViewTreeObserver treeObserver = getViewTreeObserver();
		treeObserver.removeOnTouchModeChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void onTouchModeChanged(boolean isInTouchMode) {
		if (!isInTouchMode) {
			// leaving touch mode.. if nothing has focus, let's give it to
			// the indicator of the current tab
			if (mCurrentView != null && (!mCurrentView.hasFocus() || mCurrentView.isFocused())) {
				mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
			}
		}
	}

	/**
	 * Add a tab.
	 * 
	 * @param tabSpec
	 *            Specifies how to create the indicator and content.
	 */
	public void addTab(TabSpec tabSpec) {
		if (tabSpec.mIndicatorStrategy == null) {
			throw new IllegalArgumentException("you must specify a way to create the tab indicator.");
		}

		if (tabSpec.mContentStrategy == null) {
			throw new IllegalArgumentException("you must specify a way to create the tab content");
		}
		View tabIndicator = tabSpec.mIndicatorStrategy.createIndicatorView();
		tabIndicator.setOnKeyListener(mTabKeyListener);

		// If this is a custom view, then do not draw the bottom strips for
		// the tab indicators.
		if (tabSpec.mIndicatorStrategy instanceof ViewIndicatorStrategy) {
			mTabWidget.setStripEnabled(false);
		}

		// a bug in the original android source, the LinearLayout divider will
		// offset right
		// here set tab indicator's left margin to make up
		if ((tabIndicator instanceof LinearLayout) && (mTabWidget.getShowDividers() & LinearLayout.SHOW_DIVIDER_MIDDLE) != 0) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
			lp.leftMargin += mTabWidget.getDividerWidth();
			if (needMarginLeft)
				lp.leftMargin += mTabspecSpace;
			
			tabIndicator.setLayoutParams(lp);
			tabIndicator.requestLayout();
		}
		if (tabIndicator.getLayoutParams() != null) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
			tabIndicator.setLayoutParams(lp);
			tabIndicator.requestLayout();
		} else {
			mTabIndicator.setVisibility(View.GONE);
		}

		mTabWidget.addView(tabIndicator);
		mTabSpecs.add(tabSpec);
		needMarginLeft = true;

		if (mCurrentTab == -1) {
			if (mTabSpecs.size() > mInitialTabIndex) {
				setCurrentTab(mInitialTabIndex);
			}
		}
	}

	/**
	 * Removes all tabs from the tab widget associated with this tab host.
	 */
	public void clearAllTabs() {
		mTabWidget.removeAllViews();
		needMarginLeft = false;
		initTabHost();
		mTabContent.removeAllViews();
		mTabSpecs.clear();
		requestLayout();
		invalidate();
	}

	public TwsTabWidget getTabWidget() {
		return mTabWidget;
	}

	public int getCurrentTab() {
		return mCurrentTab;
	}

	public String getCurrentTabTag() {
		if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) {
			return mTabSpecs.get(mCurrentTab).getTag();
		}
		return null;
	}

	public View getCurrentTabView() {
		if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) {
			return mTabWidget.getChildTabViewAt(mCurrentTab);
		}
		return null;
	}

	public View getCurrentView() {
		return mCurrentView;
	}

	public void setCurrentTabByTag(String tag) {
		int i;
		for (i = 0; i < mTabSpecs.size(); i++) {
			if (mTabSpecs.get(i).getTag().equals(tag)) {
				setCurrentTab(i);
				break;
			}
		}
	}

	/**
	 * Get the FrameLayout which holds tab content
	 */
	public FrameLayout getTabContentView() {
		return mTabContent;
	}

	public ViewPager getViewPager() {
		return mViewPager;
	}
	
	public TabIndicator getTabIndicator() {
		return mTabIndicator;
	}

	/**
	 * get single tab content view by tag
	 */
	public View getTabContentViewByTag(String tag) {
		for (int i = 0; i < mTabSpecs.size(); i++) {
			if (mTabSpecs.get(i).getTag().equals(tag)) {
				return mTabSpecs.get(i).mContentStrategy.getContentView();
			}
		}

		return null;
	}

	/**
	 * get tab indicator view by tag
	 */
	public View getIndicatorViewByTag(String tag) {
		for (int i = 0; i < mTabSpecs.size(); i++) {
			if (mTabSpecs.get(i).getTag().equals(tag)) {
				return mTabSpecs.get(i).mIndicatorStrategy.getIndicatorView();
			}
		}

		return null;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		final boolean handled = super.dispatchKeyEvent(event);

		// unhandled key ups change focus to tab indicator for embedded
		// activities
		// when there is nothing that will take focus from default focus
		// searching
		if (!handled && (event.getAction() == KeyEvent.ACTION_DOWN) && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) && (mCurrentView != null) && (mCurrentView.isRootNamespace())
				&& (mCurrentView.hasFocus()) && (mCurrentView.findFocus().focusSearch(View.FOCUS_UP) == null)) {
			mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
			playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
			return true;
		}
		return handled;
	}

	@Override
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		if (mCurrentView != null) {
			mCurrentView.dispatchWindowFocusChanged(hasFocus);
		}
	}

	public void setCurrentTab(int index) {
		if (index < 0 || index >= mTabSpecs.size()) {
			return;
		}

		if (index == mCurrentTab) {
			return;
		}

		switchToTab(index);
	}

	/**
	 * Register a callback to be invoked when the selected state of any of the
	 * items in this list changes
	 * 
	 * @param l
	 *            The callback that will run
	 */
	public void setOnTabChangedListener(OnTabChangeListener l) {
		mOnTabChangeListener = l;
	}

	private void invokeOnTabChangeListener() {
		if (mOnTabChangeListener != null) {
			mOnTabChangeListener.onTabChanged(getCurrentTabTag());
		}
	}

	/**
	 * Interface definition for a callback to be invoked when tab changed
	 */
	public interface OnTabChangeListener {
		void onTabChanged(String tabId);
	}

	/**
	 * Makes the content of a tab when it is selected. Use this if your tab
	 * content needs to be created on demand, i.e. you are not showing an
	 * existing view or starting an activity.
	 */
	public interface TabContentFactory {
		/**
		 * Callback to make the tab contents
		 * 
		 * @param tag
		 *            Which tab was selected.
		 * @return The view to display the contents of the selected tab.
		 */
		View createTabContent(String tag);
	}

	/**
	 * A tab has a tab indicator, content, and a tag that is used to keep track
	 * of it. This builder helps choose among these options.
	 * 
	 * For the tab indicator, your choices are: 1) set a label 2) set a label
	 * and an icon
	 * 
	 * For the tab content, your choices are: 1) the id of a {@link View} 2) a
	 * {@link TabContentFactory} that creates the {@link View} content. 3) an
	 * {@link Intent} that launches an {@link android.app.Activity}.
	 */
	public class TabSpec {

		private String mTag;

		private IndicatorStrategy mIndicatorStrategy;
		private ContentStrategy mContentStrategy;

		private TwsTabHost mTabHost;

		private TabSpec(String tag, TwsTabHost tabHost) {
			mTag = tag;
			mTabHost = tabHost;
		}

		/**
		 * Specify a label as the tab indicator.
		 */
		public TabSpec setIndicator(CharSequence label) {
			mIndicatorStrategy = new LabelIndicatorStrategy(label);
			return this;
		}

		/**
		 * Specify a label and icon as the tab indicator.
		 */
		public TabSpec setIndicator(CharSequence label, Drawable icon) {
			mIndicatorStrategy = new LabelAndIconIndicatorStrategy(label, icon);
			return this;
		}

		/**
		 * Specify a view as the tab indicator.
		 */
		public TabSpec setIndicator(View view) {
			mIndicatorStrategy = new ViewIndicatorStrategy(view);
			return this;
		}

		/**
		 * Specify the id of the view that should be used as the content of the
		 * tab.
		 */
		public TabSpec setContent(int viewId) {
			if (mFragmentManager == null) {
				// traditional tab style
				mContentStrategy = new ViewIdContentStrategy(viewId);
			} else {
				// tws tab style, with animation
				mContentStrategy = new ViewIdContentStrategy2(viewId, mTag);
			}
			return this;
		}

		/**
		 * Specify a
		 * {@link com.tencent.tws.assistant.widget.TabHost.TabContentFactory} to
		 * use to create the content of the tab.
		 */
		public TabSpec setContent(TabContentFactory contentFactory) {
			if (mFragmentManager == null) {
				// traditional tab style
				mContentStrategy = new FactoryContentStrategy(mTag, contentFactory);
			} else {
				// tws tab style, with animation
				mContentStrategy = new FactoryContentStrategy2(mTag, contentFactory);
			}
			return this;
		}

		/**
		 * Specify an intent to use to launch an activity as the tab content.
		 */
		public TabSpec setContent(Intent intent) {
			mContentStrategy = new IntentContentStrategy(mTag, intent);
			return this;
		}

		// support user-defined fragment as content
		public TabSpec setContent(Fragment fragment) {
			mContentStrategy = new FragmentContentStrategy(fragment, mTag, mTabHost);
			return this;
		}

		public String getTag() {
			return mTag;
		}
	}

	/**
	 * Specifies what you do to create a tab indicator.
	 */
	private static interface IndicatorStrategy {

		/**
		 * Return the view for the indicator.
		 */
		View createIndicatorView();

		// return the view of the indicator
		View getIndicatorView();

		// TODO: should add another interface
		/**
		 * is support text fade animation?
		 */
		boolean supportTextFade();

		int getTextColor(View view);

		ColorStateList getTextColors(View view);

		void setTextColor(View view, int color);

		void setTextColors(View view, ColorStateList colors);
	}

	/**
	 * Specifies what you do to manage the tab content.
	 */
	private static interface ContentStrategy {

		/**
		 * Return the content view. The view should may be cached locally.
		 */
		View getContentView();

		/**
		 * Perhaps do something when the tab associated with this content has
		 * been closed (i.e make it invisible, or remove it).
		 */
		void tabClosed();
	}

	/**
	 * How to create a tab indicator that just has a label.
	 */
	private class LabelIndicatorStrategy implements IndicatorStrategy {

		private final CharSequence mLabel;
		private View mView;

		private LabelIndicatorStrategy(CharSequence label) {
			mLabel = label;
		}

		public View createIndicatorView() {
			final Context context = getContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View tabIndicator = inflater.inflate(mTabLayoutId, mTabWidget, false);

			final TextView tv = (TextView) tabIndicator.findViewById(R.id.title);
			tv.setText(mLabel);

			mView = tabIndicator;

			return tabIndicator;
		}

		public View getIndicatorView() {
			return mView;
		}

		public boolean supportTextFade() {
			return true;
		}

		public int getTextColor(View view) {
			if (view == null)
				return 0;

			final TextView tv = (TextView) view.findViewById(R.id.title);
			return tv.getCurrentTextColor();
		}

		public void setTextColor(View view, int color) {
			if (view == null)
				return;

			final TextView tv = (TextView) view.findViewById(R.id.title);
			tv.setTextColor(color);
		}

		public ColorStateList getTextColors(View view) {
			if (view == null)
				return null;

			final TextView tv = (TextView) view.findViewById(R.id.title);
			return tv.getTextColors();
		}

		public void setTextColors(View view, ColorStateList colors) {
			if (view == null || colors == null)
				return;

			final TextView tv = (TextView) view.findViewById(R.id.title);
			tv.setTextColor(colors);
		}
	}

	/**
	 * How we create a tab indicator that has a label and an icon
	 */
	private class LabelAndIconIndicatorStrategy implements IndicatorStrategy {

		private final CharSequence mLabel;
		private final Drawable mIcon;
		private View mView;

		private LabelAndIconIndicatorStrategy(CharSequence label, Drawable icon) {
			mLabel = label;
			mIcon = icon;
		}

		public View createIndicatorView() {
			final Context context = getContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View tabIndicator = inflater.inflate(mTabLayoutId, mTabWidget,  false);

			final TextView tv = (TextView) tabIndicator.findViewById(R.id.title);
			final ImageView iconView = (ImageView) tabIndicator.findViewById(R.id.icon);

			// when icon is gone by default, we're in exclusive mode
			final boolean exclusive = iconView.getVisibility() == View.GONE;
			final boolean bindIcon = !exclusive || TextUtils.isEmpty(mLabel);

			tv.setText(mLabel);

			if (bindIcon && mIcon != null) {
				iconView.setImageDrawable(mIcon);
				iconView.setVisibility(VISIBLE);
			}

			mView = tabIndicator;

			return tabIndicator;
		}

		public View getIndicatorView() {
			return mView;
		}

		public boolean supportTextFade() {
			return false;
		}

		public int getTextColor(View view) {
			return 0;
		}

		public void setTextColor(View view, int color) {
		}

		public ColorStateList getTextColors(View view) {
			return null;
		}

		public void setTextColors(View view, ColorStateList colors) {
		}
	}

	/**
	 * How to create a tab indicator by specifying a view.
	 */
	private class ViewIndicatorStrategy implements IndicatorStrategy {

		private final View mView;

		private ViewIndicatorStrategy(View view) {
			mView = view;
		}

		public View createIndicatorView() {
			return mView;
		}

		public View getIndicatorView() {
			return mView;
		}

		public boolean supportTextFade() {
			return false;
		}

		public int getTextColor(View view) {
			return 0;
		}

		public void setTextColor(View view, int color) {
		}

		public ColorStateList getTextColors(View view) {
			return null;
		}

		public void setTextColors(View view, ColorStateList colors) {
		}
	}

	/**
	 * How to create the tab content via a view id.
	 */
	private class ViewIdContentStrategy implements ContentStrategy {

		private final View mView;

		private ViewIdContentStrategy(int viewId) {
			mView = mTabContent.findViewById(viewId);
			if (mView != null) {
				mView.setVisibility(View.GONE);
			} else {
				throw new RuntimeException("Could not create tab content because " + "could not find view with id " + viewId);
			}
		}

		public View getContentView() {
			mView.setVisibility(View.VISIBLE);
			return mView;
		}

		public void tabClosed() {
			mView.setVisibility(View.GONE);
		}
	}

	/**
	 * How tab content is managed using {@link TabContentFactory}.
	 */
	private class FactoryContentStrategy implements ContentStrategy {
		private View mTabContent;
		private final CharSequence mTag;
		private TabContentFactory mFactory;

		public FactoryContentStrategy(CharSequence tag, TabContentFactory factory) {
			mTag = tag;
			mFactory = factory;
		}

		public View getContentView() {
			if (mTabContent == null) {
				mTabContent = mFactory.createTabContent(mTag.toString());
			}
			mTabContent.setVisibility(View.VISIBLE);
			return mTabContent;
		}

		public void tabClosed() {
			mTabContent.setVisibility(View.GONE);
		}
	}

	/**
	 * How tab content is managed via an {@link Intent}: the content view is the
	 * decorview of the launched activity.
	 */
	private class IntentContentStrategy implements ContentStrategy {

		private final String mTag;
		private final Intent mIntent;

		private View mLaunchedView;

		private IntentContentStrategy(String tag, Intent intent) {
			mTag = tag;
			mIntent = intent;
		}

		public View getContentView() {
			if (mLocalActivityManager == null) {
				throw new IllegalStateException("Did you forget to call 'public void setup(LocalActivityManager activityGroup)'?");
			}
			final Window w = mLocalActivityManager.startActivity(mTag, mIntent);
			final View wd = w != null ? w.getDecorView() : null;
			if (mLaunchedView != wd && mLaunchedView != null) {
				if (mLaunchedView.getParent() != null) {
					mTabContent.removeView(mLaunchedView);
				}
			}
			mLaunchedView = wd;

			// XXX Set FOCUS_AFTER_DESCENDANTS on embedded activities for now so
			// they can get
			// focus if none of their children have it. They need focus to be
			// able to
			// display menu items.
			//
			// Replace this with something better when Bug 628886 is fixed...
			//
			if (mLaunchedView != null) {
				mLaunchedView.setVisibility(View.VISIBLE);
				mLaunchedView.setFocusableInTouchMode(true);
				((ViewGroup) mLaunchedView).setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
			}
			return mLaunchedView;
		}

		public void tabClosed() {
			if (mLaunchedView != null) {
				mLaunchedView.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * tws view id content strategy, the view specified by id will be combined
	 * with fragment thus can be added to a ViewPager and be switched by sliding
	 */
	private class ViewIdContentStrategy2 extends Fragment implements ContentStrategy {

		private final View mView;
		private Fragment mFragment;

		private ViewIdContentStrategy2(int viewId, String tag) {
			mView = mTabContent.findViewById(viewId);
			if (mView != null) {
				// in case use viewpager to switch view, remove it from
				// tabcontent first
				// because a view cannot be placed in more than 1 parent view
				((ViewGroup) mView.getParent()).removeView(mView);
				mFragment = new Fragment() {
					public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
						return mView;
					}
				};

				FragmentTransaction transaction = mFragmentManager.beginTransaction();
				transaction.add(R.id.tabviewpager, mFragment, tag);
				// transaction.hide(mFragment);
				transaction.commit();
				// mFragmentManager.executePendingTransactions();
			} else {
				throw new RuntimeException("Could not create tab content because " + "could not find view with id 0x" + Integer.toHexString(viewId));
			}
		}

		public View getContentView() {
			return mView;
		}

		public void tabClosed() {
			// do nothing
		}
	}

	/**
	 * fragment content strategy, tab users can directly set fragment as tab
	 * content
	 */
	private class FragmentContentStrategy implements ContentStrategy {

		private Fragment mFragment;

		private FragmentContentStrategy(Fragment fragment, String tag, TwsTabHost tabHost) {
			mFragment = fragment;
			FragmentTransaction transaction = mFragmentManager.beginTransaction();
			transaction.add(R.id.tabviewpager, fragment, tag);
			// transaction.hide(fragment);
			transaction.commit();
			// mFragmentManager.executePendingTransactions();
		}

		public View getContentView() {
			return mFragment.getView();
		}

		public void tabClosed() {
			// do nothing
		}
	}

	/**
	 * tws factory content strategy, the content view created by caller will be
	 * combined with a fragment
	 */
	private class FactoryContentStrategy2 implements ContentStrategy {
		private View mTabContent;
		private String mTag;
		private TabContentFactory mFactory;
		private TwsFactoryContentStrategy2FragmentV4 mFragment;

		public FactoryContentStrategy2(String tag, TabContentFactory factory) {
			mTag = tag;
			mFactory = factory;

			mTabContent = mFactory.createTabContent(mTag);
			// tws-start modify fragment not an empty
			// constructor::2014-12-19
			mFragment = new TwsFactoryContentStrategy2FragmentV4(mTabContent);
			// tws-end modify fragment not an empty
			// constructor::2014-12-19
			FragmentTransaction transaction = mFragmentManager.beginTransaction();
			transaction.add(R.id.tabviewpager, mFragment, tag);
			// transaction.hide(mFragment);
			transaction.commit();
			// mFragmentManager.executePendingTransactions();
		}

		public View getContentView() {
			return mTabContent;
		}

		public void tabClosed() {
			// do nothing
		}
	}

	/**
	 * show the tab indicator animcation
	 */
	void switchToTab(int index) {
		// notify old tab content
		if (mCurrentTab != -1) {
			mTabSpecs.get(mCurrentTab).mContentStrategy.tabClosed();
		}

		mCurrentTab = index;
		final TwsTabHost.TabSpec spec = mTabSpecs.get(index);

		// Call the tab widget's focusCurrentTab(), instead of just
		// selecting the tab.
		mTabWidget.focusCurrentTab(index);

		if (mFragmentManager == null) {
			// no fragment manager specified, do traditional tab content switch
			// tab content
			mCurrentView = spec.mContentStrategy.getContentView();

			if (mCurrentView.getParent() == null) {
				mTabContent.addView(mCurrentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			}

			if (!mTabWidget.hasFocus()) {
				// if the tab widget didn't take focus (likely because we're in
				// touch mode)
				// give the current tab content view a shot
				mCurrentView.requestFocus();
			}
		} else {
			// show target tab content via ViewPager
			mViewPager.setCurrentItem(index);
		}

		// mTabContent.requestFocus(View.FOCUS_FORWARD);
		invokeOnTabChangeListener();
	}

	/**
	 * Adapter for ViewPager, implements only necessary abstract methods
	 */
	private class ViewPagerAdapter extends PagerAdapter {

		private FragmentTransaction mCurTransaction;

		public ViewPagerAdapter() {
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}
			mCurTransaction.detach((Fragment) object);
		}

		@Override
		public void finishUpdate(View container) {
		}

		public void finishUpdate(ViewGroup container) {
		}

		@Override
		public Object instantiateItem(View container, int position) {
			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}
			Fragment fragment = mFragmentManager.findFragmentByTag(mTabSpecs.get(position).getTag());
			if (fragment != null) {
				mCurTransaction.attach(fragment);
			}
			return fragment;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((Fragment) object).getView();
		}

		@Override
		public int getCount() {
			return mTabSpecs.size();
		}
	}

	/**
	 * get ViewPager callback
	 * 
	 * @param ViewPager
	 *            .OnPageChangeListener
	 */
	public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
		mPagerListener = listener;
	}
}
