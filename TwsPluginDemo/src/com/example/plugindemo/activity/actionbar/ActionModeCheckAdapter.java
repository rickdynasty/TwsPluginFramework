package com.example.plugindemo.activity.actionbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.example.plugindemo.R;

public class ActionModeCheckAdapter extends ArrayAdapter<String> {

	public interface Callback {
		public void onAdapterCheckedListener();

		public void onAdapterUnCheckedListener();
	}

	private Set<String> mSelected = new HashSet<String>();
	private Callback mCallback;
	private ArrayList<String> mList;
	private boolean isSelectAll = false;

	private static class ViewHolder {
		public CheckBox mCheckBox;
		public TextView mTextView;
	}

	public ActionModeCheckAdapter(Context context, int resource, int textViewResourceId, List<String> objects,
			Callback callback) {
		super(context, resource, textViewResourceId, objects);
		// TODO Auto-generated constructor stub
		mCallback = callback;
		mList = (ArrayList<String>) objects;
	}

	public boolean hasChecked() {
		return mSelected.isEmpty();
	}

	public void removeSelected() {

		if (isSelectAll) {
			mList.removeAll(mList);
		} else if (!mSelected.isEmpty()) {
			for (String string : mSelected) {
				mList.remove(string);
			}
		}
		mSelected.clear();
		notifyDataSetChanged();
	}

	public void selectAll() {
		isSelectAll = true;
		notifyDataSetChanged();
		mSelected.clear();
	}

	public void unselectAll() {
		if (isSelectAll) {
			isSelectAll = false;
			notifyDataSetChanged();
			mSelected.clear();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View view = super.getView(position, convertView, parent);
		ViewHolder holder = null;
		Object tag = view.getTag();
		if (tag == null) {
			holder = new ViewHolder();
			holder.mCheckBox = (CheckBox) view.findViewById(R.id.check_box);
			holder.mTextView = (TextView) view.findViewById(R.id.text);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) tag;
		}
		holder.mCheckBox.setTag(holder.mTextView.getText());
		holder.mCheckBox.setOnCheckedChangeListener(mListener);
		if (isSelectAll) {
			holder.mCheckBox.setChecked(true);
		} else {
			holder.mCheckBox.setChecked(false);
		}
		return view;
	}

	private OnCheckedChangeListener mListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			// TODO Auto-generated method stub
			String text = (String) buttonView.getTag();
			if (isChecked) {
				mSelected.add(text);
				if (mCallback != null) {
					mCallback.onAdapterCheckedListener();
				}
			} else {
				mSelected.remove(text);
				if (mCallback != null) {
					mCallback.onAdapterUnCheckedListener();
				}
				if (isSelectAll) {
					isSelectAll = false;
				}
			}
		}
	};

}
