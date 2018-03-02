package com.example.pluginbluetooth.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.example.pluginbluetooth.R;
import com.example.pluginbluetooth.app.animation.WatchHandsAnimation;


public class WatchWithOneComplicationLayout extends WatchLayout {
	private static final String TAG = "WatchLayout::WatchWithOneComplicationLayout";

	private WatchHandsAnimation.WatchHandModel mComplicationWatchHandModel;
	private WatchHandsAnimation mComplicationWatchHandsAnimation;

	protected ImageView mImageViewWatchHandHoursComplication;
	protected ImageView mImageViewWatchHandMinutesComplication;

	public WatchWithOneComplicationLayout(Context context) {
		this(context, null, 0);
	}

	public WatchWithOneComplicationLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WatchWithOneComplicationLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mImageViewWatchHandHoursComplication = (ImageView) findViewById(R.id.imageViewWatchHandHoursComplication);
		mImageViewWatchHandMinutesComplication = (ImageView) findViewById(R.id.imageViewWatchHandMinutesComplication);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		int horizontalPadding = getPaddingRight() + getPaddingLeft();
		int contentWidth = right - left - horizontalPadding;

		int imageViewHandWidth = mImageViewWatchHandMinutesComplication.getMeasuredWidth();
		int imageViewHandHeight = mImageViewWatchHandMinutesComplication.getMeasuredHeight();

		int lwhm = getPaddingLeft() + (contentWidth - imageViewHandWidth) / 2;
		int twhm = getPaddingTop() /*+ mPointOffsetY*/;
		int rwhm = lwhm + imageViewHandWidth;
		int bwhm = twhm + imageViewHandHeight;

		mImageViewWatchHandMinutesComplication.layout(lwhm, twhm, rwhm, bwhm);

		mImageViewWatchHandMinutesComplication.setPivotX(imageViewHandWidth / 2f);
		mImageViewWatchHandMinutesComplication.setPivotY(imageViewHandHeight - imageViewHandWidth / 2f);

		int imageViewHandHoursWidth = mImageViewWatchHandHoursComplication.getMeasuredWidth();
		int imageViewHandHoursHeight = mImageViewWatchHandHoursComplication.getMeasuredHeight();

		int lwhh = getPaddingLeft() + (contentWidth - imageViewHandHoursWidth) / 2;
		int twhh = getPaddingTop()/* + mPointOffsetY*/;
		int rwhh = lwhh + imageViewHandHoursWidth;
		int bwhh = twhh + imageViewHandHoursHeight;

		mImageViewWatchHandHoursComplication.layout(lwhh, twhh, rwhh, bwhh);

		mImageViewWatchHandHoursComplication.setPivotX(imageViewHandHoursWidth / 2f);
		mImageViewWatchHandHoursComplication.setPivotY(imageViewHandHoursHeight - imageViewHandHoursWidth / 2f);

	}

	public void setComplicationWatchHandModel(WatchHandsAnimation.WatchHandModel complicationWatchHandModel) {
		mComplicationWatchHandModel = complicationWatchHandModel;
		if (mComplicationWatchHandsAnimation != null) {
			mComplicationWatchHandsAnimation.setWatchHandModel(mComplicationWatchHandModel);
		}
	}

	@Override
	public void updateHands(final boolean animate) {
		if (mComplicationWatchHandModel != null) {
			if (mComplicationWatchHandsAnimation == null) {
				mComplicationWatchHandsAnimation = new WatchHandsAnimation(mImageViewWatchHandMinutesComplication,
						mImageViewWatchHandHoursComplication, mComplicationWatchHandModel);
			}
			mComplicationWatchHandsAnimation.update(animate);
		}
		super.updateHands(animate);
	}
}