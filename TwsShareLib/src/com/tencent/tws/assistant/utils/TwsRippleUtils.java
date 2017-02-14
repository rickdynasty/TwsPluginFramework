package com.tencent.tws.assistant.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.tencent.tws.assistant.drawable.TwsRippleDrawable;
import com.tencent.tws.sharelib.R;

public class TwsRippleUtils {

    public static int RIPPLE_STYLE_NORMAL = TwsRippleDrawable.RIPPLE_STYLE_NORMAL;
    public static int RIPPLE_STYLE_CLEAR = TwsRippleDrawable.RIPPLE_STYLE_CLEAR;

    public static TwsRippleDrawable getDefaultDrawable(Context context) {
        return getClearDrawable(context);
    }

    public static TwsRippleDrawable getNormalDrawable(Context context) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.default_ripple_light);
        return getCustomDrawable(context, colors, null, null);
    }

    public static TwsRippleDrawable getClearDrawable(Context context) {
        return getCustomColorClearDrawable(context, R.color.default_ripple_light);
    }

    public static TwsRippleDrawable getDefaultDarkDrawable(Context context) {
        return getCustomColorClearDrawable(context, R.color.default_ripple_dark);
    }

    public static TwsRippleDrawable getCustomColorClearDrawable(Context context, int color) {
    	ColorStateList colors = context.getResources().getColorStateList(color);
        return getCustomDrawable(context, colors, null, null, RIPPLE_STYLE_CLEAR);
    }

    public static TwsRippleDrawable getDefaultListSelector(Context context) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.list_item_ripple_light);
        Drawable mask = context.getResources().getDrawable(R.color.tws_white);
        return getCustomDrawable(context, colors, null, mask);
    }

    public static TwsRippleDrawable getDefaultListDarkSelector(Context context) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.list_item_ripple_dark);
        Drawable mask = context.getResources().getDrawable(R.color.tws_white);
        return getCustomDrawable(context, colors, null, mask);
    }

    public static TwsRippleDrawable getHasContentDrawable(Context context, int contentId) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.default_ripple_light);
        Drawable content = null;
        if (contentId > 0) {
            content = context.getResources().getDrawable(contentId);
        }
        return getCustomDrawable(context, colors, content, null);
    }

    public static TwsRippleDrawable getHasContentDrawable(Context context, Drawable content) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.default_ripple_light);
        return getCustomDrawable(context, colors, content, null);
    }

    public static TwsRippleDrawable getHasMaskDrawable(Context context, int maskId) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.list_item_ripple_light);
        Drawable mask = null;
        if (maskId > 0) {
            mask = context.getResources().getDrawable(maskId);
        }
        return getCustomDrawable(context, colors, null, mask);
    }

    public static TwsRippleDrawable getHasMaskDrawable(Context context, Drawable mask) {
        ColorStateList colors = context.getResources().getColorStateList(R.color.list_item_ripple_light);
        return getCustomDrawable(context, colors, null, mask);
    }

    public static TwsRippleDrawable getCustomDrawable(Context context, int rippleColorId, int contentId,
            int maskId) {
        return getCustomDrawable(context, rippleColorId, contentId, maskId, RIPPLE_STYLE_NORMAL);
    }

    public static TwsRippleDrawable getCustomDrawable(Context context, int rippleColorId, int contentId,
            int maskId, int rippleStyle) {
        ColorStateList rippleColor = context.getResources().getColorStateList(rippleColorId);
        Drawable content = null;
        if (contentId > 0) {
            content = context.getResources().getDrawable(contentId);
        }
        Drawable mask = null;
        if (maskId > 0) {
            mask = context.getResources().getDrawable(maskId);
        }
        return getCustomDrawable(context, rippleColor, content, mask, rippleStyle);
    }

    public static TwsRippleDrawable getCustomDrawable(Context context, ColorStateList rippleColor,
            Drawable content, Drawable mask) {
        return getCustomDrawable(context, rippleColor, content, mask, RIPPLE_STYLE_NORMAL);
    }

    public static TwsRippleDrawable getCustomDrawable(Context context, ColorStateList rippleColor,
            Drawable content, Drawable mask, int rippleStyle) {
        ColorStateList defaultColor = context.getResources().getColorStateList(R.color.default_ripple_light);
        if (rippleColor == null) {
            rippleColor = defaultColor;
        }
        return new TwsRippleDrawable(rippleColor, content, mask, rippleStyle);
    }
}
