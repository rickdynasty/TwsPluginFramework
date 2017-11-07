package com.rick.tws.pluginhost.debug.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

public class StatusButton extends AppCompatButton {
    public static final int UNINSTALL_PLUGIN = 0;
    public static final int INSTALLED_PLUGIN = 1;
    private static final String STR_INSATALL = "点击安装 插件:";
    private static final String STR_UNINSTALL = "点击卸载  插件:";
    private static final String STR_UPDATE = "点击更新  插件:";
    private int mStatus = UNINSTALL_PLUGIN;
    private boolean onlyForUpdata = false;
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
        } else if (onlyForUpdata) {
            setText(STR_UPDATE + mPluginLabel);
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

    public boolean isOnlyForUpdata() {
        return onlyForUpdata;
    }

    public void setOnlyForUpdata(boolean onlyForUpdata) {
        this.onlyForUpdata = onlyForUpdata;
    }
}
