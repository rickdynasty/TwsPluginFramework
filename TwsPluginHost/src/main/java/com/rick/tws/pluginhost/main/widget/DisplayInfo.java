package com.rick.tws.pluginhost.main.widget;

import android.content.Context;
import android.text.TextUtils;

import com.tws.plugin.content.DisplayItem;

import qrom.component.log.QRomLog;

//二次解析DisplayItem
public class DisplayInfo {
    private static final String TAG = "DisplayInfo";

    public static final int DISPLAY_AT_HOTSEAT = 1;
    public static final int DISPLAY_AT_HOME_FRAGEMENT = 2;
    public static final int DISPLAY_AT_MENU = 3;
    public static final int DISPLAY_AT_OTHER_POS = 4;

    public static final int UNKNOW_POS = -1;

    public String action_id = null;
    public int componentType;

    //显示的标题【分语种】
    public CharSequence title_en = null;
    public CharSequence title_zh_CN = null;
    public CharSequence title_zh_TW = null;
    public CharSequence title_zh_HK = null;

    public String normalResName = null;
    public String focusResName = null;
    public String statKey = "";
    public String packageName = "";
    public int location = 0; // 一级索引位置

    public String giconres_normal;
    public String giconres_focus;

    public boolean establishedDependOn = true;

    // ActionBar
    public String ab_title_zh_CN = null;
    public String ab_title_zh_TW = null;
    public String ab_title_zh_HK = null;
    public String ab_title_en = null;
    public String ab_title = null;
    public int ab_titlerestype;

    // ActionBar右侧按钮上触发点击后的行为内容
    public String ab_rbtncontent = null;
    // ActionBar右侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
    public int ab_rbtnctype = 2;// 默认是activity
    // 显示在ActionBar右侧按钮上的内容类型 1、String文本资源 2、图标资源
    public int ab_rbtnrestype = 1;
    // 显示在ActionBar右侧按钮上的内容，根据类型进行配置
    public String ab_rbtnres_normal = null;
    public String ab_rbtnres_focus = null;

    // ActionBar左侧按钮
    public String ab_lbtncontent = null;
    public int ab_lbtnctype = 2;
    public int ab_lbtnrestype = 1;
    public String ab_lbtnres_normal = null;
    public String ab_lbtnres_focus = null;

    public DisplayInfo(Context context, final DisplayItem di, final String packageName, final boolean establishedDependOn) {
        this.action_id = di.action_id;
        this.componentType = di.action_type;
        // 注意协议对二级pos没配置的默认赋值是-1，这种需要随波逐流，也就是谁先安装谁放前面，谁叫没给配置咧
        // final int pos = getPosByPackageName(dc.pos, packageName);
        if (di.x < 0) {
            di.x = 0;
        }
        this.location = di.x;
        this.establishedDependOn = establishedDependOn;

        QRomLog.d(TAG, "pgName:" + packageName + " title=" + di.title + " pos[" + di.x + ", " + di.y + "] location is " + this.location);

        if (!TextUtils.isEmpty(di.title)) {
            final String[] titles = di.title.split(DisplayItem.SEPARATOR_VALUE);
            this.title_en = this.title_zh_CN = this.title_zh_TW = this.title_zh_HK = titles[0];
            if (1 < titles.length) {
                this.title_zh_CN = titles[1];
            }

            if (2 < titles.length) {
                this.title_zh_TW = this.title_zh_HK = titles[2];
            }

            if (3 < titles.length) {
                this.title_zh_TW = titles[3];
            }
        }

        this.statKey = di.statistic_key;
        if (!TextUtils.isEmpty(di.icon)) {
            final String[] resNames = di.icon.split(DisplayItem.SEPARATOR_VALUE);
            this.normalResName = resNames[0];
            if (1 < resNames.length) {
                this.focusResName = resNames[1];
            }
        }
        this.packageName = packageName;

        QRomLog.d(TAG, "DisplayInfo[在DM上的显示信息：] action_id=" + this.action_id + " location=" + location + " componentType="
                + this.componentType + " title_zh=" + this.title_zh_CN + "/" + this.ab_title_zh_HK + "/"
                + this.ab_title_zh_TW + " normalResName=" + this.normalResName + " focusResName=" + this.focusResName
                + " packageName=" + this.packageName);
    }
}
