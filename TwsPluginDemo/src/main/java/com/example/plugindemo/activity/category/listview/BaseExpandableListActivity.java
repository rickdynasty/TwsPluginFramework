package com.example.plugindemo.activity.category.listview;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.TwsActivity;
import android.content.res.TypedArray;
import android.os.Bundle;

import com.example.plugindemo.R;

public class BaseExpandableListActivity extends TwsActivity {

	protected ArrayList<String> titles = new ArrayList<String>();
	protected ArrayList<String> subTitles = new ArrayList<String>();
	protected HashMap<Integer, ArrayList<String>> children = new HashMap<Integer, ArrayList<String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("BaseExpandableList");
		initData();
	}

	private void initData() {
		TypedArray groups = getResources().obtainTypedArray(R.array.groups);

		int numGroups = groups.length();
		for (int i = 0; i < numGroups; i++) {
			String[] strs = getResources().getStringArray(groups.getResourceId(i, -1));
			titles.add(strs[0]);
			subTitles.add(strs[1]);
			ArrayList<String> subChildren = new ArrayList<String>();

			for (int j = 2; j < strs.length; j++) {
				subChildren.add(strs[j]);
			}
			children.put(i, subChildren);
		}

		groups.recycle();

		titles.add("group" + String.valueOf(numGroups + 1));
		subTitles.add("");
		ArrayList<String> lastGroupChildren = new ArrayList<String>();
		for (int i = 0; i < 30; i++) {
			lastGroupChildren.add("child " + String.valueOf(i));
		}
		children.put(numGroups, lastGroupChildren);
	}
}
