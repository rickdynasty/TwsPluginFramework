package com.tencent.tws.assistant.gaussblur;

import android.content.Context;
import android.graphics.Bitmap;

public class JNIBlur {
	
	private StackBlurManager mStackBlurManager;

	public JNIBlur(Context context) {
		mStackBlurManager = StackBlurManager.getInstance();
	}

	public Bitmap blur(Bitmap image, boolean fast) {
		return mStackBlurManager.processNatively(image, 10);
	}
	
	public Bitmap blurRadius(Bitmap image, int radius) {
		return mStackBlurManager.processNatively(image, radius);
	}
}

