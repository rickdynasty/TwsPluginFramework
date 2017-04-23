package com.example.plugindemo.activity.category.listview.sortlistview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwsSearchView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.AbsListView.OnScrollListener;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.assistant.widget.SideBar;
import com.tencent.tws.assistant.widget.SideBar.OnTouchingLetterChangedListener;

public class SortListView extends RelativeLayout implements SortInterface {

	protected TwsSearchView qsvSearch;
	protected ListView mListView;
	protected SideBar mSidebar;
	protected TextView tvAirBox;
	protected TextView tvTopCategory;
	protected TextView tvNotFound;

	protected ListViewAdapter adapter;

	protected String[] categorySet;
	protected ArrayList<BaseData> dataSet;
	Comparator<BaseData> orderKeyComeComparator;

	public SortListView(Context context) {
		super(context);
		initData();
		initView();
	}

	public void initData() {
		categorySet = giveCategorySet();
		dataSet = giveBaseData();
		setDataSetOrderKey(dataSet);
		setDataSetCategory(dataSet);
		orderKeyComeComparator = giveOderKeyComparator();
		Collections.sort(dataSet, orderKeyComeComparator);
		Collections.sort(dataSet, new CategoryComparator());
	}

	public void initView() {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.activity_add_friends, this);
		mListView = (ListView) findViewById(R.id.country_lvcountry);
		tvTopCategory = (TextView) findViewById(R.id.title_layout_catalog);
		mSidebar = (SideBar) findViewById(R.id.sidrbar);
		tvAirBox = (TextView) findViewById(R.id.dialog);
		tvNotFound = (TextView) this.findViewById(R.id.title_layout_no_friends);
		qsvSearch = (TwsSearchView) findViewById(R.id.filter_edit);

		mSidebar.setSideBarEntries(categorySet);
		mSidebar.invalidate();
		adapter = new ListViewAdapter(getContext(), dataSet);
		mListView.setAdapter(adapter);
		mListView.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(com.tencent.tws.assistant.widget.AbsListView arg0, int arg1, int arg2, int arg3) {
				tvTopCategory.setText(dataSet.get(arg1).category);
			}

			@Override
			public void onScrollStateChanged(com.tencent.tws.assistant.widget.AbsListView arg0, int arg1) {
			}
		});
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(com.tencent.tws.assistant.widget.AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Toast.makeText(getContext(), ((BaseData) adapter.getItem(arg2)).text, Toast.LENGTH_SHORT).show();

			}
		});
		mSidebar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
			@Override
			public void onTouchingLetterChanged(String s) {
				int position = adapter.getPositionForSection(s);
				if (position != -1) {
					mListView.setSelection(position);
				}
			}

			@Override
			public void onTouchUp() {

			}

			@Override
			public void onTouchingLetterChanged(int letterIndex) {

			}
		});

	}

	private void filterData(String filterStr) {
		List<BaseData> filterDateList = new ArrayList<BaseData>();

		CharacterParser characterParser = CharacterParser.getInstance();
		if (TextUtils.isEmpty(filterStr)) {
			filterDateList = dataSet;
			tvNotFound.setVisibility(View.GONE);
		} else {
			filterDateList.clear();
			for (BaseData sortModel : dataSet) {
				String name = sortModel.text;
				if (name.indexOf(filterStr.toString()) != -1
						|| characterParser.getSelling(name).startsWith(filterStr.toString())) {
					filterDateList.add(sortModel);
				}
			}
		}
		adapter.updateListView(filterDateList);
		if (filterDateList.size() == 0) {
			tvNotFound.setVisibility(View.VISIBLE);
		}
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
		return new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
				"S", "T", "U", "V", "W", "X", "Y", "Z", "#" };
	}

	@Override
	public void setDataSetOrderKey(ArrayList<BaseData> dataSet) {
		for (int i = 0; i < dataSet.size(); i++) {
			dataSet.get(i).orderKey = dataSet.get(i).text;
		}
	}

	@Override
	public void setDataSetCategory(ArrayList<BaseData> dataSet) {
		CharacterParser characterParser = CharacterParser.getInstance();
		for (int i = 0; i < dataSet.size(); i++) {
			String pinyin = characterParser.getSelling(dataSet.get(i).text);
			String sortString = pinyin.substring(0, 1).toUpperCase();
			if (sortString.matches("[A-Z]")) {
				dataSet.get(i).category = sortString.toUpperCase();
			} else {
				dataSet.get(i).category = "#";
			}
		}
	}

	@Override
	public Comparator<BaseData> giveOderKeyComparator() {
		// TODO Auto-generated method stub
		return new OrderKeyComparator();
	}

	class CategoryComparator implements Comparator<BaseData> {

		public int compare(BaseData o1, BaseData o2) {

			int o1category = 0, o2category = 0;
			for (int i = 0; i < categorySet.length; i++) {
				if (o1.category.equals(categorySet[i]))
					o1category = i;
				if (o2.category.equals(categorySet[i]))
					o2category = i;
			}

			if (o1category < o2category)
				return -1;
			else if (o1category > o2category)
				return 1;
			else
				return 0;
		}
	}

	class OrderKeyComparator implements Comparator<BaseData> {

		public int compare(BaseData o1, BaseData o2) {

			return o1.orderKey.compareTo(o2.orderKey);
		}
	}

	public SortListView(Context context, ListView mListView, TextView tvAirBox, TextView tvTopCategory,
			TextView tvNotFound, ListViewAdapter adapter) {
		super(context);
		this.mListView = mListView;
		this.tvAirBox = tvAirBox;
		this.tvTopCategory = tvTopCategory;
		this.tvNotFound = tvNotFound;
		this.adapter = adapter;
	}

}
