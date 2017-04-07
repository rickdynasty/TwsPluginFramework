package com.tencent.tws.pluginhost.plugindebug.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class StatusButton extends Button {
	public static final int UNINSTALL_PLUGIN = 0;
	public static final int INSTALLED_PLUGIN = 1;
	private static final String STR_INSATALL = "点击安装 插件:";
	private static final String STR_UNINSTALL = "点击卸载  插件:";
	private int mStatus = UNINSTALL_PLUGIN;
	private CharSequence mPluginLabel;

	public StatusButton(Context context) {
		super(context);
		init();
	}

	public StatusButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StatusButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public void setPluginLabel(CharSequence text) {
		mPluginLabel = text;
		setShowText();
	}

	public CharSequence getPluginLabel() {
		return mPluginLabel;
	}

	private void init() {
		mStatus = UNINSTALL_PLUGIN;
	}

	public void setStatus(int status) {
		mStatus = status;
		setShowText();
	}

	private void setShowText() {
		if (mStatus == UNINSTALL_PLUGIN) {
			setText(STR_INSATALL + mPluginLabel);
		} else {
			setText(STR_UNINSTALL + mPluginLabel);
		}
	}

	public int getStatus() {
		return mStatus;
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		super.setOnClickListener(l);
	}
}
