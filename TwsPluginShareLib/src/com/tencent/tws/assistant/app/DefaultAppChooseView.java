package com.tencent.tws.assistant.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;

import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.widget.RadioButton;
import com.tencent.tws.sharelib.R;

public class DefaultAppChooseView extends RelativeLayout implements Checkable {

	private ImageView mImageView;
	private TextView mTextView;
	private LayoutInflater mLayoutInflater;
	private boolean mChecked;
	private RadioButton mIsDefaultIRadioButton;

	public DefaultAppChooseView(Context context, int layoutid) {
		this(context, null, 0, layoutid);

	}

	public DefaultAppChooseView(Context context, AttributeSet attrs, int layoutid) {
		this(context, attrs, 0, layoutid);

	}

	public DefaultAppChooseView(Context context, AttributeSet attrs, int defStyle, int layoutid) {
		super(context, attrs, defStyle);
		mLayoutInflater = LayoutInflater.from(context);
		mLayoutInflater.inflate(layoutid, this, true);
		mImageView = (ImageView) findViewById(R.id.app_icon);
		mTextView = (TextView) findViewById(R.id.app_name);
		mIsDefaultIRadioButton = (RadioButton) findViewById(R.id.is_default);

	}

	public void setImageDrawable(Drawable d) {
		mImageView.setImageDrawable(d);
	}

	public void setText(String string) {
		mTextView.setText(string);
	}

	@Override
	public boolean isChecked() {
		// TODO Auto-generated method stub
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		
		if (mChecked != checked) {
			mChecked = checked;
			mIsDefaultIRadioButton.setChecked(mChecked);

		}
	}

	@Override
	public void toggle() {
		// TODO Auto-generated method stub
		setChecked(!mChecked);
	}

}
