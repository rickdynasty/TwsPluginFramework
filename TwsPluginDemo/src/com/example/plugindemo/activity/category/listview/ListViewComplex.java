package com.example.plugindemo.activity.category.listview;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.Toast;

public class ListViewComplex extends TwsListActivity {

	String[] dataList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("ListViewCustom");

		dataList = getResources().getStringArray(R.array.date);
		getListView().setFirstItemHigher(true);
		this.setListAdapter(new MyAdapter());

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(ListViewComplex.this, parent.getAdapter().getItem(position) + "item is clicked",
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Toast.makeText(ListViewComplex.this, l.getAdapter().getItem(position) + "item is clicked", Toast.LENGTH_SHORT)
				.show();
	}

	class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return dataList.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return dataList[arg0];
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {

			Holder holder;
			if (null == convertView) {
				holder = new Holder();
				convertView = (LinearLayout) LayoutInflater.from(ListViewComplex.this).inflate(R.layout.item_listview,
						null);
				holder.textView = (TextView) convertView.findViewById(R.id.textView1);
				holder.button = (Button) convertView.findViewById(R.id.button1);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}
			holder.textView.setText(dataList[arg0]);
			holder.button.setText("click me");
			holder.button.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Toast.makeText(ListViewComplex.this, "Button is clicked", Toast.LENGTH_SHORT).show();
				}
			});
			return convertView;
		}

		class Holder {
			public TextView textView;
			public Button button;

		}

	}

}