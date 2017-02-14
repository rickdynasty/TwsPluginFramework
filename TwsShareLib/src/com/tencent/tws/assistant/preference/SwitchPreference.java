/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.TypedArray;


import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import com.tencent.tws.assistant.preference.PreferenceManager;
import com.tencent.tws.assistant.preference.PreferenceScreen;
import com.tencent.tws.assistant.preference.Preference.OnPreferenceClickListener;
import com.tencent.tws.assistant.utils.TwsRippleEffectInterface;
import com.tencent.tws.assistant.widget.Switch;
import com.tencent.tws.sharelib.R;

/**
 * A {@link Preference} that provides a two-state toggleable option.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 *
 * @attr ref android.R.styleable#SwitchPreference_summaryOff
 * @attr ref android.R.styleable#SwitchPreference_summaryOn
 * @attr ref android.R.styleable#SwitchPreference_switchTextOff
 * @attr ref android.R.styleable#SwitchPreference_switchTextOn
 * @attr ref android.R.styleable#SwitchPreference_disableDependentsState
 */
public class SwitchPreference extends TwoStatePreference {
    private final Listener mListener = new Listener();

    // Switch text for on and off states
    private CharSequence mSwitchOn;
    private CharSequence mSwitchOff;
    private OnPreferenceClickListener mOnClickListener;
//    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener;
    private OnPreferenceSwitchClickListener mSwitchClickListener;
    private String mSwitchTag;

    public void setOnPreferenceSwitchClickListener(OnPreferenceSwitchClickListener switchClickListener) {
        mSwitchClickListener = switchClickListener;
    }

    @Deprecated
    public void setOnPreferenceSwitchClickListener(CompoundButton.OnCheckedChangeListener listener) {
    }

//    @Override
//    protected void onClick() {
//        if (mCheckedChangeListener == null) {
//            super.onClick();
//        }
//    }
    @Override
    protected void onWidgetFrameClick() {
        super.onWidgetFrameClick();
        if (mSwitchClickListener != null) {
            mSwitchClickListener.onPreferenceSwitchClick(SwitchPreference.this);
        }
    }

    public void setSwitchTag(String tag) {
        mSwitchTag = tag;
    }

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }

            SwitchPreference.this.setChecked(isChecked);

            mOnClickListener = getOnPreferenceClickListener();
            if (mOnClickListener != null && mOnClickListener.onPreferenceClick(SwitchPreference.this)) {
                return;
            }

            PreferenceManager preferenceManager = getPreferenceManager();
            if (preferenceManager != null) {
                PreferenceManager.OnPreferenceTreeClickListener listener = preferenceManager.getOnPreferenceTreeClickListener();
                PreferenceScreen preferenceScreen = null;
                if (listener != null && listener.onPreferenceTreeClick(preferenceScreen, SwitchPreference.this)) {
                    return;
                }
            }
        }
    }

    /**
     * Construct a new SwitchPreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     * @param defStyle Theme attribute defining the default style options
     */
    public SwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                android.R.styleable.SwitchPreference, defStyle, 0);
        setSummaryOn(a.getString(android.R.styleable.SwitchPreference_summaryOn));
        setSummaryOff(a.getString(android.R.styleable.SwitchPreference_summaryOff));
        setSwitchTextOn(a.getString(
                android.R.styleable.SwitchPreference_switchTextOn));
        setSwitchTextOff(a.getString(
                android.R.styleable.SwitchPreference_switchTextOff));
        setDisableDependentsState(a.getBoolean(
                android.R.styleable.SwitchPreference_disableDependentsState, false));
        a.recycle();
    }

    /**
     * Construct a new SwitchPreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     */
    public SwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.switchPreferenceStyle);
    }

    /**
     * Construct a new SwitchPreference with default style options.
     *
     * @param context The Context that will style this preference
     */
    public SwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View checkableView = view.findViewById(R.id.switchWidget);
        if (checkableView != null && checkableView instanceof Checkable) {
            if (checkableView instanceof Switch) {
                final Switch switchView = (Switch) checkableView;
                switchView.setOnCheckedChangeListener(null);
            }

            ((Checkable) checkableView).setChecked(mChecked);

            if (checkableView instanceof Switch) {
                final Switch switchView = (Switch) checkableView;
//                switchView.setTextOn(mSwitchOn);
//                switchView.setTextOff(mSwitchOff);
                if (mSwitchClickListener == null) {
                    switchView.setOnCheckedChangeListener(mListener);
                }
            }
        }

        syncSummaryView(view);
    }

    /**
     * Set the text displayed on the switch widget in the on state.
     * This should be a very short string; one word if possible.
     *
     * @param onText Text to display in the on state
     */
    public void setSwitchTextOn(CharSequence onText) {
        mSwitchOn = onText;
        notifyChanged();
    }

    /**
     * Set the text displayed on the switch widget in the off state.
     * This should be a very short string; one word if possible.
     *
     * @param offText Text to display in the off state
     */
    public void setSwitchTextOff(CharSequence offText) {
        mSwitchOff = offText;
        notifyChanged();
    }

    /**
     * Set the text displayed on the switch widget in the on state.
     * This should be a very short string; one word if possible.
     *
     * @param resId The text as a string resource ID
     */
    public void setSwitchTextOn(int resId) {
        setSwitchTextOn(getContext().getString(resId));
    }

    /**
     * Set the text displayed on the switch widget in the off state.
     * This should be a very short string; one word if possible.
     *
     * @param resId The text as a string resource ID
     */
    public void setSwitchTextOff(int resId) {
        setSwitchTextOff(getContext().getString(resId));
    }

    /**
     * @return The text that will be displayed on the switch widget in the on state
     */
    public CharSequence getSwitchTextOn() {
        return mSwitchOn;
    }

    /**
     * @return The text that will be displayed on the switch widget in the off state
     */
    public CharSequence getSwitchTextOff() {
        return mSwitchOff;
    }

    public interface OnPreferenceSwitchClickListener {
        boolean onPreferenceSwitchClick(Preference preference);
    }

    @Override
    protected boolean isSupportWidgetFrameClick() {
        return true;
    }
}
