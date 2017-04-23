package com.example.plugindemo.activity.category.listview.sortlistview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import android.content.Context;

import com.example.plugindemo.R;

public class MySortListView extends SortListView {

	public MySortListView(Context context) {
		super(context);
	}

	@Override
	public ArrayList<BaseData> giveBaseData() {
		ArrayList<BaseData> dataList = new ArrayList<BaseData>();
		String[] data = getResources().getStringArray(R.array.date);
		for (int i = 0; i < data.length; i++) {

			dataList.add(new BaseData(data[i]));
		}
		return dataList;
	}

	@Override
	public String[] giveCategorySet() {
		// TODO Auto-generated method stub
		return new String[] { "1", "2", "3", "4", "5" };
	}

	@Override
	public void setDataSetOrderKey(ArrayList<BaseData> dataSet) {
		// TODO Auto-generated method stub
		super.setDataSetOrderKey(dataSet);
	}

	@Override
	public void setDataSetCategory(ArrayList<BaseData> dataSet) {
		for (int i = 0; i < dataSet.size(); i++) {
			dataSet.get(i).category = categorySet[new Random().nextInt(5)];
		}
	}

	@Override
	public Comparator<BaseData> giveOderKeyComparator() {
		// TODO Auto-generated method stub
		return super.giveOderKeyComparator();
	}

}
