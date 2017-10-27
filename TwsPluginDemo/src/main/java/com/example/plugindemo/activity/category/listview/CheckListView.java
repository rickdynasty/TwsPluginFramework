package com.example.plugindemo.activity.category.listview;

import java.util.HashMap;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.CheckBox;

public class CheckListView extends TwsListActivity implements OnItemClickListener {

	private String[] dataArr;
	private HashMap<String, Boolean> mRadiostates;
	private SimpleItemAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setTitle("CheckListView");

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		dataArr = getResources().getStringArray(R.array.date);
		mAdapter = new SimpleItemAdapter();
		mRadiostates = new HashMap<String, Boolean>(mAdapter.getCount());
		for (int i = 0; i < mAdapter.getCount(); i++) {
			mRadiostates.put(i + "", false);
		}
		getListView().setHeaderBlankWithStatusbar(false);
		getListView().setFooterBlank(false);
		setListAdapter(mAdapter);
		getListView().setOnItemClickListener(this);

		// getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private class SimpleItemAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return dataArr.length;
		}

		@Override
		public Object getItem(int position) {
			return dataArr[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			if (convertView == null) {
				holder = new Holder();
				convertView = (LinearLayout) LayoutInflater.from(CheckListView.this).inflate(
						R.layout.tws_listview_item_check, null);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.summary = (TextView) convertView.findViewById(R.id.summary);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
				holder.checkBox.setFocusable(false);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}
			holder.title.setText(dataArr[position]);
			holder.summary.setText(dataArr[position]);
			holder.icon.setImageResource(R.drawable.ic_search);
			holder.checkBox.setFocusable(false);

			boolean res = false;
			if (mRadiostates.get(String.valueOf(position)) == false) {
				res = false;
				mRadiostates.put(String.valueOf(position), false);
			} else {
				res = true;
			}
			holder.checkBox.setChecked(res);
			return convertView;
		}

		class Holder {
			TextView title, summary;
			ImageView icon;
			CheckBox checkBox;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
		position = position - getListView().getHeaderViewsCount();
		// 重置，确保最多只有一项被选中
		for (String key : mRadiostates.keySet()) {
			mRadiostates.put(key, false);
		}
		mRadiostates.put(String.valueOf(position), true);
		mAdapter.notifyDataSetChanged();
	}

}
