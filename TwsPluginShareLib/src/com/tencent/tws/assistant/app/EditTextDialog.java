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

package com.tencent.tws.assistant.app;

import com.tencent.tws.assistant.app.AlertController;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.app.AlertDialog.ButtonColor;
import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.view.ContextThemeWrapper;
import android.text.InputType;

/**
 * <p>
 * A dialog showing a progress indicator and an optional text message or view. Only a text message or a view
 * can be used at the same time.
 * </p>
 * <p>
 * The dialog can be made cancelable on back key press.
 * </p>
 * <p>
 * The progress range is 0..10000.
 * </p>
 */
public class EditTextDialog extends AlertDialog {
    private TextView mMessageView;
    private EditText mEditText;
    private CharSequence mMessage;
    private CharSequence mEditString;
    private int mInputType = InputType.TYPE_CLASS_TEXT;
    private boolean mHasStarted;

    public EditTextDialog(Context context) {
        this(context, R.style.Theme_tws_Second_Dialog);
    }

    public EditTextDialog(Context context, int theme) {
        super(context, theme);
    }

    public static EditTextDialog show(Context context, CharSequence title, CharSequence message) {
        return show(context, title, message, InputType.TYPE_CLASS_TEXT);
    }

    public static EditTextDialog show(Context context, CharSequence title, CharSequence message, int inputType) {
        EditTextDialog dialog = new EditTextDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setInputType(inputType);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.alert_dialog_edittext, null);
        if (view != null) {
            mMessageView = (TextView) view.findViewById(R.id.message);
            mEditText = (EditText) view.findViewById(R.id.edittext);
            setView(view);
        }

        if (mMessage != null) {
            setMessage(mMessage);
        }
        if (mEditString != null) {
            setEditTextText(mEditString);
        }
        setInputType(mInputType);
        super.onCreate(savedInstanceState);
    }

    public void setMessage(CharSequence message) {
        if (message != null) {
            if (mMessageView != null) {
                mMessageView.setVisibility(View.VISIBLE);
                mMessageView.setText(message);
            } else {
                mMessage = message;
            }
        }
    }

    public void setInputType(int inputType) {
        if (mEditText != null) {
            mEditText.setInputType(inputType);
        } else {
            mInputType = inputType;
        }
    }

    public EditText getEditText() {
        return mEditText;
    }

    public void setEditTextText(CharSequence editString) {
        if (mEditText != null) {
            mEditText.setText(editString);
            int pos = mEditText.getText().length();
            if (pos >= 0) {
                try {
                    mEditText.setSelection(pos);
                } catch (IndexOutOfBoundsException e) {

                }
            }
        } else {
            mEditString = editString;
        }
    }
}
