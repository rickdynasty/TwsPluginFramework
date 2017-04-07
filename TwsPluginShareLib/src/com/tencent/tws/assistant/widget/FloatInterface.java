package com.tencent.tws.assistant.widget;

import android.view.KeyEvent;
import android.view.View;

//tws-start add global float view::2014-09-13
public interface FloatInterface {

	public static final int BUTTON_LEFT 	= 0;
	
	public static final int BUTTON_MIDDLE 	= 1;
	
	public static final int BUTTON_RIGHT 	= 2;
	
	public void cancel();
	
	public void dismiss();
	
	interface OnCancelListener {
		public void onCancel(FloatInterface f);
	}
	
	interface OnDismissListener {
		public void onDismiss(FloatInterface f);
	}

	interface onDismissDelayListener {
		public void onDismiss(FloatInterface f);
	}
	
	interface onShowListener {
		public void onShow(FloatInterface f);
	}
	
	
	interface OnButtonClickListener {
		public void onClick(FloatInterface f, int which);
	}
	
	interface OnItemClickListener {
		public void onClick(FloatInterface f, int which);
	}
	
	interface OnSingleChoiceClickListener {
		public void onClick(FloatInterface f, int which, boolean isChecked);
	}
	
	interface OnMultiChoiceClickListener {
		public void onClick(FloatInterface f, int which, boolean isChecked);
	}
	
	interface OnKeyListener {
		public boolean onKey(FloatInterface f, int keyCode, KeyEvent event);
	}
	interface OnContentClickListener {
		public void onClick(View z);
	}
}
//tws-end add global float view::2014-09-13