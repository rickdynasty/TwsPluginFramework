package com.tws.plugin.content;

/**
 * Created by Administrator on 2017/10/29 0029.
 */

public class ActionBarDisplayItem {
    // ActionBar title,这里之所以要存储各种语言，是因为插件这个时候可能还没加载起来
    public String title_zh_CN = null;
    public String title_zh_TW = null;
    public String title_zh_HK = null;
    public String title_en = null;

    public String subtitle_zh_CN = null;
    public String subtitle_zh_TW = null;
    public String subtitle_zh_HK = null;
    public String subtitle_en = null;

    public String r_btn_text_zh_CN = null;
    public String r_btn_text_zh_TW = null;
    public String r_btn_text_zh_HK = null;
    public String r_btn_text_en = null;

    // ActionBar右侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
    public int r_action_type = DisplayItem.TYPE_ACTIVITY;// 默认是activity
    // ActionBar右侧按钮上触发点击后的行为内容
    public String r_action_id = null;

    // 显示在ActionBar右侧按钮上的内容类型 1、String文本资源 2、图标资源
    public int r_res_type = DisplayItem.RES_TYPE_STRING;
    // 显示在ActionBar右侧按钮上的内容，根据类型进行配置
    public String r_res_normal = null;
    public String r_res_focus = null;
}
