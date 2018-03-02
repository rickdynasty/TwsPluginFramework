package com.example.pluginbluetooth.screens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.utils.SystemUtils;
import com.example.pluginbluetooth.widget.AnimatedToolbar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class BaseFragment extends Fragment implements BasePresenter.ActivityLauncher {

	private static final String TAG = BaseFragment.class.getSimpleName();
	private BasePresenter mBasePresenter;

	protected AnimatedToolbar mToolbar;
	private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;

	/*
	 * Android restores scroll position after resume and don't report it back in
	 * view tree observer, hence we need this to keep track of it by ourselves.
	 */
	private int mSavedScrollOffset;

	public BaseFragment() {
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				onScrolled();
			}
		};

		setupToolbar(view);

		refreshToolbar(0);
	}

	public MainController getMainController() {
		final FragmentActivity activity = getActivity();
		if (activity instanceof MainController) {
			return (MainController) activity;
		} else {
			throw new RuntimeException("Containing Activity needs to implement MainController");
		}
	}

	protected void onScrolled() {
		refreshToolbar(getScrollOffset());
	}

	public abstract String getName();

	public String getFeaturePathName() {
		return getString(R.string.app_name);
	}

	@Override
	public void onResume() {
		super.onResume();
		ScrollView scrollView = getScrollView();
		if (scrollView != null) {
			refreshToolbar(mSavedScrollOffset);
			scrollView.getViewTreeObserver().addOnScrollChangedListener(mScrollChangedListener);
		}
		initSpeclialStateBarColor();

	}

	@Override
	public void onPause() {
		super.onPause();
		ScrollView scrollView = getScrollView();
		if (scrollView != null) {
			scrollView.getViewTreeObserver().removeOnScrollChangedListener(mScrollChangedListener);
			mSavedScrollOffset = getScrollOffset();
		}
	}

	private void initSpeclialStateBarColor(){
		String SYSTEM_TYPE = SystemUtils.getmSystemUtil().getSystem();
		if(SYSTEM_TYPE.equals(SystemUtils.SYS_EMUI)){
			// 华为
		}else if(SYSTEM_TYPE.equals(SystemUtils.SYS_FLYME)){
			// 魅族
			changeFLYStateBarColor(statusBarIsBlack());
		}else if(SYSTEM_TYPE.equals(SystemUtils.SYS_MIUI)){
			// 小米
			changeMIUIStateBarColor(statusBarIsBlack());
		}
	}

	/**
	 * Override and return the name of the parent fragment to make "back" and
	 * "up" go straight there
	 */
	protected String getParentFragmentName() {
		return null;
	}

	protected int getScrollOffset() {
		ScrollView scrollView = getScrollView();
		return scrollView == null ? 0 : scrollView.getScrollY();
	}

	protected void refreshToolbar(final int scrollOffset) {
		if (mToolbar != null) {
			mToolbar.refreshToolbar(scrollOffset);
		} else {
			Log.d(TAG, "refreshToolbar: no toolbar available.");
		}
	}

	protected void setToolbarTextAndActionColor(final int color) {
		if (mToolbar != null) {
			mToolbar.setTextAndActionColor(color);
		} else {
			Log.d(TAG, "setToolbarTextAndActionColor: no toolbar available.");
		}
	}

	protected Drawable getToolbarBackDrawable() {
		// use default back drawable, sub classes can override if needed
		return null;
	}

	@Override
	public void startActivityForResult(final Intent intent, final int requestCode) {
		getActivity().startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (mBasePresenter != null) {
			mBasePresenter.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void setBasePresenter(final BasePresenter basePresenter) {
		mBasePresenter = basePresenter;
	}

	protected boolean accessEvenIfDisconnected() {
		return false;
	}

	protected boolean statusBarIsBlack() {
		return true;
	}


	protected boolean showEvenDisconnectDialog() {
		return true;
	}

	private void setupToolbar(final View view) {
		mToolbar = (AnimatedToolbar) view.findViewById(R.id.toolbar);

		final Activity activity = getActivity();
		/*
		 * activity.setSupportActionBar(mToolbar); final ActionBar
		 * supportActionBar = activity.getSupportActionBar(); if
		 * (supportActionBar != null) { Drawable toolbarBackDrawable =
		 * getToolbarBackDrawable(); if (toolbarBackDrawable != null) {
		 * mToolbar.setBackDrawable(toolbarBackDrawable); }
		 * supportActionBar.setHomeAsUpIndicator(mToolbar.getBackDrawable());
		 * supportActionBar.setDisplayShowTitleEnabled(false);
		 * supportActionBar.setDisplayHomeAsUpEnabled(RemoteConfigController.
		 * getInstance
		 * (KronabyApplication.getContext()).getAppToolBarBackButtonEnable());
		 * 
		 * } else
		 */{
			Log.d(TAG, "setupToolbar: no support toolbar available");
		}

		final TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			toolbarTitle.setText(getFeaturePathName());
		} else {
			Log.d(TAG, "setupToolbar: no toolbar title available");
		}
	}

	/**
	 * Gets the scroll view if the fragment uses a scroll view Override this in
	 * sub classes in order to get scroll offset to work
	 * 
	 * @return the fragments scroll view if any, null if no scroll view is used
	 */
	protected ScrollView getScrollView() {
		return null;
	}

	protected int getActionBarHeight() {
		Context context = getContext();
		if (context == null) {
			return 0;
		}
		TypedValue tv = new TypedValue();
		if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
			return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
		}
		return 0;
	}

	protected int getWatchYOffset(int watchParentHeight) {
		return Math.round(watchParentHeight
				* 0.5f
				- (watchParentHeight - getActionBarHeight() - getResources().getDimension(
						R.dimen.drop_target_top_margin)) * 0.5f);
	}




	// 魅族状态栏变色方案
	private void changeFLYStateBarColor(boolean dark){
		try {
			Window window = getActivity().getWindow();
			WindowManager.LayoutParams lp = window.getAttributes();
			Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
			Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
			darkFlag.setAccessible(true);
			meizuFlags.setAccessible(true);
			int bit = darkFlag.getInt(null);
			int value = meizuFlags.getInt(lp);
			if (dark)
				value |= bit;//设置状态栏深色图标和文字
			else
				value &= ~bit;//设置状态栏浅色图标和文字
			meizuFlags.setInt(lp, value);
			window.setAttributes(lp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void changeMIUIStateBarColor(boolean dark){
		Class<? extends Window> clazz = getActivity().getWindow().getClass();
		try {
			int darkModeFlag = 0;
			Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
			Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
			darkModeFlag = field.getInt(layoutParams);
			Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
			extraFlagField.invoke(getActivity().getWindow(), dark ? darkModeFlag : 0, darkModeFlag);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
