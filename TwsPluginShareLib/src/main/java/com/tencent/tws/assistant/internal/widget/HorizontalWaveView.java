package com.tencent.tws.assistant.internal.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.WindowManager;

public class HorizontalWaveView extends View {

    private Paint mPaint;
    private Path mPath;

    private int delta, mHeight, mWidth;
    
    private Bitmap mBitmap;
	private int mAmplitude;
	
	private static final float mPathDelta = 1.0f;
    
	public HorizontalWaveView(Context context, int height) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPath = new Path();
        mHeight = height;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) {
        	setPath();
            int sc = canvas.saveLayer(0.0f, 0.0f, mBitmap.getWidth(), mBitmap.getHeight(), null,
                    Canvas.MATRIX_SAVE_FLAG |
                    Canvas.CLIP_SAVE_FLAG |
                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                    Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                    Canvas.CLIP_TO_LAYER_SAVE_FLAG);
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPath(mPath, mPaint);
            mPaint.setXfermode(null);
            canvas.restoreToCount(sc);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if (mBitmap != null)
        	setMeasuredDimension(mBitmap.getWidth(), mHeight);
        mWidth = getWidth();
        
    }

    public void setPath() {
        float x = 0.0f;
        float y = 0.0f;
        delta+=15;
        if (delta >= 360)
        	delta = 0;
        mPath.reset();
        for (int i = 0; i < mWidth; i++) {
            x = i;
            y = (float)(mAmplitude * Math.sin((i * 0.5f + delta) * Math.PI / 180) + mHeight * 0.98f);
            if (i == 0) {
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
        }
        mPath.lineTo(mWidth, mHeight+mPathDelta);
        mPath.lineTo(0, mHeight+mPathDelta);
        mPath.close();
        if (mAmplitude != 0) {
        	invalidate();
        }
    }
    
    
    public void setStackedDrawable(Drawable drawable) {
    	
    	mBitmap = Bitmap.createBitmap(((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth(), mHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap); 
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
    }
    
    public void setAmplitude(int amplitude) {
		mAmplitude = amplitude;
		invalidate();
	}
    
    public int getAmplitude() {
    	return mAmplitude;
    }
}