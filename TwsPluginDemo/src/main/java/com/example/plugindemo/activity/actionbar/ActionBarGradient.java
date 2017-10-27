package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.ToggleButton;

public class ActionBarGradient extends TwsActivity {

	private ActionMode mActionMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.act_actionbar_gradient);
		getTwsActionBar().setTitle(R.string.action_bar_title);
		getTwsActionBar().setSubtitle(R.string.action_bar_subtitle);
		setupButtonLongClick();
	}

	private void setupButtonLongClick() {
		Button longClickButton = (Button) findViewById(R.id.longclick);
		longClickButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				// must have before startActionMode()
				mActionMode = startActionMode(mActionModeCallback);
				mActionMode.setTitle("一二三");
				((Button) getTwsActionBar().getCloseView(true)).setText("打开");
				ToggleButton rightButton = (ToggleButton) getTwsActionBar().getMultiChoiceView();
				rightButton.setChecked(false);
				return true;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.action_bar_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		getTwsActionBar().setPopupMenuMarks(false, new boolean[] { false, false, false });
		return true;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			getTwsActionBar().twsSetActionModeBackOnClickListener(null);
			mActionMode = null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			menu.add("title1").setIcon(getResources().getDrawable(R.drawable.ic_menu_copy_bottom))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.add("title2").setIcon(getResources().getDrawable(R.drawable.ic_menu_cut_bottom))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.add("title3").setIcon(getResources().getDrawable(R.drawable.ic_menu_share_bottom))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.add("title4").setIcon(getResources().getDrawable(R.drawable.ic_menu_edit_bottom))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.add("title5").setIcon(getResources().getDrawable(R.drawable.ic_menu_delete_bottom))
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO Auto-generated method stub
			mode.finish();
			return true;
		}
	};
}
