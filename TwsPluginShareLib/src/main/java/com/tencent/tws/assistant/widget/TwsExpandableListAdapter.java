package com.tencent.tws.assistant.widget;

import java.util.ArrayList;
import java.util.HashMap;

import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class TwsExpandableListAdapter  extends BaseExpandableListAdapter {
	private LayoutInflater inflater;
	private ExpandableListView listView;
	private Context context;
	
	private Drawable parentIndicateUp, parentIndicateDown, parentDivider, childIndicate;
	private int parentTitleColor, parentSubtitleColor, childTitleColor;
	private boolean childIndicateVisibility = true;
	
	private ArrayList<String> parentTitles, parentSubTitles;
	private HashMap<Integer, ArrayList<String>> childTitles, childInfos;
	
	public TwsExpandableListAdapter(Context context, ExpandableListView listView) {
		this.context = context;
		this.listView = listView;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		if (childTitles != null)
			return childTitles.get(groupPosition).get(childPosition);
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		if (childTitles != null)
			return childTitles.get(groupPosition).size();
		return 0;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		ChildHolder holder = null;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.tws_child_item, null);
			holder = new ChildHolder();
			holder.title = (TextView) convertView.findViewById(R.id.child_title);
			holder.indicate = (ImageView) convertView.findViewById(R.id.child_indicate);
			holder.info = (TextView) convertView.findViewById(R.id.child_info);
			convertView.setTag(holder);
		}
		else {
			holder = (ChildHolder)convertView.getTag();
		}
		
		holder.title.setVisibility(View.VISIBLE);
		holder.indicate.setVisibility(View.VISIBLE);
		holder.info.setVisibility(View.VISIBLE);
		
		holder.setTitle(childTitles.get(groupPosition).get(childPosition));
		holder.setIndicate((childIndicate == null) ? context.getResources().getDrawable(R.drawable.tws_expand_child_point) : childIndicate);
		holder.setInfo(childInfos.get(groupPosition).get(childPosition));
		
		int titleColor = R.color.tws_text_light_body;
		holder.setTitleColor((childTitleColor == 0) ? titleColor : childTitleColor);

		holder.indicate.setVisibility(childIndicateVisibility ? View.VISIBLE : View.GONE);
		
		boolean hasTitle = (holder.title.getText().toString().equals("") || holder.title == null) ? false : true;
		
		if (!hasTitle) {
			holder.indicate.setVisibility(View.GONE);
		}
		// tws-start modify 0.2 expandablelistview style::2014-10-04
		holder.indicate.setVisibility(View.GONE);
		// tws-end modify 0.2 expandablelistview style::2014-10-04
		return convertView;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return getGroup(groupPosition);
	}
	
	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public int getGroupCount() {
		if (parentTitles != null)
			return parentTitles.size();
		else if (parentTitles == null && parentSubTitles != null)
			return parentSubTitles.size();
		return 0;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {

		ParentHolder holder = null;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.tws_parent_item, null);
			holder = new ParentHolder();
			holder.title = (TextView) convertView.findViewById(R.id.parent_title);
			holder.subtitle = (TextView) convertView.findViewById(R.id.parent_subtitle);
			holder.divider = (ImageView) convertView.findViewById(R.id.parent_divider);
			holder.indicate = (ImageView) convertView.findViewById(R.id.parent_indicate);
			convertView.setTag(holder);
		}
		else {
			holder = (ParentHolder)convertView.getTag();
		}
		
		holder.title.setVisibility(View.VISIBLE);
		holder.subtitle.setVisibility(View.VISIBLE);
		holder.divider.setVisibility(View.VISIBLE);
		holder.indicate.setVisibility(View.VISIBLE);
		
		if (parentTitles != null)
			holder.setTitle(parentTitles.get(groupPosition));
		if (parentSubTitles != null)
			holder.setSubtitle(parentSubTitles.get(groupPosition));
		
		Drawable[] pIndicates = {context.getResources().getDrawable(R.drawable.tws_expand_arrow_up), context.getResources().getDrawable(R.drawable.tws_expand_arrow_down)};
		if (parentIndicateUp == null || parentIndicateDown == null)
			holder.setIndicate(isExpanded ? pIndicates[0] : pIndicates[1]);
		else if (parentIndicateUp != null && parentIndicateDown != null)
			holder.setIndicate(isExpanded ? parentIndicateUp : parentIndicateDown);
		
		int titleColor = R.color.tws_text_light_body;
		holder.setTitleColor((parentTitleColor == 0) ? titleColor : parentTitleColor);
		
		int subtitleColor = R.color.tws_text_light_summary;
		holder.setSubtitleColor((parentSubtitleColor == 0) ? subtitleColor : parentSubtitleColor);
		
		if (parentDivider != null)
			holder.setDivider(parentDivider);
		
		boolean hasTitle = (holder.title == null || holder.title.getText().toString().equals("")) ? false : true;
		boolean hasSubTitle = (holder.subtitle.getText().toString().equals("") || holder.subtitle == null) ? false : true;
		
		if (hasTitle && !hasSubTitle) {
			holder.divider.setVisibility(View.GONE);
		}
		else if (!hasTitle && hasSubTitle) {
			holder.title.setVisibility(View.GONE);
			holder.divider.setVisibility(View.GONE);
		}
		
		// tws-start modify 0.2 expandablelistview style::2014-10-04
		holder.subtitle.setVisibility(View.GONE);
		holder.divider.setVisibility(View.GONE);
		// tws-end modify 0.2 expandablelistview style::2014-10-04
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		for (int i = 0; i < getGroupCount(); i++) {
			if (groupPosition != i && listView.isGroupExpanded(groupPosition)) {
				listView.collapseGroup(i);
			}
		}
	}
	
	private class ParentHolder {
		
		TextView title, subtitle;
		ImageView divider, indicate;
		
		public void setTitle(String str) {
			title.setText(str);
		}
		public void setSubtitle(String str) {
			subtitle.setText(str);
		}
		public void setIndicate(Drawable drawable) {
			indicate.setImageDrawable(drawable);
		}
		public void setDivider(Drawable drawable) {
			divider.setImageDrawable(drawable);
		}
		public void setTitleColor(int resId) {
			try {
				ColorStateList csl = (ColorStateList)context.getResources().getColorStateList(resId);
				title.setTextColor(csl);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void setSubtitleColor(int resId) {
			try {
				ColorStateList csl = (ColorStateList)context.getResources().getColorStateList(resId);
				subtitle.setTextColor(csl);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ChildHolder {
		
		TextView title;
		ImageView indicate;
		TextView info;
		
		public void setTitle(String str) {
			title.setText(str);
		}
		public void setIndicate(Drawable drawable) {
			indicate.setImageDrawable(drawable);
		}
		public void setInfo(String str) {
			info.setText(str);
		}
		public void setTitleColor(int resId) {
			try {
				ColorStateList csl = (ColorStateList)context.getResources().getColorStateList(resId);
				title.setTextColor(csl);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * set group main titles in standard layout
	 * @param datas the group`s title array
	 */
	public final void setParentTitles(ArrayList<String> datas) {
		this.parentTitles = datas;
	}
	
	/**
	 * set group subtitles in standard layout
	 * @param datas the group`s subtitle array
	 */
	public final void setParentSubtitles(ArrayList<String> datas) {
		this.parentSubTitles = datas;
	}
	
	/**
	 * set child titles in standard layout
	 * @param datas the child`s title array, key is index of its parent, value is title array 
	 */
	public final void setChildTitles(HashMap<Integer, ArrayList<String>> datas) {
		this.childTitles = datas;
	}
	
	/**
	 * set group indicator`s drawable in the right of the group layout when this group is expanded, it`ll show the default drawable when not called
	 * @param parentIndicateUp the drawable of the indicator
	 */
	public final void setParentIndicateUp(Drawable parentIndicateUp) {
		this.parentIndicateUp = parentIndicateUp;
	}

	/**
	 * set group indicator`s drawable in the right of the group layout when this group is collapsed, it`ll show the default drawable when not called
	 * @param parentIndicateDown the drawable of the indicator
	 */
	public final void setParentIndicateDown(Drawable parentIndicateDown) {
		this.parentIndicateDown = parentIndicateDown;
	}

	/**
	 * set group divider`s drawable between title and subtitle, it`ll show the default drawable when not called
	 * @param parentIndicateDown the drawable of the divider
	 */
	public final void setParentDivider(Drawable parentDivider) {
		this.parentDivider = parentDivider;
	}

	/**
	 * set child indicator`s drawable in the left of the group layout, it`ll show the default drawable when not called
	 * @param childIndicate the drawable of the indicator
	 */
	public final void setChildIndicate(Drawable childIndicate) {
		this.childIndicate = childIndicate;
	}
	
	/**
	 * set group title`s color, it`ll show the default color when not called
	 * @param color the color of the title
	 */
	public final void setParentTitleColor(int color) {
		this.parentTitleColor = color;
	}

	/**
	 * set group subtitle`s color, it`ll show the default color when not called
	 * @param color the color of the subtitle
	 */
	public final void setParentSubtitleColor(int color) {
		this.parentSubtitleColor = color;
	}
	
	/**
	 * set child title`s color, it`ll show the default color when not called
	 * @param color the color of the title
	 */
	public final void setChildTitleColor(int color) {
		this.childTitleColor = color;
	}
	
	/**
	 * set child indicate`s visibility, it`ll show when not called
	 * @param color the color of the title
	 */
	public final void setChildIndicateVisible(boolean flag) {
		this.childIndicateVisibility = flag;
	}
	
	/**
	 * set child infos in standard layout
	 * @param datas the child`s info array, key is index of its parent, value is info array 
	 */
	public final void setChildInfos(HashMap<Integer, ArrayList<String>> datas) {
		this.childInfos = datas;
	}
}

