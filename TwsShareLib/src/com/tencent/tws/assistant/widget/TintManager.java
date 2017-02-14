package com.tencent.tws.assistant.widget;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;

import com.tencent.tws.assistant.support.v4.graphics.drawable.DrawableCompat;
import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * @hide
 */
public class TintManager {

    public static final boolean SHOULD_BE_USED = true;

    private static final String TAG = "TintManager";
    private static final boolean DEBUG = false;
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private static final WeakHashMap<Context, TintManager> INSTANCE_CACHE = new WeakHashMap<Context, TintManager>();
    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal}, using the default
     * mode using a raw color filter.
     */
    private static final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = {};

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal}, using
     * {@link DrawableCompat}'s tinting functionality.
     */
    private static final int[] TINT_COLOR_CONTROL_NORMAL = {};

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlActivated}, using a color
     * filter.
     */
    private static final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = {};

    /**
     * Drawables which should be tinted with the value of {@code android.R.attr.colorBackground}, using the
     * {@link android.graphics.PorterDuff.Mode#MULTIPLY} mode and a color filter.
     */
    private static final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = {};

    /**
     * Drawables which should be tinted using a state list containing values of
     * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated}
     */
//    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {R.drawable.abc_switch_track_mtrl_alpha,
//            R.drawable.abc_switch_thumb_material,};
    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {};

    /**
     * Drawables which should be tinted using a state list containing values of
     * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated} for the checked state.
     */
    private static final int[] TINT_CHECKABLE_BUTTON_LIST = {R.drawable.btn_check_holo_light,
            R.drawable.btn_radio_holo_light};

    private final WeakReference<Context> mContextRef;
    private SparseArray<ColorStateList> mTintLists;
    private ColorStateList mDefaultColorStateList;

    public TintManager(Context context) {
        mContextRef = new WeakReference<Context>(context);
    }

    public static TintManager get(Context context) {
        TintManager tm = INSTANCE_CACHE.get(context);
        if (tm == null) {
            tm = new TintManager(context);
            INSTANCE_CACHE.put(context, tm);
        }
        return tm;
    }

    /**
     * A helper method to get a {@link TintManager} and then call {@link #getDrawable(int)}. This method
     * should not be used routinely.
     */
    public static Drawable getDrawable(Context context, int resId) {
        if (isInTintList(resId)) {
            return TintManager.get(context).getDrawable(resId);
        } else {
            return context.getResources().getDrawable(resId);
        }
    }

    public Drawable getDrawable(int resId) {
        return getDrawable(resId, false);
    }

    public Drawable getDrawable(int resId, boolean failIfNotKnown) {
        final Context context = mContextRef.get();
        if (context == null)
            return null;

        Drawable drawable = context.getResources().getDrawable(resId);

        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= 8) {
                // Mutate can cause NPEs on 2.1
                drawable = drawable.mutate();
            }

            final ColorStateList tintList = getTintList(resId);
            if (tintList != null) {
                // First wrap the Drawable and set the tint list
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tintList);

                // If there is a blending mode specified for the drawable, use it
                final PorterDuff.Mode tintMode = getTintMode(resId);
                if (tintMode != null) {
                    DrawableCompat.setTintMode(drawable, tintMode);
                }
            }  else if (resId == R.drawable.seekbar_progress_holo_light) {
                LayerDrawable ld = (LayerDrawable) drawable;
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.background),
                        ThemeUtils.getThemeAttrColor(context, R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.secondaryProgress),
                        ThemeUtils.getThemeAttrColor(context, R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.progress),
                        ThemeUtils.getThemeAttrColor(context, R.attr.colorControlActivated), DEFAULT_MODE);
            } else {
                final boolean usedColorFilter = tintDrawableUsingColorFilter(resId, drawable);
                if (!usedColorFilter && failIfNotKnown) {
                    // If we didn't tint using a ColorFilter, and we're set to fail if we don't
                    // know the id, return null
                    drawable = null;
                }
            }
        }
        return drawable;
    }

    public final boolean tintDrawableUsingColorFilter(final int resId, Drawable drawable) {
        final Context context = mContextRef.get();
        if (context == null)
            return false;

        PorterDuff.Mode tintMode = DEFAULT_MODE;
        boolean colorAttrSet = false;
        int colorAttr = 0;
        int alpha = -1;

        if (arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, resId)) {
            colorAttr = R.attr.colorControlNormal;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, resId)) {
            colorAttr = R.attr.colorControlActivated;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, resId)) {
            colorAttr = android.R.attr.colorBackground;
            colorAttrSet = true;
            tintMode = PorterDuff.Mode.MULTIPLY;
        }

        if (colorAttrSet) {
            final int color = ThemeUtils.getThemeAttrColor(context, colorAttr);
            drawable.setColorFilter(getPorterDuffColorFilter(color, tintMode));

            if (alpha != -1) {
                drawable.setAlpha(alpha);
            }

            if (DEBUG) {
                Log.d(TAG, "Tinted Drawable: " + context.getResources().getResourceName(resId)
                        + " with color: #" + Integer.toHexString(color));
            }
            return true;
        }
        return false;
    }

    private static boolean arrayContains(int[] array, int value) {
        for (int id : array) {
            if (id == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInTintList(int drawableId) {
        return arrayContains(TINT_COLOR_CONTROL_NORMAL, drawableId)
                || arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, drawableId)
                || arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, drawableId)
                || arrayContains(TINT_COLOR_CONTROL_STATE_LIST, drawableId)
                || arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, drawableId)
                || arrayContains(TINT_CHECKABLE_BUTTON_LIST, drawableId);
    }

    final PorterDuff.Mode getTintMode(final int resId) {
        PorterDuff.Mode mode = null;

//        if (resId == R.drawable.abc_switch_thumb_material) {
//            mode = PorterDuff.Mode.MULTIPLY;
//        }

        return mode;
    }

    public final ColorStateList getTintList(int resId) {
        final Context context = mContextRef.get();
        if (context == null)
            return null;

        // Try the cache first (if it exists)
        ColorStateList tint = mTintLists != null ? mTintLists.get(resId) : null;

        if (tint == null) {
            // ...if the cache did not contain a color state list, try and create one
//            if (resId == R.drawable.abc_switch_track_mtrl_alpha) {
//                tint = createSwitchTrackColorStateList(context);
//            } else if (resId == R.drawable.abc_switch_thumb_material) {
//                tint = createSwitchThumbColorStateList(context);
//            } else 
            if (arrayContains(TINT_COLOR_CONTROL_NORMAL, resId)) {
                tint = ThemeUtils.getThemeAttrColorStateList(context, R.attr.colorControlNormal);
            } else if (arrayContains(TINT_COLOR_CONTROL_STATE_LIST, resId)) {
                tint = getDefaultColorStateList(context);
            } else if (arrayContains(TINT_CHECKABLE_BUTTON_LIST, resId)) {
                tint = createCheckableButtonColorStateList(context);
            } else if (resId == R.drawable.seek_thumb) {
                tint = createSeekbarThumbColorStateList(context);
            }

            if (tint != null) {
                if (mTintLists == null) {
                    // If our tint list cache hasn't been set up yet, create it
                    mTintLists = new SparseArray<ColorStateList>();
                }
                // Add any newly created ColorStateList to the cache
                mTintLists.append(resId, tint);
            }
        }
        return tint;
    }

    private ColorStateList getDefaultColorStateList(Context context) {
        if (mDefaultColorStateList == null) {
            /**
             * Generate the default color state list which uses the colorControl attributes. Order is
             * important here. The default enabled state needs to go at the bottom.
             */

            final int colorControlNormal = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlNormal);
            final int colorControlActivated = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlActivated);

            final int[][] states = new int[7][];
            final int[] colors = new int[7];
            int i = 0;

            // Disabled state
            states[i] = ThemeUtils.DISABLED_STATE_SET;
            colors[i] = ThemeUtils.getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
            i++;

            states[i] = ThemeUtils.FOCUSED_STATE_SET;
            colors[i] = colorControlActivated;
            i++;

            states[i] = ThemeUtils.ACTIVATED_STATE_SET;
            colors[i] = colorControlActivated;
            i++;

            states[i] = ThemeUtils.PRESSED_STATE_SET;
            colors[i] = colorControlActivated;
            i++;

            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = colorControlActivated;
            i++;

            states[i] = ThemeUtils.SELECTED_STATE_SET;
            colors[i] = colorControlActivated;
            i++;

            // Default enabled state
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = colorControlNormal;
            i++;

            mDefaultColorStateList = new ColorStateList(states, colors);
        }
        return mDefaultColorStateList;
    }

    private ColorStateList createCheckableButtonColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = ThemeUtils.getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchTrackColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, android.R.attr.colorForeground, 0.1f);
        i++;

        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlActivated, 0.3f);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, android.R.attr.colorForeground, 0.3f);
        i++;

        return new ColorStateList(states, colors);
    }

    /*private ColorStateList createSwitchThumbColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        final ColorStateList thumbColor = getThemeAttrColorStateList(context, R.attr.colorSwitchThumbNormal);

        if (thumbColor != null && thumbColor.isStateful()) {
            // If colorSwitchThumbNormal is a valid ColorStateList, extract the default and
            // disabled colors from it

            // Disabled state
            states[i] = ThemeUtils.DISABLED_STATE_SET;
            colors[i] = thumbColor.getColorForState(states[i], 0);
            i++;

            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = thumbColor.getDefaultColor();
            i++;
        } else {
            // Else we'll use an approximation using the default disabled alpha

            // Disabled state
            states[i] = ThemeUtils.DISABLED_STATE_SET;
            colors[i] = getDisabledThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;

            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;
        }

        return new ColorStateList(states, colors);
    }

    private ColorStateList createEditTextColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createDefaultButtonColorStateList(Context context) {
        return createButtonColorStateList(context, R.attr.colorButtonNormal);
    }

    private ColorStateList createColoredButtonColorStateList(Context context) {
        return createButtonColorStateList(context, R.attr.colorAccent);
    }

    private ColorStateList createButtonColorStateList(Context context, int baseColorAttr) {
        final int[][] states = new int[4][];
        final int[] colors = new int[4];
        int i = 0;

        final int baseColor = getThemeAttrColor(context, baseColorAttr);
        final int colorControlHighlight = getThemeAttrColor(context, R.attr.colorControlHighlight);

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorButtonNormal);
        i++;

        states[i] = ThemeUtils.PRESSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;

        states[i] = ThemeUtils.FOCUSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = baseColor;
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSpinnerColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }*/

    private ColorStateList createSeekbarThumbColorStateList(Context context) {
        final int[][] states = new int[2][];
        final int[] colors = new int[2];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = ThemeUtils.getDisabledThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }

    public static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        if (shouldMutateBackground(drawable) && drawable.mutate() != drawable) {
            Log.d(TAG, "Mutated drawable is not the same instance as the input.");
            return;
        }

        if (tint.mHasTintList || tint.mHasTintMode) {
            drawable.setColorFilter(createTintFilter(tint.mHasTintList ? tint.mTintList : null,
                    tint.mHasTintMode ? tint.mTintMode : DEFAULT_MODE, state));
        } else {
            drawable.clearColorFilter();
        }

        if (Build.VERSION.SDK_INT <= 10) {
            // On Gingerbread, GradientDrawable does not invalidate itself when it's
            // ColorFilter has changed, so we need to force an invalidation
            drawable.invalidateSelf();
        }
    }

    private static boolean shouldMutateBackground(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 16) {
            // For SDK 16+, we should be fine mutating the drawable
            return true;
        }

        if (drawable instanceof LayerDrawable) {
            return Build.VERSION.SDK_INT >= 16;
        } else if (drawable instanceof InsetDrawable) {
            return Build.VERSION.SDK_INT >= 14;
        } else if (drawable instanceof DrawableContainer) {
            // If we have a DrawableContainer, let's traverse it's child array
            final Drawable.ConstantState state = drawable.getConstantState();
            if (state instanceof DrawableContainer.DrawableContainerState) {
                final DrawableContainer.DrawableContainerState containerState = (DrawableContainer.DrawableContainerState) state;
                for (Drawable child : containerState.getChildren()) {
                    if (!shouldMutateBackground(child)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static PorterDuffColorFilter createTintFilter(ColorStateList tint, PorterDuff.Mode tintMode,
            final int[] state) {
        if (tint == null || tintMode == null) {
            return null;
        }
        final int color = tint.getColorForState(state, Color.TRANSPARENT);
        return getPorterDuffColorFilter(color, tintMode);
    }

    private static PorterDuffColorFilter getPorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        // First, lets see if the cache already contains the color filter
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);

        if (filter == null) {
            // Cache miss, so create a color filter and add it to the cache
            filter = new PorterDuffColorFilter(color, mode);
            COLOR_FILTER_CACHE.put(color, mode, filter);
        }

        return filter;
    }

    private static void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
        d.setColorFilter(getPorterDuffColorFilter(color, mode == null ? DEFAULT_MODE : mode));
    }

    public static PorterDuff.Mode parseTintMode(int value, Mode defaultMode) {
        switch (value) {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return Build.VERSION.SDK_INT >= 11 ? PorterDuff.Mode.valueOf("ADD") : defaultMode;
            default:
                return defaultMode;
        }
    }
}
