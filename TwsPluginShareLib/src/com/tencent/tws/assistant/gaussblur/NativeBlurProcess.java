package com.tencent.tws.assistant.gaussblur;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class NativeBlurProcess {
	
	private static native void functionToBlur(Bitmap bitmapOut, int radius, int threadCount, int threadIndex, int round);
	
	public static boolean noBlurSo = false;

	static {
		try {
			System.loadLibrary("blurjni");
		} catch (UnsatisfiedLinkError e) {
			noBlurSo = true;
		}
	}

	public Bitmap blur(Bitmap original, float radius) {

		int cores = StackBlurManager.EXECUTOR_THREADS;
		cores = 1;

		ArrayList<NativeTask> horizontal = new ArrayList<NativeTask>(cores);
		ArrayList<NativeTask> vertical = new ArrayList<NativeTask>(cores);
		for (int i = 0; i < cores; i++) {
			horizontal.add(new NativeTask(original, (int) radius, cores, i, 1));
			vertical.add(new NativeTask(original, (int) radius, cores, i, 2));
		}

		try {
			StackBlurManager.EXECUTOR.invokeAll(horizontal);
		} catch (InterruptedException e) {
			return original;
		}

		try {
			StackBlurManager.EXECUTOR.invokeAll(vertical);
		} catch (InterruptedException e) {
			return original;
		}
		return original;
	}

	private static class NativeTask implements Callable<Void> {
		private final Bitmap _bitmapOut;
		private final int _radius;
		private final int _totalCores;
		private final int _coreIndex;
		private final int _round;

		public NativeTask(Bitmap bitmapOut, int radius, int totalCores, int coreIndex, int round) {
			_bitmapOut = bitmapOut;
			_radius = radius;
			_totalCores = totalCores;
			_coreIndex = coreIndex;
			_round = round;
		}

		@Override public Void call() throws Exception {
			functionToBlur(_bitmapOut, _radius, _totalCores, _coreIndex, _round);
			return null;
		}

	}
}
