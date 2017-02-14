/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tws.assistant.utils;

import com.tencent.tws.assistant.support.v4.graphics.ColorUtils;
import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;

/**
 * @hide
 */
public class ThemeUtils {

    private static final ThreadLocal<TypedValue> TL_TYPED_VALUE = new ThreadLocal<TypedValue>();

    public static final int[] DISABLED_STATE_SET = new int[] {-android.R.attr.state_enabled};
    public static final int[] FOCUSED_STATE_SET = new int[] {android.R.attr.state_focused};
    public static final int[] ACTIVATED_STATE_SET = new int[] {android.R.attr.state_activated};
    public static final int[] PRESSED_STATE_SET = new int[] {android.R.attr.state_pressed};
    public static final int[] CHECKED_STATE_SET = new int[] {android.R.attr.state_checked};
    public static final int[] SELECTED_STATE_SET = new int[] {android.R.attr.state_selected};
    public static final int[] NOT_PRESSED_OR_FOCUSED_STATE_SET = new int[] {-android.R.attr.state_pressed,
            -android.R.attr.state_focused};
    public static final int[] EMPTY_STATE_SET = new int[0];

    private static final int[] TEMP_ARRAY = new int[1];

    public static final int ACTIONBAR_BACKGROUND_NORMAL = 0;
    public static final int ACTIONBAR_BACKGROUND_GRADIENT = 1;
    
    public static final int ACTIONBARTAB_THEME_NORMAL = 0;
    public static final int ACTIONBARTAB_THEME_WAVE = 1;

    public static ColorStateList createDisabledStateList(int textColor, int disabledTextColor) {
        // Now create a new ColorStateList with the default color, and the new disabled
        // color
        final int[][] states = new int[2][];
        final int[] colors = new int[2];
        int i = 0;

        // Disabled state
        states[i] = DISABLED_STATE_SET;
        colors[i] = disabledTextColor;
        i++;

        // Default state
        states[i] = EMPTY_STATE_SET;
        colors[i] = textColor;
        i++;

        return new ColorStateList(states, colors);
    }

    public static int getThemeAttrColor(Context context, int attr) {
        TEMP_ARRAY[0] = attr;
        TypedArray a = context.obtainStyledAttributes(null, TEMP_ARRAY);
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    public static ColorStateList getThemeAttrColorStateList(Context context, int attr) {
        TEMP_ARRAY[0] = attr;
        TypedArray a = context.obtainStyledAttributes(null, TEMP_ARRAY);
        try {
            return a.getColorStateList(0);
        } finally {
            a.recycle();
        }
    }

    public static int getDisabledThemeAttrColor(Context context, int attr) {
        final ColorStateList csl = getThemeAttrColorStateList(context, attr);
        if (csl != null && csl.isStateful()) {
            // If the CSL is stateful, we'll assume it has a disabled state and use it
            return csl.getColorForState(DISABLED_STATE_SET, csl.getDefaultColor());
        } else {
            // Else, we'll generate the color using disabledAlpha from the theme

            final TypedValue tv = getTypedValue();
            // Now retrieve the disabledAlpha value from the theme
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, tv, true);
            final float disabledAlpha = tv.getFloat();

            return getThemeAttrColor(context, attr, disabledAlpha);
        }
    }

    private static TypedValue getTypedValue() {
        TypedValue typedValue = TL_TYPED_VALUE.get();
        if (typedValue == null) {
            typedValue = new TypedValue();
            TL_TYPED_VALUE.set(typedValue);
        }
        return typedValue;
    }

    public static int getThemeAttrColor(Context context, int attr, float alpha) {
        final int color = getThemeAttrColor(context, attr);
        final int originalAlpha = Color.alpha(color);
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha));
    }

    public static boolean isShowRipple(Context context){
        return context.getResources().getBoolean(R.bool.config_show_ripple);
    }

    public static boolean isActionBarSplitTheme(Context context){
        return context.getResources().getBoolean(R.bool.config_actionbar_split_theme);
    }

    public static int getActionBarBackgroundStyle(Context context){
        return context.getResources().getInteger(R.integer.config_actionbar_background_style);
    }
    
    public static int getActionBarTabStyle(Context context){
        return context.getResources().getInteger(R.integer.config_actionbartab_theme);
    }

    /**
     * check actionBar background is normal
     * @param context
     */
    public static boolean isActionBarBackgroundNormal(Context context){
        return getActionBarBackgroundStyle(context) == ACTIONBAR_BACKGROUND_NORMAL;
    }

    /**
     * check actionBar background is gradient
     * @param context
     */
    public static boolean isActionBarBackgroundGradient(Context context){
        return getActionBarBackgroundStyle(context) == ACTIONBAR_BACKGROUND_GRADIENT;
    }
    
    /**
     * check actionBarTab style is normal
     * @param context
     */
    public static boolean isActionBarTabStyleNormal(Context context){
        return getActionBarTabStyle(context) == ACTIONBARTAB_THEME_NORMAL;
    }
    
    /**
     * check actionBarTab style is wave
     * @param context
     */
    public static boolean isActionBarTabStyleWave(Context context){
        return getActionBarTabStyle(context) == ACTIONBARTAB_THEME_WAVE;
    }

    /**
     * base widget support tint
     * @param context
     */
    public static boolean isSupportTintDrawable(Context context){
        return context.getResources().getBoolean(R.bool.config_base_widget_tint_drawable);
    }
}
