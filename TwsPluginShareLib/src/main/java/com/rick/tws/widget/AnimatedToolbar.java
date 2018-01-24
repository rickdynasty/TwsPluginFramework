package com.rick.tws.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.TintTypedArray;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.rick.tws.framework.HostProxy;
import com.rick.tws.sharelib.R;

public class AnimatedToolbar extends Toolbar {

    private static final String TAG = AnimatedToolbar.class.getSimpleName();

    // This means that RecyclerViews in the app that doesn't have setHasFixedSize(true)
    // has to have a minimum height on the top View of 40dp
    public static final int ANIMATION_FINISHED_OFFSET_DP = 40;
    private static int mAnimationFinishedOffset;

    private final Context mContext;
    private Drawable mToolbarDrawable;
    private Drawable mBackDrawable;

    //主要是覆盖Toolbar里面的mTitleTextView，处理位置偏差的问题
    private TextView mTitleTextView;

    private TextView mToolbarAction;

    public AnimatedToolbar(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public AnimatedToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public AnimatedToolbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    private void init() {
        mToolbarDrawable = getBackground().mutate();
        mAnimationFinishedOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, ANIMATION_FINISHED_OFFSET_DP, getResources().getDisplayMetrics());
        mBackDrawable = ContextCompat.getDrawable(mContext, R.drawable.toolbar_back_drawable);
        setPadding(0, HostProxy.getStatusBarHeight(), 0, 0);
    }

    public void refreshToolbar(int scrollOffset) {
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        updateBarTransparency(Math.min(scrollOffset * 255 / mAnimationFinishedOffset, 255));
    }

    @Override
    public void setTitle(int resId) {
        if (mTitleTextView == null) {
            mTitleTextView = (TextView) findViewById(R.id.toolbar_title);
            mTitleTextView.setSingleLine();
            mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        }

        if (null != mTitleTextView) {
            mTitleTextView.setText(resId);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (mTitleTextView == null) {
            mTitleTextView = (TextView) findViewById(R.id.toolbar_title);
            mTitleTextView.setSingleLine();
            mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        }
        if (null != mTitleTextView) {
            mTitleTextView.setText(title);
        }
    }

    public TextView enableActionText(boolean enable) {
        if (enable) {
            mToolbarAction = (TextView) findViewById(R.id.toolbar_action);
            mToolbarAction.setVisibility(VISIBLE);
        } else if (mToolbarAction != null) {
            mToolbarAction.setVisibility(GONE);
            mToolbarAction = null;
        }
        return mToolbarAction;
    }

    public static int getAnimationFinishedOffset() {
        return mAnimationFinishedOffset;
    }

    public void setTextAndActionColor(int color) {
        if (mTitleTextView == null) {
            mTitleTextView = (TextView) findViewById(R.id.toolbar_title);
        }

        if (mToolbarAction == null) {
            mToolbarAction = (TextView) findViewById(R.id.toolbar_action);
        }

        mTitleTextView.setTextColor(ContextCompat.getColor(mContext, color));
        mBackDrawable.setColorFilter(ContextCompat.getColor(mContext, color), PorterDuff.Mode.SRC_ATOP);
        mToolbarAction.setTextColor(ContextCompat.getColor(mContext, color));
    }

    private void updateBarTransparency(int transparency) {
        if (mTitleTextView == null) {
            mTitleTextView = (TextView) findViewById(R.id.toolbar_title);
        }
        mToolbarDrawable.setAlpha(transparency);

        final int color = transparency == 0 ? R.color.tws_white : R.color.tws_black;
        mBackDrawable.setColorFilter(ContextCompat.getColor(mContext, color), PorterDuff.Mode.SRC_ATOP);
        mTitleTextView.setTextColor(ContextCompat.getColor(mContext, color));
        if (mToolbarAction != null) {
            mToolbarAction.setTextColor(ContextCompat.getColor(mContext, color));
        }
    }

    public Drawable getBackDrawable() {
        return mBackDrawable;
    }

    public void setBackDrawable(Drawable drawable) {
        mBackDrawable = drawable;
    }
}
