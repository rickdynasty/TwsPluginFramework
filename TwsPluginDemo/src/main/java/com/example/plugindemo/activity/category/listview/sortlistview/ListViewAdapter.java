package com.example.plugindemo.activity.category.listview.sortlistview;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.plugindemo.R;

public class ListViewAdapter extends BaseAdapter {
	private List<BaseData> list = null;
	private Context mContext;

	public ListViewAdapter(Context mContext, List<BaseData> list) {
		this.mContext = mContext;
		this.list = list;
	}

	public void updateListView(List<BaseData> list) {
		this.list = list;
		notifyDataSetChanged();
	}

	public int getCount() {
		return this.list.size();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View view, ViewGroup arg2) {
		ViewHolder viewHolder = null;
		final BaseData mContent = list.get(position);
		if (view == null) {
			viewHolder = new ViewHolder();
			view = LayoutInflater.from(mContext).inflate(R.layout.activity_group_member_item, null);
			viewHolder.tvTitle = (TextView) view.findViewById(R.id.title);
			viewHolder.tvCategory = (TextView) view.findViewById(R.id.catalog);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		boolean isFirst = false;
		for (int i = 0; i <= position; i++) {
			if (list.get(i).category.equals(list.get(position).category)) {
				if (i == position) {
					viewHolder.tvCategory.setVisibility(View.VISIBLE);
					viewHolder.tvCategory.setText(list.get(position).category);
					isFirst = true;
					break;
				} else
					break;
			}
		}

		if (isFirst == false)
			viewHolder.tvCategory.setVisibility(View.GONE);

		viewHolder.tvTitle.setText(list.get(position).text);

		return view;

	}

	final static class ViewHolder {
		TextView tvCategory;
		TextView tvTitle;
	}

	public int getPositionForSection(String s) {
		for (int i = 0; i < getCount(); i++) {
			String sortStr = list.get(i).category;
			if (sortStr.equals(s)) {
				return i;
			}
		}

		return -1;
	}

}