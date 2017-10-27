package com.tws.plugin.content;

import java.io.Serializable;

import qrom.component.log.QRomLog;

/**
 * 分隔符： # 分割DisplayConfig @ 分割DisplayConfig的属性 = 属性赋值 / 分割属性值
 */
public class DisplayConfig implements Serializable {
	public static final int DISPLAY_AT_HOTSEAT = 1;
	public static final int DISPLAY_AT_HOME_FRAGEMENT = 2;
	public static final int DISPLAY_AT_MENU = 3;
	public static final int DISPLAY_AT_OTHER_POS = 4;

	public static final String SEPARATOR_CONFIG = "#";
	public static final String SEPARATOR_ATTRIBUTE = "@";
	public static final String SEPARATOR_VALUATION = "=";
	public static final String SEPARATOR_VALUE = "/";
	public static final String SEPARATOR_VER = "_";
	public static final String SEPARATOR_DEPEND = SEPARATOR_VALUE;

	public static final int TYPE_FRAGMENT = 1;
	public static final int TYPE_ACTIVITY = 2;
	public static final int TYPE_SERVICE = 3;
	public static final int TYPE_PACKAGENAEM = 4; // only start plugin
	public static final int TYPE_VIEW = 5;
	public static final int TYPE_PLUGIN_CUSTOM_CLICK = 6; // 插件自己定义点击事件的去向

	public static final int ACTIONBAR_BTN_TYPE_STRING = 1; // 文本资源
	public static final int ACTIONBAR_BTN_TYPE_ICON = 2; // 图标资源

	private static final String TAG = "rick_Print:DisplayConfig";
	// 显示位置 1、Hotseat 2、MyWatchFragment 3、ActionBarMenu 4、其他(service)
	public int pos = 0;
	public int secondPos = 0;
	// 显示标题 文本内容
	public String title = null;

	// 显示的内容类型 1、fragment 2、activity 3、service 4、packagename 5、view
	// 6、插件自定义的buttong点击事件
	public int contentType = 0;

	// 显示的内容 fragment直接复制exported-fragment的name;activity或者service组件配置类路径信息
	public String content = null;

	// 图标资源名 资源文件名
	public String iconResName = null;

	// 分组id
	public String gid = null;
	// 分组索引位置
	public int gpos = 0;
	// 分组名称
	public String gtitle = null;
	// 分组图标资源名
	public String gicon = null;

	// 用于统计
	public String statKey = null;
	// ActionBar标题ab-title：actionbar标题 中文/英文 也可以只配置一个
	// 暂不支持subTitle,显示的内容根据类型不同而不一样
	public String ab_title = null;
	// ActionBar标题类型ab-titlerestype：0:文本按钮(res配置中英文String) 1:ImageView(res配置图标)
	public int ab_titlerestype;
	// ActionBar右侧按钮上触发点击后的行为内容
	public String ab_rbtncontent = null;
	// ActionBar右侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
	public int ab_rbtnctype = 2;// 默认是activity【也可以由插件自己制定点击事件，只是这个需要插件每次切换到焦点的时候都设置一下】
	// 显示在ActionBar右侧按钮上的内容类型 1、String文本资源 2、图标资源【默认是图标资源】
	public int ab_rbtnrestype = 2;
	// 显示在ActionBar右侧按钮上的内容，根据类型进行配置
	public String ab_rbtnres = null;

	// ActionBar左侧按钮上触发点击后的行为内容
	public String ab_lbtncontent = null;
	// ActionBar左侧按钮上触发点击后行为的内容类型，同contentType【当前默认是activity，而且也暂时只有activity】
	public int ab_lbtnctype = 2;// 默认是activity【也可以由插件自己制定点击事件，只是这个需要插件每次切换到焦点的时候都设置一下】
	// 显示在ActionBar左侧按钮上的内容类型 1、String文本资源 2、图标资源【默认是图标资源】
	public int ab_lbtnrestype = 2;
	// 显示在ActionBar左侧按钮上的内容，根据类型进行配置
	public String ab_lbtnres = null;

	public void printf() {
		QRomLog.d(TAG, "DisplayConfig pos=" + pos + " secondPos=" + secondPos + " title=" + title + " cType="
				+ contentType + " content=" + content + " iconResName=" + iconResName);

		if (pos == 1) {
			QRomLog.d(TAG, "DisplayConfig ActionBar info ab_title=" + ab_title + " ab_rbtnctype=" + ab_rbtnctype
					+ " ab_rbtncontent=" + ab_rbtncontent + " ab_rbtnrestype=" + ab_rbtnrestype + " ab_rbtnres="
					+ ab_rbtnres + " ab_lbtnctype=" + ab_lbtnctype + " ab_lbtncontent=" + ab_lbtncontent
					+ " ab_lbtnrestype=" + ab_lbtnrestype + " ab_lbtnres=" + ab_lbtnres);
		}
	}
}
