/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.tencent.tws.assistant.preference;

import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.util.Log;


/**
 * A {@link Preference} that provides checkbox widget
 * functionality.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 * 
 * @attr ref android.R.styleable#CheckBoxPreference_summaryOff
 * @attr ref android.R.styleable#CheckBoxPreference_summaryOn
 * @attr ref android.R.styleable#CheckBoxPreference_disableDependentsState
 */
public class CheckBoxPreference extends TwoStatePreference {

    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CheckBoxPreference, defStyle, 0);
        setSummaryOn(a.getString(android.R.styleable.CheckBoxPreference_summaryOn));
        setSummaryOff(a.getString(android.R.styleable.CheckBoxPreference_summaryOff));
        setDisableDependentsState(a.getBoolean(
                R.styleable.CheckBoxPreference_disableDependentsState, false));
        a.recycle();
    }

    public CheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkBoxPreferenceStyle);
    }

    public CheckBoxPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View checkboxView = view.findViewById(R.id.checkbox);
        if (checkboxView != null && checkboxView instanceof Checkable) {
            ((Checkable) checkboxView).setChecked(mChecked);
            sendAccessibilityEvent(checkboxView);
        }

        syncSummaryView(view);
    }
}
