package com.example.plugindemo.activity.category.picker;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.TwsActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.AlertDialog;
import com.tencent.tws.assistant.widget.DateTimePicker;
import com.tencent.tws.assistant.widget.DateTimePicker.OnDateTimeChangedListener;

public class PickerDialogOutside extends TwsActivity implements OnClickListener, OnDateTimeChangedListener {

    TextView mStartTextView;
    TextView mEndTextView;
    EditText mEditText;
    AlertDialog mDialog;
    DateTimePicker mPickerView;
    Context mContext;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    long mCurrentTime;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
		
        mContext = this;
        setContentView(R.layout.act_picker_outside);
		setTitle("PickerDialogOutside");
		
        mStartTextView = (TextView) findViewById(R.id.tv_start);
        mEndTextView = (TextView) findViewById(R.id.tv_end);
        mEditText = (EditText) findViewById(R.id.et_thing);
        mCurrentTime = System.currentTimeMillis();
        mStartTextView.setText(mFormat.format(new Date(mCurrentTime)));
        mEndTextView.setText(mFormat.format(new Date(mCurrentTime + 600000)));
        mStartTextView.setOnClickListener(this);
        mEndTextView.setOnClickListener(this);
        mEditText.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_start:
                mStartTextView.setSelected(true);
                mEndTextView.setSelected(false);
                mStartTextView.setText(mFormat.format(new Date(mCurrentTime)));
                hideSoftKeyboard(mEditText);
                showDateDialog(mCurrentTime);
                break;
            case R.id.tv_end:
                mStartTextView.setSelected(false);
                mEndTextView.setSelected(true);
                hideSoftKeyboard(mEditText);
                mEndTextView.setText(mFormat.format(new Date(mCurrentTime + 24 * 60 * 60 * 1000)));
                showDateDialog(mCurrentTime + 24 * 60 * 60 * 1000);
                break;
            case R.id.et_thing:
                mStartTextView.setSelected(false);
                mEndTextView.setSelected(false);
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                break;

            default:
                break;
        }
    }

    private void showDateDialog(long time) {
        if (mPickerView == null) {
            mPickerView = new DateTimePicker(mContext);
            mPickerView.init(time, this);
        }
        if (mDialog == null) {
            mDialog = new AlertDialog(mContext, true);
            mDialog.setView(mPickerView, 0, 0, 0, 0);
            mDialog.setDialogDimAmount(0);
            mDialog.setCancelable(false);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        if (mDialog.isShowing()) {
            mPickerView.updateDateTime(time);
        } else {
            mDialog.show();
        }
    }

    @Override
    public void onDateTimeChanged(DateTimePicker picker, long time) {
        if (mStartTextView.isSelected()) {
            mCurrentTime = time;
            mStartTextView.setText(mFormat.format(new Date(time)));
        } else if (mEndTextView.isSelected()) {
            mCurrentTime = time;
            mEndTextView.setText(mFormat.format(new Date(time)));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mDialog != null && mDialog.isShowing()) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                mStartTextView.setSelected(false);
                mEndTextView.setSelected(false);
                mDialog.dismiss();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void hideSoftKeyboard(View view) {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
