/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.tencent.tws.assistant.utils.ThemeUtils;
import com.tencent.tws.sharelib.R;

/**
 * A {@link SeekBar} which supports compatible features on older version of the platform.
 * 
 * <p>
 * This will automatically be used when you use {@link SeekBar} in your layouts. You should only need to
 * manually use this class when writing custom views.
 * </p>
 */
public class SeekBar extends android.widget.SeekBar {

    private SeekBarHelper mSeekBarHelper;
    private TintManager mTintManager;

    public SeekBar(Context context) {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTintManager = TintManager.get(context);

        mSeekBarHelper = new SeekBarHelper(this, mTintManager);
        mSeekBarHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    public void setThumb(Drawable d, @Nullable int customColor) {
        if (d == null) {
            return;
        }
        ColorStateList csl = getResources().getColorStateList(customColor);
        if (csl != null) {
            setThumb(d, csl);
        }
    }

    public void setThumb(Drawable d, @Nullable ColorStateList customColor) {
        if (d == null) {
            return;
        }
        final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
        final boolean isSupportTintDrawable = ThemeUtils.isSupportTintDrawable(getContext());
        int colorControlNormal = 0;
        int colorControlDisabled = 0;
        if (customColor.isStateful()) {
            colorControlNormal = customColor.getColorForState(ThemeUtils.EMPTY_STATE_SET,
                    customColor.getDefaultColor());
            colorControlDisabled = customColor.getColorForState(ThemeUtils.DISABLED_STATE_SET,
                    customColor.getDefaultColor());
        } else {
            colorControlNormal = customColor.getDefaultColor();
            colorControlDisabled = customColor.getDefaultColor();
        }
        if (isSupportTintDrawable) {
            d.setColorFilter(colorControlNormal, DEFAULT_MODE);
        }
        setThumb(d);
        setThumbOffset(0);
    }

    public void setProgressDrawable(Drawable d, @Nullable int backgroundColor, @Nullable int progressColor) {
        if (d == null) {
            return;
        }
        ColorStateList backgroundCsl = getResources().getColorStateList(backgroundColor);
        ColorStateList progressCsl = getResources().getColorStateList(progressColor);
        if (backgroundCsl != null && progressCsl != null) {
            setProgressDrawable(d, backgroundCsl, progressCsl);
        }
    }

    public void setProgressDrawable(Drawable d, @Nullable ColorStateList backgroundColor, @Nullable ColorStateList progressColor) {
        if (d == null) {
            return;
        }
        final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
        final boolean isSupportTintDrawable = ThemeUtils.isSupportTintDrawable(getContext());
        int backgroundNormal = 0;
        int backgroundDisabled = 0;
        if (backgroundColor.isStateful()) {
            backgroundNormal = backgroundColor.getColorForState(ThemeUtils.EMPTY_STATE_SET,
                    backgroundColor.getDefaultColor());
            backgroundDisabled = backgroundColor.getColorForState(ThemeUtils.DISABLED_STATE_SET,
                    backgroundColor.getDefaultColor());
        } else {
            backgroundNormal = backgroundColor.getDefaultColor();
            backgroundDisabled = backgroundColor.getDefaultColor();
        }

        int progressNormal = 0;
        int progressDisabled = 0;
        if (progressColor.isStateful()) {
            progressNormal = progressColor.getColorForState(ThemeUtils.EMPTY_STATE_SET,
                    progressColor.getDefaultColor());
            progressDisabled = progressColor.getColorForState(ThemeUtils.DISABLED_STATE_SET,
                    progressColor.getDefaultColor());
        } else {
            progressNormal = progressColor.getDefaultColor();
            progressDisabled = progressColor.getDefaultColor();
        }
        if (isSupportTintDrawable) {
            LayerDrawable ld = (LayerDrawable) d;
            ld.findDrawableByLayerId(android.R.id.background).setColorFilter(backgroundNormal, DEFAULT_MODE);
            ld.findDrawableByLayerId(android.R.id.secondaryProgress).setColorFilter(backgroundNormal, DEFAULT_MODE);
            ld.findDrawableByLayerId(android.R.id.progress).setColorFilter(progressNormal, DEFAULT_MODE);
        }
        setProgressDrawable(d);
    }
}
