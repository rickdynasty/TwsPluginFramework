package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.internal.view.menu.MenuItemImpl;

public class ActionMenuSelectedText extends TwsActivity {

	MenuItemImpl mCopyMenu;
	MenuItemImpl mCutMenu;
	MenuItemImpl mDeleteMenu;
	MenuItemImpl mDeleteMenu2;
	MenuItemImpl mDeleteMenu3;
	MenuItemImpl mEditMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setSplitActionWhenNarrowOptions(true);
		getTwsActionBar().setTitle(R.string.action_bar_title);
		getTwsActionBar().setSubtitle(R.string.action_bar_subtitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.action_menu_selected_text, menu);
		mCopyMenu = (MenuItemImpl) menu.findItem(R.id.action_copy);
		mCutMenu = (MenuItemImpl) menu.findItem(R.id.action_cut);
		mDeleteMenu = (MenuItemImpl) menu.findItem(R.id.action_delete);
		mEditMenu = (MenuItemImpl) menu.findItem(R.id.action_edit);
		mDeleteMenu2 = (MenuItemImpl) menu.findItem(R.id.action_delete2);
		mDeleteMenu3 = (MenuItemImpl) menu.findItem(R.id.action_delete3);
		mCopyMenu.setSelected(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_copy:
			mCopyMenu.setSelected(true);
			mCutMenu.setSelected(false);
			mDeleteMenu.setSelected(false);
			mEditMenu.setSelected(false);
			mDeleteMenu2.setEnabled(false);
			mDeleteMenu3.setEnabled(false);
			break;
		case R.id.action_cut:
			mCopyMenu.setSelected(false);
			mCutMenu.setSelected(true);
			mDeleteMenu.setSelected(false);
			mEditMenu.setSelected(false);
			break;
		case R.id.action_delete:
			mCopyMenu.setSelected(false);
			mCutMenu.setSelected(false);
			mDeleteMenu.setSelected(true);
			mEditMenu.setSelected(false);
			break;
		case R.id.action_edit:
			mCopyMenu.setSelected(false);
			mCutMenu.setSelected(false);
			mDeleteMenu.setSelected(false);
			mEditMenu.setSelected(true);
			break;

		default:
			break;
		}
		return false;
	}
}
