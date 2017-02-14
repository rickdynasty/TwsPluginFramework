package com.tencent.tws.pluginhost.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.pluginhost.R;

public class ToastFragment extends Fragment {
	private TextView mToastTextView;
	private CharSequence mTtext = null;

	@SuppressLint("ResourceAsColor")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_toast, container, false);

		mToastTextView = (TextView) rootView.findViewById(R.id.show_messsage_tv);
		if (mToastTextView != null) {
			mToastTextView.setText(mTtext);
		}

		return rootView;
	}

	public void setToastMsg(CharSequence text) {
		mTtext = text;
		if (mToastTextView != null) {
			mToastTextView.setText(mTtext);
		}
	}
}
