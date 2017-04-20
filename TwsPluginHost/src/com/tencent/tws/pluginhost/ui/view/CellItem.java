package com.tencent.tws.pluginhost.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.tws.pluginhost.R;

public class CellItem extends RelativeLayout {
	private ImageView mNotifyImageView = null;
	private ImageView mImageView = null;
	private TextView mTextView = null;

	private ComponentName mComponentName = new ComponentName();

	private int mComponentType = 0;

	private boolean mIsFocus = false;
	private Drawable mNormalBackground;
	private Drawable mFocusBackground;
	private int mTextColor_normal;
	private int mTextColor_focus;

	// 用于显示的索引位置
	private int mLocation = 0;

	private int mTagIndex = 0;

	public ActionBarInfo mActionBarInfo = new ActionBarInfo();

	public CellItem(Context context) {
		this(context, null);
	}

	public CellItem(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CellItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context);
	}

	public ComponentName getComponentName() {
		return mComponentName;
	}

	public int getComponentType() {
		return mComponentType;
	}

	private void init(Context context) {
		int id = 0x7b0a0000;
		// setGravity(Gravity.CENTER_HORIZONTAL);
		// weight 在设置LayoutParams的时候处理
		// craete ImageView
		mImageView = new ImageView(context);
		final float home_bottom_tab_img_width = getResources().getDimension(R.dimen.home_bottom_tab_img_width);
		final float home_bottom_tab_img_height = getResources().getDimension(R.dimen.home_bottom_tab_img_height);
		LayoutParams ivParams = new LayoutParams((int) home_bottom_tab_img_width, (int) home_bottom_tab_img_height);
//		int top = (int) getResources().getDimension(R.dimen.home_bottom_tab_img_margin_top);
//		ivParams.topMargin = top;
		// ivParams.setMargins(0, top, 0, 0);
		ivParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		mImageView.setLayoutParams(ivParams);
		mImageView.setId(id);
		++id;
		// tvParams.addRule(RelativeLayout.RIGHT_OF, mImageView.getId());
		addView(mImageView, 0);

		// create TextView
		mTextView = new TextView(context);
		LayoutParams tvParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tvParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		tvParams.addRule(RelativeLayout.BELOW, mImageView.getId());
		mTextView.setLayoutParams(tvParams);
		// mTextView.setTextColor(getResources().getColor(R.color.home_bottom_tab_text_pressed_color));
		addView(mTextView, 1);
		mTextView.setVisibility(View.GONE);
		++id;
	}

	public void setNormalBackground(Drawable drawable) {
		mNormalBackground = drawable;
		if (mNormalBackground == null) {
			mNormalBackground = getResources().getDrawable(R.drawable.ic_launcher);
		}

		if (!mIsFocus) {
			mImageView.setBackground(mNormalBackground);
		}
	}

	public void setFocusBackground(Drawable drawable) {
		mFocusBackground = drawable;
		if (mFocusBackground == null) {
			mFocusBackground = getResources().getDrawable(R.drawable.ic_launcher);
		}
		if (mIsFocus) {
			mImageView.setBackground(mFocusBackground);
		}
	}

	public void setTextColorNormal(int color) {
		mTextColor_normal = color;
		if (!mIsFocus) {
			mTextView.setTextColor(mTextColor_normal);
		}
	}

	public void setTextColorFocus(int color) {
		mTextColor_focus = color;
		if (mIsFocus) {
			mTextView.setTextColor(mTextColor_focus);
		}
	}

	public void setText(CharSequence text) {
		mTextView.setText(text);
	}

	public void setText(int resid) {
		mTextView.setText(resid);
	}

	public CharSequence getText() {
		if (mTextView != null) {
			return mTextView.getText();
		}

		return null;
	}

	public void setActionClass(String classId, String packageName, int componentType) {
		mComponentName.setValue(classId, packageName);
		mComponentType = componentType;
	}

	public void setFocus(boolean focus) {
		if (mIsFocus == focus)
			return;

		mIsFocus = focus;

		if (mIsFocus) {
			mTextView.setTextColor(mTextColor_focus);
			mImageView.setBackground(mFocusBackground);
		} else {
			mTextView.setTextColor(mTextColor_normal);
			mImageView.setBackground(mNormalBackground);
		}
	}

	public void setComponentName(ComponentName componentName) {
		mComponentName.setValue(componentName);
	}

	public void setLocation(int location) {
		mLocation = location;
	}

	public int getLocation() {
		return mLocation;
	}

	public void setTagIndex(int tagIndex) {
		mTagIndex = tagIndex;
	}

	public int getTagIndex() {
		return mTagIndex;
	}

	public class ActionBarInfo {
		public ActionBarInfo() {
			ab_title = getResources().getString(R.string.app_name);
		}

		// ActionBar
		// 这里就部分语言环境了，直接复制就行
		public String ab_title = null;
		// ActionBar右侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
		public int ab_rbtnctype = 2;// 默认是activity
		// ActionBar右侧按钮上触发点击后的行为内容
		public String ab_rbtncontent = null;

		// 显示在ActionBar右侧按钮上的内容类型 1、String文本资源 2、图标资源
		public int ab_rbtnrestype = 1;
		// 显示在ActionBar右侧按钮上的内容，根据类型进行配置
		public String ab_rbtnres_normal = null;
		public String ab_rbtnres_focus = null;

		@Override
		public String toString() {
			return "ab_title=" + ab_title + " ab_rbtnctype=" + ab_rbtnctype + " ab_rbtncontent=" + ab_rbtncontent
					+ " ab_rbtnrestype=" + ab_rbtnrestype + " ab_rbtnres_normal=" + ab_rbtnres_normal
					+ " ab_rbtnres_focus=" + ab_rbtnres_focus;
		}

	}

	public Object getPluginPackageName() {
		return mComponentName.getPluginPackageName();
	}

	public String getClassId() {
		return mComponentName.getClassId();
	}

	// 将fragment的ClassId和插件的包名捆绑在一起，这样有利于避开多插件的命名规范同时也可以优化查找速度
	public class ComponentName {
		private String mClassId;
		// 保存标识用于判断
		private String mPluginPackageName = "";

		public void setValue(ComponentName componentName) {
			this.mClassId = componentName.mClassId;
			this.mPluginPackageName = componentName.mPluginPackageName;
		}

		public void setValue(String classId, String packageName) {
			this.mClassId = classId;
			this.mPluginPackageName = packageName;
		}

		public String getClassId() {
			return mClassId;
		}

		public String getPluginPackageName() {
			return mPluginPackageName;
		}
	}

	private void setToNotify() {
		if (mNotifyImageView != null)
			return;

		// create ImageView
		mNotifyImageView = new ImageView(getContext());
		LayoutParams nivParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		nivParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		nivParams.topMargin = 15;
		nivParams.addRule(RelativeLayout.START_OF, mImageView.getId());
		// nivParams.addRule(RelativeLayout.ALIGN_TOP, mImageView.getId());
		mNotifyImageView.setLayoutParams(nivParams);
		mNotifyImageView.setBackground(getResources().getDrawable(R.drawable.red_point));
		int id = mImageView.getId() + 2;
		mNotifyImageView.setId(id);
		addView(mNotifyImageView, 2);
	}

	public void setHighlight(boolean needHighlight) {
		if (needHighlight) {
			setToNotify();
			mNotifyImageView.setVisibility(View.VISIBLE);
		} else if (mNotifyImageView != null) {
			mNotifyImageView.setVisibility(View.INVISIBLE);
		}
	}
}
