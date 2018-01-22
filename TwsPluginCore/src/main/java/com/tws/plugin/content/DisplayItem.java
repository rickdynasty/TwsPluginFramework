package com.tws.plugin.content;

import java.io.Serializable;

import qrom.component.log.QRomLog;

/**
 * Created by Administrator on 2017/10/27 0027.
 * 只负责保存一项的显示内容
 * 分两块 1、显示位置(x，y)；2、显示内容(类型、action、标题、图标、统计key、附加信息)
 */
public class DisplayItem implements Serializable {
    private static final String TAG = "DisplayItem";

    //Diplay rule
    public static final String SEPARATOR_CONFIG = "#";       //Item条目分隔符
    public static final String SEPARATOR_ATTRIBUTE = "@";    //Item的属性分隔符
    public static final String SEPARATOR_VALUATION = "=";    //属性的赋值分隔符
    public static final String SEPARATOR_VALUE = "/";        //属性存在多个值之间的分隔符
    public static final String SEPARATOR_VER = "_";          //版本信息内部的分隔符

    public static final int INVALID_POS = -1;
    /**
     * Diplay Type
     * 对齐ActivityManager的定义
     * public static final int INTENT_SENDER_BROADCAST = 1;
     * public static final int INTENT_SENDER_ACTIVITY = 2;
     * public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;
     * public static final int INTENT_SENDER_SERVICE = 4;
     *
     * 用于对组件的类型标识
     */
    public static final int TYPE_UNKOWN = -1;
    public static final int TYPE_APPLICATION = 0;
    public static final int TYPE_BROADCAST = 1;
    public static final int TYPE_ACTIVITY = 2;
    public static final int TYPE_FRAGMENT = 3;
    public static final int TYPE_SERVICE = 4;
    public static final int TYPE_PROVIDER = 5;
    public static final int TYPE_VIEW = 6;

    public static final int RES_TYPE_STRING = 0;
    public static final int RES_TYPE_DRAWABLE = 1;

    //通常是INVALID_POS
    //孪生的Item位置信息，如果配置这个，说明当前这个Item是不能单独生效，必须匹配到对于的“孪生Item”一起使用
    public int gemel_x = INVALID_POS; //(当前Item存在孪生性质，不能单独生效，需要配合孪生Item一起)孪生Item的x位置
    public int gemel_y = INVALID_POS; //(当前Item存在孪生性质，不能单独生效，需要配合孪生Item一起)孪生Item的x位置

    //位置
    public int x = INVALID_POS;       //结合y一起决定显示位置
    public int y = INVALID_POS;       //结合x一起决定显示位置

    //内容
    public int action_type = TYPE_UNKOWN;        //action行为的表示Type符
    public String action_id = null;             //动作行为id标识符，一般是告诉宿主当前Item点击的跳转去向的类名信息
    public String title = null;                 //显示的文本
    public String icon = null;                  //显示的图标
    public String statistic_key = null;         //统计上报标识符

    //通常是null
    public String extras = null;                //显示的扩展内容,比如：替换成插件的fragment后需要更改activity的标题

    @Override
    public String toString() {
        return "DisplayItem x=" + x + " y=" + y + " action_type=" + action_type + " action_id=" + action_id
                + " title=" + title + " icon=" + icon + " statistic_key=" + statistic_key + " extras=" +
                extras + " gemel_x:" + gemel_x + " gemel_y=" + gemel_y;
    }

    public void printf() {
        QRomLog.i(TAG, toString());
    }
}
