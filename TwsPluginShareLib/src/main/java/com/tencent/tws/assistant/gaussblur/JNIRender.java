package com.tencent.tws.assistant.gaussblur;

import android.graphics.Bitmap;

public class JNIRender {
	
	static {
//        System.loadLibrary("blurjni");
    }
	
	private static native void Blur(Bitmap in, Bitmap out, int r);

	public void blur(int radius, Bitmap in, Bitmap out) {
		Blur(in, out, radius);
	}
}
