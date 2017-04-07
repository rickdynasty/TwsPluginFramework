package com.tencent.tws.assistant.gaussblur;

import android.graphics.Bitmap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StackBlurManager {
	
	static final int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
	static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

	private Bitmap outBitmap;
	private NativeBlurProcess blur;
	
	private static StackBlurManager sSingleton = null;
	
	public static synchronized StackBlurManager getInstance() {
		if (sSingleton == null) {
			sSingleton = new StackBlurManager();
		}
		return sSingleton;
	}
	
	private StackBlurManager() {
		blur = new NativeBlurProcess();
	}

	public Bitmap processNatively(Bitmap bitmap, int radius) {
		outBitmap = blur.blur(bitmap, radius);
		return outBitmap;
	}
}
