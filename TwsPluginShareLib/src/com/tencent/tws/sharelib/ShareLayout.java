package com.tencent.tws.sharelib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * 仅仅用来测试插件程序中是否可以使用宿主程序中的自定义控件和控件的布局文件
 * @author yongchen
 *
 */
public class ShareLayout extends LinearLayout {
	
	public ShareLayout(Context context) {
		super(context);
	}
	
	public ShareLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		LayoutInflater.from(getContext()).inflate(R.layout.share_layout, this);
	}
}
