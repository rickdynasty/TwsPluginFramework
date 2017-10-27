package com.example.plugindemo.activity.category.tab;

import android.app.LocalActivityManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TwsTabHost;
import android.widget.TwsTabHost.OnTabChangeListener;

import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.assistant.support.v4.view.ViewPager;
import com.tencent.tws.assistant.support.v4.view.ViewPager.OnPageChangeListener;
import com.tencent.tws.assistant.widget.TabIndicator;

public class TabViewpagerActivity extends TwsFragmentActivity {
	private static final String TAG = "TabViewpagerActivity2";

	private LocalActivityManager mLocalActivityManager;
	private TwsTabHost mTabHost;

	private static String TAB_FIRST = "推荐歌单";
	private static String TAB_SECOND = "手表歌曲";
	private static String TAB_THIRD = "tab3";

	private static int TAB_FIRST_INDEX = 0;
	private static int TAB_SECOND_INDEX = 1;
	private static int TAB_THIRD_INDEX = 2;
	private int mCurrentTabIndex = TAB_FIRST_INDEX;

	private ViewPagerFragment mNetWorkSettingsFragment;
	private ViewPagerFragment mSoundSettingsFragment;
	private ViewPagerFragment mDisplaySettingsFragment;

	private TabChangeListener mTabChangeListener = new TabChangeListener();
	private ViewPager mPager = null;
	private TabIndicator mIndicator = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		getTwsActionBar().setTitle("TabViewpagerActivity");
		mTabHost = TwsTabHost.inflateFromHost();
		setContentView(mTabHost);

		prepareTabViewAndActionBar();

		mTabHost.setCurrentTab(mCurrentTabIndex);
		mPager = mTabHost.getViewPager();
		mIndicator = mTabHost.getTabIndicator();
		ViewPager.OnPageChangeListener mChangeListener = new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		};
		mTabHost.setOnPageChangeListener(mChangeListener);
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String arg0) {

			}
		});
	}

	private void prepareTabViewAndActionBar() {
		if (mTabHost != null) {
			mTabHost.setup(mLocalActivityManager, getSupportFragmentManager(), mCurrentTabIndex);

			String tabTitle = TAB_FIRST;
			mNetWorkSettingsFragment = ViewPagerFragment.newInstance("NETWORK");
			mTabHost.addTab(mTabHost.newTabSpec(TAB_FIRST).setIndicator(tabTitle).setContent(mNetWorkSettingsFragment));

			tabTitle = TAB_SECOND;
			mSoundSettingsFragment = ViewPagerFragment.newInstance("SOUND");
			mTabHost.addTab(mTabHost.newTabSpec(TAB_SECOND).setIndicator(tabTitle).setContent(mSoundSettingsFragment));

			tabTitle = TAB_THIRD;
			mDisplaySettingsFragment = ViewPagerFragment.newInstance("DISPLAY");
			mTabHost.addTab(mTabHost.newTabSpec(TAB_THIRD).setIndicator(tabTitle).setContent(mDisplaySettingsFragment));

			mTabHost.setOnTabChangedListener(mTabChangeListener);
		}
	}

	private class TabChangeListener implements OnTabChangeListener {

		@Override
		public void onTabChanged(String tabId) {
			Log.d(TAG, "ExceptAlertTabHost onTabChanged tabId = " + tabId);

			if (tabId == TAB_FIRST) {
				mCurrentTabIndex = TAB_FIRST_INDEX;
				mNetWorkSettingsFragment.onResume();
			} else if (tabId == TAB_SECOND) {
				mCurrentTabIndex = TAB_SECOND_INDEX;
				mNetWorkSettingsFragment.onResume();
			} else if (tabId == TAB_THIRD) {
				mCurrentTabIndex = TAB_THIRD_INDEX;
				mDisplaySettingsFragment.onResume();
			}
		}
	}

}
