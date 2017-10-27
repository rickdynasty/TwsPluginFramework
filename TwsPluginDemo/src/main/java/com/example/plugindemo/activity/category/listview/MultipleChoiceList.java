package com.example.plugindemo.activity.category.listview;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;

import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.framework.HostProxy;

public class MultipleChoiceList extends TwsListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		setTitle("MultipleChoiceList");

		setListAdapter(new ArrayAdapter<String>(this, HostProxy.getShareLayoutId("select_dialog_multichoice"), GENRES));

		final ListView listView = getListView();

		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		listView.setClickDelay(false);
	}

	private static final String[] GENRES = new String[] { "Action", "Adventure", "Animation", "Children", "Comedy",
			"Documentary", "Drama", "Foreign", "History", "Independent", "Romance", "Sci-Fi", "Television", "Thriller" };
}
