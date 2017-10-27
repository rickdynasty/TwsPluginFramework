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

package com.tencent.tws.assistant.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

class TextHelper {

    static TextHelper create(TextView textView) {
        if (Build.VERSION.SDK_INT >= 17) {
            return new TextHelperV17(textView);
        }
        return new TextHelper(textView);
    }

    private static final int[] VIEW_ATTRS = {android.R.attr.textAppearance,
            android.R.attr.drawableLeft, android.R.attr.drawableTop,
            android.R.attr.drawableRight, android.R.attr.drawableBottom };

    final TextView mView;

    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableTopTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableBottomTint;

    TextHelper(TextView view) {
        mView = view;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        final Context context = mView.getContext();
        final TintManager tintManager = TintManager.get(context);

        // First read the TextAppearance style id
        TypedArray a = context.obtainStyledAttributes(attrs, VIEW_ATTRS, defStyleAttr, 0);
        final int ap = a.getResourceId(0, -1);

        // Now read the compound drawable and grab any tints
        if (a.hasValue(1)) {
            mDrawableLeftTint = new TintInfo();
            mDrawableLeftTint.mHasTintList = true;
            mDrawableLeftTint.mTintList = tintManager.getTintList(a.getResourceId(1, 0));
        }
        if (a.hasValue(2)) {
            mDrawableTopTint = new TintInfo();
            mDrawableTopTint.mHasTintList = true;
            mDrawableTopTint.mTintList = tintManager.getTintList(a.getResourceId(2, 0));
        }
        if (a.hasValue(3)) {
            mDrawableRightTint = new TintInfo();
            mDrawableRightTint.mHasTintList = true;
            mDrawableRightTint.mTintList = tintManager.getTintList(a.getResourceId(3, 0));
        }
        if (a.hasValue(4)) {
            mDrawableBottomTint = new TintInfo();
            mDrawableBottomTint.mHasTintList = true;
            mDrawableBottomTint.mTintList = tintManager.getTintList(a.getResourceId(4, 0));
        }
        a.recycle();
    }

    void applyCompoundDrawablesTints() {
        if (mDrawableLeftTint != null || mDrawableTopTint != null ||
                mDrawableRightTint != null || mDrawableBottomTint != null) {
            final Drawable[] compoundDrawables = mView.getCompoundDrawables();
            applyCompoundDrawableTint(compoundDrawables[0], mDrawableLeftTint);
            applyCompoundDrawableTint(compoundDrawables[1], mDrawableTopTint);
            applyCompoundDrawableTint(compoundDrawables[2], mDrawableRightTint);
            applyCompoundDrawableTint(compoundDrawables[3], mDrawableBottomTint);
        }
    }

    final void applyCompoundDrawableTint(Drawable drawable, TintInfo info) {
        if (drawable != null && info != null) {
            TintManager.tintDrawable(drawable, info, mView.getDrawableState());
        }
    }
}
