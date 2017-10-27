package com.example.plugindemo.activity.category.listview;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.ExpandableListView;
import com.tencent.tws.assistant.widget.TwsExpandableListAdapter;

public class CustomExpandableListActivity extends BaseExpandableListActivity implements
		ExpandableListView.OnChildClickListener {

	private MyAdapter adapter;

	class MyAdapter extends TwsExpandableListAdapter {

		private Context context;

		public MyAdapter(Context context, ExpandableListView listView) {
			super(context, listView);
			// TODO Auto-generated constructor stub
			this.context = context;
		}

		public void removeItem(int groupId, int childId) {
			children.get(groupId).remove(childId);
			setChildTitles(children);
			notifyDataSetChanged();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return children.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			int cnt = 0;
			for (int i = 0; i < groupPosition; i++) {
				cnt += children.get(groupPosition).size();
			}
			return cnt + childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(R.layout.expandablelistview_child, null);
			}

			TextView title = (TextView) convertView.findViewById(R.id.title);
			title.setText(children.get(groupPosition).get(childPosition));
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			// TODO Auto-generated method stub
			return children.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			// TODO Auto-generated method stub
			return children.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			// TODO Auto-generated method stub
			return children.keySet().size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(R.layout.expandablelistview_group, null);
			}

			TextView title = (TextView) convertView.findViewById(R.id.title);
			TextView subTitle = (TextView) convertView.findViewById(R.id.subTitle);
			title.setText(titles.get(groupPosition));
			subTitle.setText(subTitles.get(groupPosition));

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			if (groupPosition == 0) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public void onGroupExpanded(int arg0) {
			// TODO Auto-generated method stub
			super.onGroupExpanded(arg0);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.expandablelistview);
		setTitle("CustomExpandableList");

		ExpandableListView view = (ExpandableListView) findViewById(R.id.listView);

		adapter = new MyAdapter(this, view);
		view.setAdapter(adapter);
		view.setOnChildClickListener(this);
	}

	@Override
	public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4) {
		// TODO Auto-generated method stub
		adapter.removeItem(arg2, arg3);
		return true;
	}
}
