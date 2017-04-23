package com.example.plugindemo.activity.category.listview.sortlistview;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;

public class SortListviewActivity extends TwsActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("SortListview");

		SortListView sortListView = new SortListView(this);
		addContentView(sortListView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

}
