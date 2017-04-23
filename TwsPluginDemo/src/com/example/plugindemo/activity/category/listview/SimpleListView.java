package com.example.plugindemo.activity.category.listview;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.Toast;

public class SimpleListView extends TwsListActivity {

	private String[] dataArr;

	private boolean topBlur, topPadding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setTitle("SimpleListView");
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		getListView().setScrollBarFadeDuration(0);

		dataArr = getResources().getStringArray(R.array.date);

		topBlur = getListView().setHeaderBlankWithStatusbar(false);
		getListView().setFooterBlank(false);
		topPadding = getListView().setFirstItemHigher(true);

		setListAdapter(new SimpleItemAdapter());

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// 用法1
				int pos = position - getListView().getHeaderViewsCount();
				Toast.makeText(SimpleListView.this, "data = " + dataArr[pos], Toast.LENGTH_LONG).show();
				// 用法2
				Toast.makeText(SimpleListView.this, "data = " + parent.getAdapter().getItem(position),
						Toast.LENGTH_LONG).show();
				// 用法3
				int pos1 = position - (topBlur ? 1 : 0) - (topPadding ? 1 : 0);
				Toast.makeText(SimpleListView.this, "data = " + dataArr[pos1], Toast.LENGTH_LONG).show();
			}
		});
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
				convertView = (LinearLayout) LayoutInflater.from(SimpleListView.this).inflate(
						R.layout.tws_listview_item_example, null);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.summary = (TextView) convertView.findViewById(R.id.summary);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}
			holder.title.setText(dataArr[position]);
			holder.summary.setText(dataArr[position]);
			return convertView;
		}

		class Holder {
			TextView title, summary;
		}
	}
}
