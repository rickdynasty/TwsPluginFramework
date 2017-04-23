package com.example.plugindemo.activity.category.tab;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.app.ActionBar.Tab;
import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.assistant.support.v4.app.FragmentManager;
import com.tencent.tws.assistant.support.v4.app.FragmentPagerAdapter;
import com.tencent.tws.assistant.support.v4.app.TwsFragmentActivity;
import com.tencent.tws.assistant.support.v4.view.ViewPager;

public class TwsActionBarTabSecond extends TwsFragmentActivity implements ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the three primary sections of the app. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	AppSectionsPagerAdapter mAppSectionsPagerAdapter;

	private final TabPagerListener mTabPagerListener = new TabPagerListener();

	/**
	 * The {@link ViewPager} that will display the three primary sections of the
	 * app, one at a time.
	 */
	ViewPager mViewPager;

	private int mLastPosition = -1;

	private LaunchpadSectionFragment mLaunchpadSectionFragment;
	private ListviewFragment mListviewFragment;
	private ListviewFragment mListviewFragment2;

	@Override
	protected void onCreate(Bundle arg0) {
		// requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(arg0);
		setContentView(R.layout.action_bar_tab);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

		// Set up the action bar.
		final ActionBar actionBar = getTwsActionBar();

		// Specify that the Home/Up button should not be enabled, since there is
		// no hierarchical
		// parent.
		actionBar.twsSetActionBarTabHasTitle(true);
		actionBar.setBackgroundDrawable(null);
		actionBar.twsSetTabWaveEnable(true);

		// Specify that we will be displaying tabs in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.twsSetTabTextSelectChange(true);

		// Set up the ViewPager, attaching the adapter and setting up a listener
		// for when the
		// user swipes between sections.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(mTabPagerListener);

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mAppSectionsPagerAdapter.getPageTitle(i))
			// .setIcon(R.drawable.ic_search)
					.setTabListener(this));
		}
	}

	private class AppSectionsPagerAdapter extends FragmentPagerAdapter {

		public AppSectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case 0:
				// The first section of the app is the most interesting -- it
				// offers
				// a launchpad into the other demonstrations in this example
				// application.
				if (mLaunchpadSectionFragment == null) {
					mLaunchpadSectionFragment = new LaunchpadSectionFragment();
				}
				return mLaunchpadSectionFragment;
			case 1:
				if (mListviewFragment == null) {
					mListviewFragment = new ListviewFragment();
				}
				return mListviewFragment;
			case 2:
				if (mListviewFragment2 == null) {
					mListviewFragment2 = new ListviewFragment();
				}
				return mListviewFragment2;
			default:
				// The other sections of the app are dummy placeholders.
				Fragment fragment = new ListviewFragment();
				return fragment;
			}
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "Section " + (position + 1);
		}
	}

	private class TabPagerListener implements ViewPager.OnPageChangeListener {
		private static final String TAG = "TABTS::TabPagerListener";

		TabPagerListener() {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			if (state == ViewPager.SCROLL_STATE_DRAGGING) {
				mLastPosition = mViewPager.getCurrentItem();
			} else if (state == ViewPager.SCROLL_STATE_IDLE) {
				mLastPosition = mViewPager.getCurrentItem();
				int count = mAppSectionsPagerAdapter.getCount();
				for (int i = 0; i < count; i++) {
					Fragment fragment = mAppSectionsPagerAdapter.getItem(i);
					if (fragment instanceof ListItemInterface) {
						((ListItemInterface) fragment).onListItemTranslationChange(mLastPosition, mLastPosition, 0.0f,
								i);
					}
				}
			}
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			ActionBar actionBar = getTwsActionBar();
			if (actionBar != null) {
				Log.v(TAG, "onPageScrolled ApiDemo");
				actionBar.twsSetPageScroll(position, positionOffset);
			}

			int count = mAppSectionsPagerAdapter.getCount();
			for (int i = 0; i < count; i++) {
				Fragment fragment = mAppSectionsPagerAdapter.getItem(i);
				if (fragment instanceof ListItemInterface) {
					((ListItemInterface) fragment).onListItemTranslationChange(mLastPosition, position, positionOffset,
							i);
				}
			}
		}

		@Override
		public void onPageSelected(int position) {
			ActionBar actionBar = getTwsActionBar();
			actionBar.setSelectedNavigationItem(position);
			actionBar.twsSetPageSelected(position);
		}
	}

	/**
	 * A fragment that launches other parts of the demo application.
	 */
	public static class LaunchpadSectionFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_section_launchpad, container, false);

			// Demonstration of a collection-browsing activity.
			rootView.findViewById(R.id.demo_collection_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// Intent intent = new Intent(getActivity(),
					// CollectionDemoActivity.class);
					// startActivity(intent);
				}
			});

			// Demonstration of navigating to external activities.
			rootView.findViewById(R.id.demo_external_activity).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// Create an intent that asks the user to pick a photo, but
					// using
					// FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET, ensures that
					// relaunching
					// the application from the device home screen does not
					// return
					// to the external activity.
					Intent externalActivityIntent = new Intent(Intent.ACTION_PICK);
					externalActivityIntent.setType("image/*");
					externalActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					startActivity(externalActivityIntent);
				}
			});

			return rootView;
		}
	}

	// /**
	// * A dummy fragment representing a section of the app, but that simply
	// displays dummy text.
	// */
	// public static class DummySectionFragment extends Fragment {
	//
	// public static final String ARG_SECTION_NUMBER = "section_number";
	//
	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// View rootView = inflater.inflate(R.layout.fragment_section_dummy,
	// container, false);
	// Bundle args = getArguments();
	// ((TextView) rootView.findViewById(android.R.id.text1)).setText(
	// getString(R.string.dummy_section_text, args.getInt(ARG_SECTION_NUMBER)));
	// return rootView;
	// }
	// }

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub

	}

	public static interface ListItemInterface {
		public void onListItemTranslationChange(int lastPosition, int position, float positionOffset, int index);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_bar_menu, menu);
		return super.onCreateOptionsMenu(menu);

	}

}
