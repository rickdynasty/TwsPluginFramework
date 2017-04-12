package com.tencent.tws.assistant.widget;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.tws.sharelib.R;
//

public class TwsSpinnerAdapter<T> extends ArrayAdapter<T> {
	private ImageView icon;
	private TextView text;
	private LayoutInflater mInflater;

	public TwsSpinnerAdapter(Context context, ArrayList<T> objects) {
		super(context, R.layout.spinner_title_item, R.id.button_text, objects);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getItemView(ViewGroup parent) {
		View view = mInflater.inflate(R.layout.spinner_title_item, parent, false);
		icon = (ImageView) view.findViewById(R.id.icon);
		text = (TextView) view.findViewById(R.id.button_text);

		return view;
	}

	public void setIcon(int resID) {
		icon.setBackgroundResource(resID);
		icon.setVisibility(View.VISIBLE);
	}

	public void disableIcon() {
		icon.setVisibility(View.GONE);
	}

	public void setText(CharSequence spinnerTitle) {
		text.setText(spinnerTitle);
	}
}