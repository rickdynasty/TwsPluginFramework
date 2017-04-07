package com.tencent.tws.assistant.widget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.tencent.tws.assistant.support.v4.graphics.drawable.DrawableCompat;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

public class DrawableUtils {

    private static final String TAG = "DrawableUtils";

    public static final Rect INSETS_NONE = new Rect();

    private static Class<?> sInsetsClazz;

    static {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                sInsetsClazz = Class.forName("android.graphics.Insets");
            } catch (ClassNotFoundException e) {
                // Oh well...
            }
        }
    }

    private DrawableUtils() {}

    /**
     * Allows us to get the optical insets for a {@link Drawable}. Since this is hidden we need to
     * use reflection. Since the {@code Insets} class is hidden also, we return a Rect instead.
     */
    public static Rect getOpticalBounds(Drawable drawable) {
        if (sInsetsClazz != null) {
            try {
                // If the Drawable is wrapped, we need to manually unwrap it and process
                // the wrapped drawable.
                drawable = DrawableCompat.unwrap(drawable);

                final Method getOpticalInsetsMethod = drawable.getClass()
                        .getMethod("getOpticalInsets");
                final Object insets = getOpticalInsetsMethod.invoke(drawable);

                if (insets != null) {
                    // If the drawable has some optical insets, let's copy them into a Rect
                    final Rect result = new Rect();

                    for (Field field : sInsetsClazz.getFields()) {
                        final String fName = field.getName();
                        if (fName.equals("left")) {
                            result.left = field.getInt(insets);
                        } else if (fName.equals("top")) {
                            result.top = field.getInt(insets);
                        } else if (fName.equals("right")) {
                            result.right = field.getInt(insets);
                        } else if (fName.equals("bottom")) {
                            result.bottom = field.getInt(insets);
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                // Eugh, we hit some kind of reflection issue...
                Log.e(TAG, "Couldn't obtain the optical insets. Ignoring.");
            }
        }

        // If we reach here, either we're running on a device pre-v18, the Drawable didn't have
        // any optical insets, or a reflection issue, so we'll just return an empty rect
        return INSETS_NONE;
    }

}