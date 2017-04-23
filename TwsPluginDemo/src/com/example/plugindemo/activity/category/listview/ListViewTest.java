package com.example.plugindemo.activity.category.listview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SimpleAdapter;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.ListView;

public class ListViewTest extends TwsActivity implements OnClickListener {

	Button btAdd;
	Button btSub;
	ListView listview;
	List<Map<String, Object>> list;
	SimpleAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pagelistview);
		setTitle("ListViewTest");

		btAdd = (Button) findViewById(R.id.btAdd);
		btSub = (Button) findViewById(R.id.btSub);
		listview = (ListView) findViewById(R.id.listview);
		btAdd.setText("add_item");
		btSub.setText("remove_item");

		btAdd.setOnClickListener(this);
		btSub.setOnClickListener(this);

		list = new ArrayList<Map<String, Object>>();
		adapter = new SimpleAdapter(this, list, R.layout.listviewtestitem, new String[] { "title", "img" }, new int[] {
				R.id.item1title, R.id.item1img });
		listview.setAdapter(adapter);
	}

	@Override
	public void onClick(View v) {
		if (v == btAdd) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("title", "item" + list.size());
			map.put("img", R.drawable.ic_launcher);
			list.add(map);
			adapter.notifyDataSetChanged();
		} else {
			if (list.size() >= 1)
				list.remove(list.size() - 1);
			adapter.notifyDataSetChanged();
		}

	}

}
