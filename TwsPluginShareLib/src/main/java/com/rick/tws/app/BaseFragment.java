package com.rick.tws.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import com.rick.tws.sharelib.R;
import com.rick.tws.widget.AnimatedToolbar;

/**
 * Created by Administrator on 2018/1/14 0014.
 */

public class BaseFragment extends Fragment {
    private static final String TAG = BaseFragment.class.getSimpleName();

    protected AnimatedToolbar mToolbar;
    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;

    /* Android restores scroll position after resume and don't report it back in view tree observer, hence
    * we need this to keep track of it by ourselves.*/
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

    protected void onScrolled() {
        refreshToolbar(getScrollOffset());
    }

    //public abstract String getName();

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

    /**
     * Override and return the name of the parent fragment to make "back" and "up" go straight there
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
            Log.i(TAG, "refreshToolbar: no toolbar available.");
        }
    }

    protected void setToolbarTextAndActionColor(final int color) {
        if (mToolbar != null) {
            mToolbar.setTextAndActionColor(color);
        } else {
            Log.i(TAG, "setToolbarTextAndActionColor: no toolbar available.");
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

//    public void setBasePresenter(final BasePresenter basePresenter) {
//        mBasePresenter = basePresenter;
//    }

    protected boolean accessEvenIfDisconnected() {
        return false;
    }

    private void setupToolbar(final View view) {
        mToolbar = (AnimatedToolbar) view.findViewById(R.id.toolbar);

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mToolbar);
        final ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            Drawable toolbarBackDrawable = getToolbarBackDrawable();
            if (toolbarBackDrawable != null) {
                mToolbar.setBackDrawable(toolbarBackDrawable);
            }
            supportActionBar.setHomeAsUpIndicator(mToolbar.getBackDrawable());
            supportActionBar.setDisplayShowTitleEnabled(false);
            //supportActionBar.setDisplayHomeAsUpEnabled(RemoteConfigController.getInstance(KronabyApplication.getContext()).getAppToolBarBackButtonEnable());
        } else {
            Log.i(TAG, "setupToolbar: no support toolbar available");
        }

        final TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(getFeaturePathName());
        } else {
            Log.i(TAG, "setupToolbar: no toolbar title available");
        }
    }

    /**
     * Gets the scroll view if the fragment uses a scroll view
     * Override this in sub classes in order to get scroll offset
     * to work
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
            return TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }
        return 0;
    }
}
