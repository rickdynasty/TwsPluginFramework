package com.example.plugindemo.activity.category.listview.sortlistview;

import java.util.ArrayList;
import java.util.Comparator;

public interface SortInterface {

	public String[] giveCategorySet();

	public ArrayList<BaseData> giveBaseData();

	public void setDataSetOrderKey(ArrayList<BaseData> dataSet);

	public void setDataSetCategory(ArrayList<BaseData> dataSet);

	public Comparator<BaseData> giveOderKeyComparator();
}
