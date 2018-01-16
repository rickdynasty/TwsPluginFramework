package com.rick.tws.pluginhost.main.content;

import android.text.TextUtils;

import com.tws.plugin.content.DisplayItem;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2017/10/29 0029.
 */
public class HostDisplayItem extends DisplayItem {
    private static final String TAG = "HostDisplayItem";

    public static final int DISPLAY_AT_HOTSEAT = 0;
    public static final int DISPLAY_AT_HOME_FRAGEMENT = 1;
    public static final int DISPLAY_AT_MENU = 2;
    public static final int DISPLAY_AT_OTHER_POS = 3;

    public static final int UNKNOW_POS = -1;

    // 缓存插件的包名PackageName信息：pid
    public String pid = "";

    public CharSequence title_en = null;
    public CharSequence title_zh_CN = null;
    public CharSequence title_zh_TW = null;
    public CharSequence title_zh_HK = null;

    public String normalResName = null;
    public String focusResName = null;

    public boolean establishedDependOn = true;

    public ActionBarDisplayItem actionBarDisplayItem = null;

    public HostDisplayItem(DisplayItem displayItem, final String pid, final boolean establishedDependOn) {
        this.pid = pid;
        this.establishedDependOn = establishedDependOn;

        this.x = displayItem.x;
        this.y = displayItem.y;

        this.gemel_x = displayItem.gemel_x;
        this.gemel_y = displayItem.gemel_y;
        this.action_type = displayItem.action_type;
        this.title = displayItem.title;
        this.action_id = displayItem.action_id;
        this.icon = displayItem.icon;
        this.statistic_key = displayItem.statistic_key;
        this.extras = displayItem.extras;

        if (!TextUtils.isEmpty(this.title)) {
            final String[] titles = this.title.split(DisplayItem.SEPARATOR_VALUE);
            this.title_en = this.title_zh_CN = this.title_zh_TW = this.title_zh_HK = titles[0];
            if (1 < titles.length) {
                this.title_en = titles[1];
            }

            if (2 < titles.length) {
                this.title_zh_TW = this.title_zh_HK = titles[2];
            }

            if (3 < titles.length) {
                this.title_zh_TW = titles[3];
            }
        }

        if (!TextUtils.isEmpty(this.icon)) {
            final String[] resNames = this.icon.split(DisplayItem.SEPARATOR_VALUE);
            this.normalResName = resNames[0];
            if (1 < resNames.length) {
                this.focusResName = resNames[1];
            }
        }

        QRomLog.i(TAG, "apply Item - plugin-pid=" + pid + " x=" + x + " y=" + y + " action_type=" + action_type + " title=" + title + " action_id=" + action_id +
                " icon=" + icon + " statistic_key=" + statistic_key + " extras=" + extras + " gemel_x:" + gemel_x + " gemel_y=" + gemel_y);
    }

    //zh/En/zh-tw/zh-hk
    public void applyActionBarTitleItem(DisplayItem displayItem) {
        if (null == displayItem || displayItem.gemel_x <= INVALID_POS || displayItem.gemel_y <= INVALID_POS) {
            throw new IllegalAccessError("This func accepts only gemel item !");
        }

        if (!TextUtils.isEmpty(displayItem.title)) {
            sureActionBarDisplayItem();

            final String[] titles = displayItem.title.split(DisplayItem.SEPARATOR_VALUE);
            this.actionBarDisplayItem.title_en = this.actionBarDisplayItem.title_zh_CN =
                    this.actionBarDisplayItem.title_zh_TW = this.actionBarDisplayItem.title_zh_HK = titles[0];
            if (1 < titles.length) {
                this.actionBarDisplayItem.title_en = titles[1];
            }

            if (2 < titles.length) {
                this.actionBarDisplayItem.title_zh_TW = this.actionBarDisplayItem.title_zh_HK = titles[2];
            }

            if (3 < titles.length) {
                this.actionBarDisplayItem.title_zh_TW = titles[3];
            }
        }
    }

    //zh/En/zh-tw/zh-hk
    public void applyActionBarSubtitleItem(DisplayItem displayItem) {
        if (null == displayItem || displayItem.gemel_x <= INVALID_POS || displayItem.gemel_y <= INVALID_POS) {
            throw new IllegalAccessError("This func accepts only gemel item !");
        }

        if (!TextUtils.isEmpty(displayItem.title)) {
            sureActionBarDisplayItem();

            final String[] titles = displayItem.title.split(DisplayItem.SEPARATOR_VALUE);
            this.actionBarDisplayItem.subtitle_en = this.actionBarDisplayItem.subtitle_zh_CN = this.actionBarDisplayItem.subtitle_zh_TW = this.actionBarDisplayItem.subtitle_zh_HK = titles[0];
            if (1 < titles.length) {
                this.actionBarDisplayItem.subtitle_en = titles[1];
            }

            if (2 < titles.length) {
                this.actionBarDisplayItem.subtitle_zh_TW = this.actionBarDisplayItem.subtitle_zh_HK = titles[2];
            }

            if (3 < titles.length) {
                this.actionBarDisplayItem.subtitle_zh_TW = titles[3];
            }
        }
    }

    public void applyActionBarLeftButtonItem(DisplayItem displayItem, final int resType) {
        if (null == displayItem || displayItem.gemel_x <= INVALID_POS || displayItem.gemel_y <= INVALID_POS) {
            throw new IllegalAccessError("This func accepts only gemel item !");
        }
    }

    public void applyActionBarRighButtonItem(DisplayItem displayItem, final int resType) {
        if (null == displayItem || displayItem.gemel_x <= INVALID_POS || displayItem.gemel_y <= INVALID_POS) {
            throw new IllegalAccessError("This func accepts only gemel item !");
        }

        switch (resType) {
            case RES_TYPE_STRING:
                sureActionBarDisplayItem();
                if (!TextUtils.isEmpty(displayItem.title)) {
                    sureActionBarDisplayItem();

                    this.actionBarDisplayItem.r_action_type = displayItem.action_type;
                    this.actionBarDisplayItem.r_action_id = displayItem.action_id;

                    this.actionBarDisplayItem.r_res_type = RES_TYPE_STRING;

                    final String[] titles = displayItem.title.split(DisplayItem.SEPARATOR_VALUE);
                    this.actionBarDisplayItem.r_btn_text_en = this.actionBarDisplayItem.r_btn_text_zh_CN = this.actionBarDisplayItem.r_btn_text_zh_TW = this.actionBarDisplayItem.r_btn_text_zh_HK = titles[0];
                    if (1 < titles.length) {
                        this.actionBarDisplayItem.r_btn_text_en = titles[1];
                    }

                    if (2 < titles.length) {
                        this.actionBarDisplayItem.r_btn_text_zh_TW = this.actionBarDisplayItem.r_btn_text_zh_HK = titles[2];
                    }

                    if (3 < titles.length) {
                        this.actionBarDisplayItem.r_btn_text_zh_TW = titles[3];
                    }
                }
                break;
            case RES_TYPE_DRAWABLE:
                sureActionBarDisplayItem();

                this.actionBarDisplayItem.r_action_type = displayItem.action_type;
                this.actionBarDisplayItem.r_action_id = displayItem.action_id;

                this.actionBarDisplayItem.r_res_type = RES_TYPE_DRAWABLE;

                if (!TextUtils.isEmpty(displayItem.icon)) {
                    final String[] resNames = displayItem.icon.split(DisplayItem.SEPARATOR_VALUE);
                    this.actionBarDisplayItem.r_res_normal = resNames[0];
                    if (1 < resNames.length) {
                        this.actionBarDisplayItem.r_res_focus = resNames[1];
                    }
                }
                break;
            default:
                throw new IllegalAccessError("ActionBarRighButton only apply string(" + RES_TYPE_STRING + ") or drawable("
                        + RES_TYPE_DRAWABLE + "), Exception for restype:" + resType);
        }
    }

    public void applyActionBarMenuItem(DisplayItem displayItem) {
        if (null == displayItem || displayItem.gemel_x <= INVALID_POS || displayItem.gemel_y <= INVALID_POS) {
            throw new IllegalAccessError("This func accepts only gemel item !");
        }
    }

    private void sureActionBarDisplayItem() {
        if (null == this.actionBarDisplayItem) {
            this.actionBarDisplayItem = new ActionBarDisplayItem();
        }
    }
}
