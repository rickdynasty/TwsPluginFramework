package com.tencent.tws.assistant.drawable;

import java.lang.reflect.Field;

import com.tencent.tws.assistant.utils.ReflectUtils;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.util.Log;
import android.widget.ScrollBarDrawable;

public class TwsScrollBarDrawable extends ScrollBarDrawable {
	private static final String TAG = "com.tencent.tws.assistant.drawable.TwsScrollBarDrawable";
	
    private int twsViewLength, twsViewStart, twsViewEnd;
    public void twsSetParameters(int viewLength, int viewStart, int viewEnd) {
    	Log.d(TAG, "twsSetParameters|len="+viewLength+"startt="+viewStart+",end="+viewEnd);
    	
    	twsViewLength = viewLength;
    	twsViewStart = viewStart;
    	twsViewEnd = viewEnd;
    }
    
	protected void twsDrawThumb(Canvas canvas, Rect bounds, int offset,
			int length, boolean vertical) {
		try {
			final Rect thumbRect = (Rect) getField("mTempBounds");
			final boolean changed = (Boolean)getField("mRangeChanged") || (Boolean)getField("mChanged");
			if (changed) {
				if (vertical) {
					if (twsViewLength == 0)
						twsViewLength = 1;
					float factor = ((float) (offset + length) / (float) twsViewLength) > 1.0f ? 1.0f
							: ((float) (offset + length) / (float) twsViewLength);
					thumbRect
							.set(bounds.left,
									bounds.top
											+ offset
											+ ((int) ((0 - twsViewStart - twsViewEnd)
													* factor + twsViewStart)),
									bounds.right,
									bounds.top
											+ offset
											+ ((int) ((0 - twsViewStart - twsViewEnd)
													* factor + twsViewStart))
											+ length);
				} else {
					thumbRect.set(bounds.left + offset, bounds.top, bounds.left
							+ offset + length, bounds.bottom);
				}
			}

			if (vertical) {
				final Drawable thumb = (Drawable)getField("mVerticalThumb");
				if (changed)
					thumb.setBounds(thumbRect);
				thumb.draw(canvas);
			} else {
				final Drawable thumb = (Drawable)getField("mHorizontalThumb");
				if (changed)
					thumb.setBounds(thumbRect);
				thumb.draw(canvas);
			}
		} catch (Exception e) {
			Log.e(TAG, "twsDrawThumb|exp:" + e.getMessage());
		}
	}
    
    @Override
    public void draw(Canvas canvas) {
        final boolean vertical = (Boolean)getField("mVertical");
        final int extent = (Integer)getField("mExtent");
        final int range = (Integer)getField("mRange");

        boolean drawTrack = true;
        boolean drawThumb = true;
        if (extent <= 0 || range <= extent) {
            drawTrack = vertical ? (Boolean)getField("mAlwaysDrawVerticalTrack") : (Boolean)getField("mAlwaysDrawHorizontalTrack");
            drawThumb = false;
        }

        Rect r = getBounds();
        if (canvas.quickReject(r.left, r.top, r.right, r.bottom, Canvas.EdgeType.AA)) {
            return;
        }
        if (drawTrack) {
            drawTrack(canvas, r, vertical);
        }

        if (drawThumb) {
            int size = vertical ? r.height() : r.width();
            int thickness = vertical ? r.width() : r.height();
            int length = Math.round((float) size * extent / range);
            int offset = Math.round((float) (size - length) * (Integer)getField("mOffset") / (range - extent));

            // avoid the tiny thumb
            int minLength = thickness * 2;
            if (length < minLength) {
                length = minLength;
            }
            // avoid the too-big thumb
            if (offset + length > size) {
                offset = size - length;
            }

            //tws-start recalculate scroll params::2015-1-9
            twsDrawThumb(canvas, r, offset, length, vertical);
            //tws-end recalculate scroll params::2015-1-9
        }
    }
    
    private Object getField(String fieldName) {
    	Object value = null;
		try {	
			Class<?> clz = ReflectUtils.forClassName("android.widget.ScrollBarDrawable");
			value = ReflectUtils.getFieldValue(fieldName, this, clz);
		} catch (Exception e) {
			Log.e(TAG, "twsSetParameters exp:"+e.getMessage());
		}
		
		return value;
    }
}
