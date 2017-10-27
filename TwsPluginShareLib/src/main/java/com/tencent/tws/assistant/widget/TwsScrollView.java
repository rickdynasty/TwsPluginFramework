package com.tencent.tws.assistant.widget;

import android.app.TwsActivity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

import com.tencent.tws.sharelib.R;


public class TwsScrollView extends ScrollView {
	
	private View contentView;
	
	private int headerHeight, footerHeight;
	
	private OnScrollChangedListener mOnScrollChangedListener;
	
	public boolean statusbarFlag = false; 

	private int mMaxHeight=0;
	
	public interface OnScrollChangedListener {
        void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt);
    }
	
	public TwsScrollView(Context context) {
		this(context , null);
	}

	public TwsScrollView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}

	public TwsScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.TwsLinearLayout);		
		mMaxHeight =a.getDimensionPixelSize(R.styleable.TwsLinearLayout_maxHeight,0);	
		a.recycle();
		
		if (android.os.Build.VERSION.SDK_INT > 18 && getResources().getBoolean(R.bool.config_statusbar_state)) {
			headerHeight =  !statusbarFlag ? 
					(int) context.getResources().getDimension(R.dimen.tws_action_bar_height) : 
						(int) context.getResources().getDimension(R.dimen.tws_action_bar_height) + TwsActivity.getStatusBarHeight();
		}
		else {
			headerHeight = (int) context.getResources().getDimension(R.dimen.tws_action_bar_height); 
		}
		footerHeight = (int) context.getResources().getDimension(R.dimen.tws_actionbar_split_height);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		if (android.os.Build.VERSION.SDK_INT > 18 && getResources().getBoolean(R.bool.config_statusbar_state)) {
			headerHeight =  !statusbarFlag ? 
					(int) mContext.getResources().getDimension(R.dimen.tws_action_bar_height) : 
						(int) mContext.getResources().getDimension(R.dimen.tws_action_bar_height) + TwsActivity.getStatusBarHeight();
		}
		else {
			headerHeight = (int) mContext.getResources().getDimension(R.dimen.tws_action_bar_height); 
		}
		footerHeight = (int) mContext.getResources().getDimension(R.dimen.tws_actionbar_split_height);
		
		contentView = getChildAt(0);
        contentView.setPadding(0, headerHeight, 0, footerHeight);
	}
	
	@Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }
	
	public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }
	
	public void setHeaderHeight(int height) {
		this.headerHeight = height;
		contentView = getChildAt(0);
		contentView.setPadding(0, height, 0, footerHeight);
	}
	
	public void setFooterHeight(int height) {
		this.footerHeight = height;
		contentView = getChildAt(0);
		contentView.setPadding(0, headerHeight, 0, height);
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(mMaxHeight > 0){
			final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
			int height = getMeasuredHeight();
			int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);
			if (height > mMaxHeight) {
				setMeasuredDimension(specWidthSize, mMaxHeight);
			} else {
				setMeasuredDimension(specWidthSize, height);
			}
		}
    }
}
