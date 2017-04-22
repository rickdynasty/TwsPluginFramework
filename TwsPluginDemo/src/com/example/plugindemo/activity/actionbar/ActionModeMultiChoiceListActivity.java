package com.example.plugindemo.activity.actionbar;

import java.util.ArrayList;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.ActionBar;
import com.tencent.tws.assistant.widget.AbsListView;
import com.tencent.tws.assistant.widget.AbsListView.MultiChoiceModeListener;
import com.tencent.tws.assistant.widget.AbsListView.OnScrollListener;
import com.tencent.tws.assistant.widget.ListView;

public class ActionModeMultiChoiceListActivity extends TwsListActivity {

	private ActionBar mActionBar;
	private boolean mScrollFlag = false;
	private int mLastVisibleItemPosition = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		mActionBar = getTwsActionBar();
		mActionBar.setShowHideAnimationEnabled(true);
		ListView mListView = getListView();
		ArrayList<String> mArrayList = new ArrayList<String>();
		String str = null;
		for (int i = 0; i < 101; i++) {
			str = new String("ListItem " + i);
			mArrayList.add(str);
		}
		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, R.layout.multi_choice_text_item, mArrayList);
		mListView.setAdapter(mAdapter);

		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mListView.setMultiChoiceModeListener(mListener);
		mListView.setOnScrollListener(mOnScrollListener);
	}

	private MultiChoiceModeListener mListener = new MultiChoiceModeListener() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			Log.e(ActionModeNormal.TAG, "onPrepareActionMode");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub
			Log.e(ActionModeNormal.TAG, "onDestroyActionMode");
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			mode.getMenuInflater().inflate(R.menu.activity_main, menu);
			Log.e(ActionModeNormal.TAG, "onCreateActionMode");
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO Auto-generated method stub
			Log.e(ActionModeNormal.TAG, "onActionItemClicked");
			mode.finish();
			return true;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			// TODO Auto-generated method stub
			Log.e(ActionModeNormal.TAG, "onItemCheckedStateChanged");
		}
	};

	OnScrollListener mOnScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
				mScrollFlag = true;
			} else {
				mScrollFlag = false;
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (mScrollFlag) {
				if (firstVisibleItem > mLastVisibleItemPosition) {
					mActionBar.hide();
				}
				if (firstVisibleItem < mLastVisibleItemPosition) {
					mActionBar.show();
				}
				if (firstVisibleItem == mLastVisibleItemPosition) {
					return;
				}
				mLastVisibleItemPosition = firstVisibleItem;
			}
		}
	};
}
