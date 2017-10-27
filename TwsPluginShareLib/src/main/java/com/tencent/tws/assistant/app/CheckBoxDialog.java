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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.tencent.tws.sharelib.R;
import com.tencent.tws.assistant.widget.CheckBox;

/**
 * <p>A dialog showing a progress indicator and an optional text message or view.
 * Only a text message or a view can be used at the same time.</p>
 * <p>The dialog can be made cancelable on back key press.</p>
 * <p>The progress range is 0..10000.</p>
 */
public class CheckBoxDialog extends AlertDialog {
	private CharSequence mCheckBoxMsg;
	private CharSequence mMessage;
	private CheckBox mCheckBox;
	private TextView mMessageView;
	private boolean mCheckState=false;
	
    public CheckBoxDialog(Context context) {
        this(context, R.style.Theme_tws_Second_Dialog);
    }

    public CheckBoxDialog(Context context, int theme) {
        super(context, theme);
    }

    
    public static CheckBoxDialog show(Context context, CharSequence title,
            CharSequence message,CharSequence checkBoxMsg) {
        return show(context, title, message,checkBoxMsg, false);
    }

    public static CheckBoxDialog show(Context context, CharSequence title,
            CharSequence message,CharSequence checkBoxMsg,boolean hasCheck) {
        CheckBoxDialog dialog = new CheckBoxDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
		dialog.setCheckBoxMsg(checkBoxMsg);
		dialog.setCheckBoxState(hasCheck);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(R.layout.alert_dialog_checkbox, null);
		if(view!=null)
		{
			mMessageView = (TextView) view.findViewById(R.id.message);
			mCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
			//final int left=(int)getContext().getResources().getDimension(R.dimen.checkbox_dialog_padding_left);
			//final int right=(int)getContext().getResources().getDimension(R.dimen.checkbox_dialog_padding_right);
			//final int top=(int)getContext().getResources().getDimension(R.dimen.checkbox_dialog_padding_top);
			//final int bottom=(int)getContext().getResources().getDimension(R.dimen.checkbox_dialog_padding_bottom);
			setView(view);
		}
		
        if (mMessage != null) {
            setMessage(mMessage);
        }
		if(mCheckBoxMsg!= null)
		{
			setCheckBoxMsg(mCheckBoxMsg);
		}
		setCheckBoxState(mCheckState);
        super.onCreate(savedInstanceState);
    }
    
	public void setMessage(CharSequence message) {
		if(message!=null)
		{
			if(mMessageView!=null)
			{
				mMessageView.setText(message);
			}
			else
			{
				mMessage=message;
			}
		}
    }
	
    public void setCheckBoxMsg(CharSequence message) 
	{
		if(message!=null)
		{
			if(mCheckBox!=null)
			{
				mCheckBox.setText(message);
			}
			else
			{
				mCheckBoxMsg=message;
			}
		}
    }

	public void setCheckBoxState(boolean checkState) 
	{
		if(mCheckBox!=null)
		{
			mCheckBox.setChecked(checkState);
		}
		else
		{
			mCheckState=checkState;
		}
    }
	
	public boolean getCheckBoxState() 
	{
		return mCheckBox.isChecked();
    }
}
