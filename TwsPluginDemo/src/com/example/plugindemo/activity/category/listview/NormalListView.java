package com.example.plugindemo.activity.category.listview;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.plugindemo.R;

public class NormalListView extends TwsListActivity {

	private String[] dataArr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setTitle("NormalListView");
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		dataArr = getResources().getStringArray(R.array.date);

		getListView().setHeaderBlankWithStatusbar(false);
		getListView().setFooterBlank(false);

		setListAdapter(new SimpleItemAdapter());

		getListView().setFirstItemHigher(false);
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
				convertView = (LinearLayout) LayoutInflater.from(NormalListView.this).inflate(
						R.layout.tws_listview_item_normal, null);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.summary = (TextView) convertView.findViewById(R.id.summary);
				holder.caption = (TextView) convertView.findViewById(R.id.caption);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.rightIcon = (ImageView) convertView.findViewById(R.id.rightIcon);
				holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}
			holder.title.setText(dataArr[position]);
			holder.summary.setText(dataArr[position]);
			holder.caption.setText(dataArr[position]);
			holder.icon.setVisibility(View.VISIBLE);
			holder.rightIcon.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.VISIBLE);
			holder.icon.setImageResource(R.drawable.ic_search);
			holder.rightIcon.setImageResource(R.drawable.search_result_clear_normal);
			return convertView;
		}

		class Holder {
			TextView title, summary, caption;
			ImageView icon, rightIcon;
			ProgressBar progressBar;
		}
	}
}
