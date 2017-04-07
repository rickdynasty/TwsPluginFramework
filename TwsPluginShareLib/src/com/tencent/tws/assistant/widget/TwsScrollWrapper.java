package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.tencent.tws.assistant.gaussblur.GlassBlur;
import com.tencent.tws.assistant.gaussblur.GlassBlurTask;
import com.tencent.tws.assistant.gaussblur.GlassBlurUtil;
import com.tencent.tws.assistant.widget.TwsScrollView;
import com.tencent.tws.assistant.widget.TwsScrollView.OnScrollChangedListener;
import com.tencent.tws.sharelib.R;

public class TwsScrollWrapper implements OnGlobalLayoutListener, OnScrollChangedListener, GlassBlurTask.Listener {
	private int contentLayout;
	private FrameLayout frame;
	private ImageView headerView, footerView;
	private int headerHeight, footerHeight;
	private int width, height;
	private Bitmap headerBitmap, footerBitmap;
	private int blurRadius = GlassBlurUtil.DEFAULT_BLUR_RADIUS;
	private GlassBlurTask blurTask;
	private int lastScrollPosition = -1;
	private TwsScrollView scrollView;
	private int downSampling = GlassBlurUtil.DEFAULT_DOWNSAMPLING;
	private Drawable windowBackground;
	
	public TwsScrollWrapper contentLayout(Context context, int layout) {
		this.contentLayout = layout;
		frame = new FrameLayout(context);
        frame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		scrollView = (TwsScrollView)LayoutInflater.from(context).inflate(contentLayout, (ViewGroup) frame, false);
		headerView = new ImageView(context);
        footerView = new ImageView(context);
        headerHeight = (int) context.getResources().getDimension(R.dimen.tws_action_bar_height); 
		footerHeight = (int) context.getResources().getDimension(R.dimen.tws_actionbar_split_height);
        LayoutParams headerLP = new LayoutParams(LayoutParams.MATCH_PARENT, headerHeight);
        LayoutParams footerLP = new LayoutParams(LayoutParams.MATCH_PARENT, footerHeight);
        headerLP.gravity = Gravity.TOP;
        footerLP.gravity = Gravity.BOTTOM;
        headerView.setLayoutParams(headerLP);
        footerView.setLayoutParams(footerLP);
        scrollView.statusbarFlag = false;
		return this;
	}
	
	public TwsScrollWrapper contentLayoutWithStatusbar(Context context, int layout) {
		this.contentLayout = layout;
		frame = new FrameLayout(context);
        frame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		scrollView = (TwsScrollView)LayoutInflater.from(context).inflate(contentLayout, (ViewGroup) frame, false);
		headerView = new ImageView(context);
        footerView = new ImageView(context);
        headerHeight = (int) context.getResources().getDimension(R.dimen.tws_action_bar_height);
		footerHeight = (int) context.getResources().getDimension(R.dimen.tws_actionbar_split_height);
        LayoutParams headerLP = new LayoutParams(LayoutParams.MATCH_PARENT, headerHeight);
        LayoutParams footerLP = new LayoutParams(LayoutParams.MATCH_PARENT, footerHeight);
        headerLP.gravity = Gravity.TOP;
        footerLP.gravity = Gravity.BOTTOM;
        headerView.setLayoutParams(headerLP);
        footerView.setLayoutParams(footerLP);
        scrollView.statusbarFlag = true;
		return this;
	}
	
	public TwsScrollWrapper contentLayoutWithStatusbar(Context context, TwsScrollView view) {
		frame = new FrameLayout(context);
        frame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		scrollView = view;
		headerView = new ImageView(context);
        footerView = new ImageView(context);
        headerHeight = (int) context.getResources().getDimension(R.dimen.tws_action_bar_height);
		footerHeight = (int) context.getResources().getDimension(R.dimen.tws_actionbar_split_height);
        LayoutParams headerLP = new LayoutParams(LayoutParams.MATCH_PARENT, headerHeight);
        LayoutParams footerLP = new LayoutParams(LayoutParams.MATCH_PARENT, footerHeight);
        headerLP.gravity = Gravity.TOP;
        footerLP.gravity = Gravity.BOTTOM;
        headerView.setLayoutParams(headerLP);
        footerView.setLayoutParams(footerLP);
        scrollView.statusbarFlag = true;
		return this;
	}


	public View createView(Context context) {
		int[] attrs = { android.R.attr.windowBackground };

		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.windowBackground, outValue, true);

		TypedArray style = context.getTheme().obtainStyledAttributes(outValue.resourceId, attrs);
		windowBackground = style.getDrawable(0);
		style.recycle();
        frame.addView(scrollView, 0);
        frame.addView(headerView, 1);
        frame.addView(footerView, 2);

		frame.getViewTreeObserver().addOnGlobalLayoutListener(this);

		scrollView.setOnScrollChangedListener(this);
		return frame;
	}

	public void invalidate() {
		headerBitmap = null;
		footerBitmap = null;
		computeBlurOverlay();
		updateBlurOverlay(lastScrollPosition, true);
	}

	public void setBlurRadius(int newValue) {
		if (!GlassBlurUtil.isValidBlurRadius(newValue)) {
			throw new IllegalArgumentException("Invalid blur radius");
		}
		if (blurRadius == newValue) {
			return;
		}
		blurRadius = newValue;
		invalidate();
	}

	public int getBlurRadius() {
		return blurRadius;
	}

	public void setDownsampling(int newValue) {
		if (!GlassBlurUtil.isValidDownsampling(newValue)) {
			throw new IllegalArgumentException("Invalid downsampling");
		}
		if (downSampling == newValue) {
			return;
		}
		downSampling = newValue;
		invalidate();
	}

	public int getDownsampling() {
		return downSampling;
	}

	@Override
	public void onGlobalLayout() {
		if (width != 0) {
			return;
		}
		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(frame.getWidth(), MeasureSpec.AT_MOST);
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
		scrollView.measure(widthMeasureSpec, heightMeasureSpec);
		width = frame.getWidth();
		height = scrollView.getMeasuredHeight();

		lastScrollPosition = scrollView != null ? scrollView.getScrollY() : 0;
		invalidate();
	}

	private void computeBlurOverlay() {
		if (headerBitmap != null || footerBitmap != null) {
			return;
		}
		int scrollPosition = 0;
		if (scrollView != null) {
			scrollPosition = scrollView.getScrollY();
		}

		headerBitmap = drawViewToBitmap(headerBitmap, scrollView, width, height, downSampling, windowBackground);
		footerBitmap = drawViewToBitmap(footerBitmap, scrollView, width, height, downSampling, windowBackground);
		startBlurTask();

		if (scrollView != null) {
			scrollView.scrollTo(0, scrollPosition);
		}
	}

	private void startBlurTask() {
		if (blurTask != null) {
			blurTask.cancel();
		}
		blurTask = new GlassBlurTask(frame.getContext(), this, headerBitmap, blurRadius);
		blurTask = new GlassBlurTask(frame.getContext(), this, footerBitmap, blurRadius);
	}

	private void updateBlurOverlay(int top, boolean force) {
		if (headerBitmap == null || footerBitmap == null) {
			return;
		}
		if (top < 0) {
			top = 0;
		}
		if (!force && lastScrollPosition == top) {
			return;
		}
		lastScrollPosition = top;
		Bitmap headerSection, footerSection;
		headerSection = Bitmap.createBitmap(headerBitmap, 0, top / downSampling, width / downSampling, headerHeight / downSampling);
		footerSection = Bitmap.createBitmap(footerBitmap, 0, 
				(top + scrollView.getHeight() - footerHeight) / downSampling < (footerBitmap.getHeight() - footerHeight / downSampling) ? 
						(top + scrollView.getHeight() - footerHeight) / downSampling : (footerBitmap.getHeight() - footerHeight / downSampling), 
							width / downSampling, footerHeight / downSampling);
		Bitmap headerBlur, footerBlur;
		if (isBlurTaskFinished()) {
			headerBlur = headerSection;
			footerBlur = footerSection;
		} else {
			headerBlur = GlassBlur.apply(frame.getContext(), headerSection);
			footerBlur = GlassBlur.apply(frame.getContext(), footerSection);
		}
		Bitmap headerEnlarged = Bitmap.createScaledBitmap(headerBlur, width, headerHeight, false);
		Bitmap footerEnlarged = Bitmap.createScaledBitmap(footerBlur, width, footerHeight, false);
		headerBlur.recycle();
		headerSection.recycle();
		footerBlur.recycle();
		footerSection.recycle();
		headerView.setImageBitmap(headerEnlarged);
		footerView.setImageBitmap(footerEnlarged);
	}

	private boolean isBlurTaskFinished() {
		return blurTask == null;
	}

	@Override
	public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
		onNewScroll(t);
	}

	private void onNewScroll(int t) {
		updateBlurOverlay(t, false);
	}

	@Override
	public void onBlurOperationFinished() {
		blurTask = null;
		updateBlurOverlay(lastScrollPosition, true);
	}

	private Bitmap drawViewToBitmap(Bitmap dest, View view, int width, int height, int downSampling, Drawable drawable) {
        float scale = 1f / downSampling;
        int heightCopy = view.getHeight();
        view.layout(0, 0, width, height);
        int bmpWidth = (int)(width * scale);
        int bmpHeight = (int)(height * scale);
        if (dest == null || dest.getWidth() != bmpWidth || dest.getHeight() != bmpHeight) {
            dest = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
        }
        Canvas c = new Canvas(dest);
        drawable.setBounds(new Rect(0, 0, width, height));
        drawable.draw(c);
        if (downSampling > 1) {
            c.scale(scale, scale);
        }
        view.draw(c);
        view.layout(0, 0, width, heightCopy);
        return dest;
    }
	
	public void setHeaderBlank(boolean flag) {
		headerView.setVisibility(flag ? View.VISIBLE : View.GONE);
		scrollView.setHeaderHeight(flag ? headerHeight : 0);
	}
	
	public void setFooterBlank(boolean flag) {
		footerView.setVisibility(flag ? View.VISIBLE : View.GONE);
		scrollView.setFooterHeight(flag ? footerHeight : 0);
	}
}