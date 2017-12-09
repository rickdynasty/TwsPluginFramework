package com.rick.tws.pluginhost.main.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rick.tws.pluginhost.R;

/**
 * 一般情况是 这个ContentItem的内容是 图标+标题+右指向的箭头，但是notification的例外
 */
public class HomeFragmentContentItem extends RelativeLayout {
	public static final int ITEM_MESSAGE = 1;
	public static final int ITEM_SETTINGS = 2;

	private ImageView mImageView = null;
	private TextView mTextView = null;
	private final int mImage_width;
	private final int mImage_height;
	private final int mText_marginLeft;
	private final int mTextSize;
	private final int mTextColor;

	private ImageView mArrowImage;
	private final int mArrowImage_width;
	private final int mArrowImage_height;
	private final int mArrowImage_marginRight;

	private String mClassId;
	private int mComponentType = 0;

	private boolean mNeedSplitLine = false;
	// 用于统计
	public String mStatKey = "";

	// 特殊标识用于特定的操作
	public int mSpecialFlg = 0;
	// 保存标识用于判断
	private String mPluginPackageName = "";
	// 用于显示的索引位置
	private int mLocation = 0;

	public HomeFragmentContentItem(Context context) {
		this(context, false);
	}

	public HomeFragmentContentItem(Context context, boolean needSplit) {
		this(context, null, needSplit);
	}

	public HomeFragmentContentItem(Context context, AttributeSet attrs) {
		this(context, attrs, false);
	}

	public HomeFragmentContentItem(Context context, AttributeSet attrs, boolean needSplit) {
		this(context, attrs, 0, needSplit);
	}

	public HomeFragmentContentItem(Context context, AttributeSet attrs, int defStyle, boolean needSplit) {
		super(context, attrs, defStyle);
		mNeedSplitLine = needSplit;
		mImage_width = mImage_height = (int) getResources().getDimension(
				R.dimen.HOST_HOME_FRAGMENT_revision_item_img_size);

		mText_marginLeft = (int) getResources().getDimension(R.dimen.HOST_HOME_FRAGMENT_revision_item_text_margin_left);
		mTextSize = 16;// getResources().getDimensionPixelSize(R.dimen.HOST_HOME_FRAGMENT_revision_item_text_size);
		mTextColor = getResources().getColor(R.color.tws_black);
		mArrowImage_width = (int) getResources().getDimension(R.dimen.HOST_HOME_FRAGMENT_revision_item_arrow_img_width);
		mArrowImage_height = (int) getResources().getDimension(
				R.dimen.HOST_HOME_FRAGMENT_revision_item_arrow_img_height);

		mArrowImage_marginRight = (int) getResources().getDimension(
				R.dimen.HOST_HOME_FRAGMENT_revision_item_arrow_img_margin_right);

		init(context);
	}

	public void setActionClass(String classId, int componentType) {
		mClassId = classId;
		mComponentType = componentType;
	}

	public String getClassId() {
		return mClassId;
	}

	public int getComponentType() {
		return mComponentType;
	}

	private void init(Context context) {
		// create ImageView
		int id = 0x7b0a0000;
		mImageView = new ImageView(context);
		LayoutParams ivParams = new LayoutParams(mImage_width, mImage_height);
		ivParams.addRule(RelativeLayout.CENTER_VERTICAL);
		mImageView.setLayoutParams(ivParams);
		mImageView.setId(id);
		++id;
		addView(mImageView, 0);

		// create TextView
		mTextView = new TextView(context);
		LayoutParams tvParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tvParams.setMargins(mText_marginLeft, 0, 0, 0);
		tvParams.addRule(RelativeLayout.CENTER_VERTICAL);
		tvParams.addRule(RelativeLayout.RIGHT_OF, mImageView.getId());
		mTextView.setLayoutParams(tvParams);
		mTextView.setTextSize(mTextSize);
		mTextView.setId(id);
		++id;
		mTextView.setTextColor(mTextColor);
		addView(mTextView, 1);

		// create ImageView
		mArrowImage = new ImageView(context);
		LayoutParams aiParams = new LayoutParams(mArrowImage_width, mArrowImage_height);
		aiParams.addRule(RelativeLayout.CENTER_VERTICAL);
		aiParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		aiParams.setMargins(0, 0, mArrowImage_marginRight, 0);
		mArrowImage.setLayoutParams(aiParams);
		mArrowImage.setId(id);
		// mArrowImage.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
		setArrowImageDrawable(R.mipmap.arrow);

		addView(mArrowImage, 2);

		// create splitLine
		if (mNeedSplitLine) {
			TextView tv = new TextView(getContext());
			tv.setBackground(getResources().getDrawable(R.color.HOST_HOME_FRAGMENT_revision_divider));
			LayoutParams lp_split = new LayoutParams(LayoutParams.MATCH_PARENT, (int) getResources().getDimension(
					R.dimen.HOST_HOME_FRAGMENT_revision_item_divider_line_height));
			lp_split.leftMargin = (int) getResources().getDimension(
					R.dimen.HOST_HOME_FRAGMENT_revision_item_divider_line_margin_left);
			lp_split.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			addView(tv, lp_split);
		}
	}

	public void setText(CharSequence text) {
		mTextView.setText(text);
	}

	public void setText(int resid) {
		mTextView.setText(resid);
	}

	public CharSequence getTextViewText() {
		return mTextView.getText();
	}

	public void setImageViewImageDrawable(int resid) {
		mImageView.setImageDrawable(getResources().getDrawable(resid));
	}

	public void setImageViewImageDrawable(Drawable drawable) {
		if (drawable == null)
			mImageView.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
		else
			mImageView.setImageDrawable(drawable);
	}

	public void setImageViewBackground(Drawable background) {
		if (background == null)
			mImageView.setBackground(getResources().getDrawable(R.mipmap.ic_launcher));
		else
			mImageView.setBackground(background);
	}

	public void setImageViewBackgroundResource(int resid) {
		mImageView.setBackgroundResource(resid);
	}

	public void setArrowImageDrawable(int resid) {
		mArrowImage.setImageDrawable(getResources().getDrawable(resid));
	}

	public void setArrowImageDrawable(Drawable drawable) {
		mArrowImage.setImageDrawable(drawable);
	}

	public void setArrowImageBackground(Drawable background) {
		mArrowImage.setBackground(background);
	}

	public void setArrowImageBackgroundResource(int resid) {
		mArrowImage.setBackgroundResource(resid);
	}

	private boolean mIsNotify = false;
	private ImageView mNotifyImageView;
	private TextView mNotifyTextView;

	public boolean isNotify() {
		return mIsNotify;
	}

	// 设置是否具备通行为
	public void setToNotify() {
		if (true == mIsNotify)
			return;

		mIsNotify = true;
		// create ImageView
		mNotifyImageView = new ImageView(getContext());
		LayoutParams nivParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		nivParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		final int layout_marginTop = (int) getResources().getDimension(
				R.dimen.notification_HOST_HOME_FRAGMENT_red_point_margin_top);
		nivParams.topMargin = layout_marginTop;
		int id = mArrowImage.getId();
		nivParams.addRule(RelativeLayout.START_OF, mArrowImage.getId());
		mNotifyImageView.setLayoutParams(nivParams);
		mNotifyImageView.setBackground(getResources().getDrawable(R.mipmap.red_point));
		++id;
		mNotifyImageView.setId(id);
		addView(mNotifyImageView);

		// create TextView
		mNotifyTextView = new TextView(getContext());
		LayoutParams tvParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tvParams.setMargins(mText_marginLeft, 0, 0, 0);
		tvParams.addRule(RelativeLayout.CENTER_VERTICAL);
		tvParams.addRule(RelativeLayout.START_OF, mNotifyImageView.getId());
		mNotifyTextView.setLayoutParams(tvParams);
		mNotifyTextView.setText(getResources().getString(R.string.HOST_HOME_FRAGMENT_notification_menu_item));
		mNotifyTextView.setTextSize(14.0f);
		mNotifyTextView.setTextColor(mTextColor);
		addView(mNotifyTextView);
	}

	public void setNotifyText(int resid) {
		mNotifyTextView.setText(resid);
	}

	public void setNotifyText(CharSequence text) {
		mNotifyTextView.setText(text);
	}

	public ImageView getNotifyImageView() {
		return mNotifyImageView;
	}

	public TextView getNotifyTextView() {
		return mNotifyTextView;
	}

	public void setPluginPackageName(String packageName) {
		mPluginPackageName = packageName;
	}

	public String getPluginPackageName() {
		return mPluginPackageName;
	}

	public void setLocation(int location) {
		mLocation = location;
	}

	public int getLocation() {
		return mLocation;
	}
}
