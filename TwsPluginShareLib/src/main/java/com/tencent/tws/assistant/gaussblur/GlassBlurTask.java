package com.tencent.tws.assistant.gaussblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;

public class GlassBlurTask {
    protected static final String TAG = "BlurTask";
    private Bitmap source;
    private Canvas canvas;
    private AsyncTask<Void, Void, Void> task;
    private Bitmap blurred;
    private Listener listener;
    private Context context;
    private int radius;

    public interface Listener {
        void onBlurOperationFinished();
    }

    public GlassBlurTask(Context context, Listener listener, Bitmap source) {
        this(context, listener, source, GlassBlurUtil.DEFAULT_BLUR_RADIUS);
    }

    public GlassBlurTask(Context context, Listener listener, Bitmap source, int radius) {
        this.context = context;
        this.listener = listener;
        this.source = source;
        this.radius = radius;
        canvas = new Canvas(source);
        startTask();
    }

    private void startTask() {
        task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                blurSourceBitmap();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                canvas.drawBitmap(blurred, 0, 0, null);
                blurred.recycle();
                listener.onBlurOperationFinished();
            }
        };
        task.execute();
    }

    private void blurSourceBitmap() {
        Bitmap section = source;
        if (section == null) {
            // Probably indicates we've reached the end.
            return;
        }
        blurred = GlassBlur.apply(context, source, radius);
    }

    public void cancel() {
        if (task != null) {
            task.cancel(true);
        }
        task = null;
    }
}
