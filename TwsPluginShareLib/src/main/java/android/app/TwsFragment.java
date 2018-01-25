package android.app;

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
import com.rick.tws.widget.TwsToolbar;

import qrom.component.log.QRomLog;

public class TwsFragment extends Fragment {
    private static final String TAG = TwsFragment.class.getSimpleName();

    protected TwsToolbar mToolbar;
    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;

    /* Android restores scroll position after resume and don't report it back in view tree observer, hence
    * we need this to keep track of it by ourselves.*/
    private int mSavedScrollOffset;

    public TwsFragment() {
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
        return null == mToolbar ? null : mToolbar.getBackDrawable();
    }

    @Override
    public void startActivityForResult(final Intent intent, final int requestCode) {
        getActivity().startActivityForResult(intent, requestCode);
    }

    private void setupToolbar(final View view) {
        //当前这个接口只支持AppCompatActivity
        if (!(getActivity() instanceof AppCompatActivity)) {
            return;
        }

        mToolbar = (TwsToolbar) view.findViewById(R.id.toolbar);
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
            supportActionBar.setDisplayHomeAsUpEnabled(false);
        } else {
            QRomLog.i(TAG, "setupToolbar: no support toolbar available");
        }

        final TextView toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(getFeaturePathName());
        } else {
            Log.i(TAG, "setupToolbar: no toolbar title available");
        }
    }

    public void setNavigationOnClickListener(View.OnClickListener listener) {
        if (null != mToolbar) {
            mToolbar.setNavigationOnClickListener(listener);
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
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        return 0;
    }
}
