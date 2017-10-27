package com.example.plugindemo.activity.actionbar;

import java.util.ArrayList;

import android.app.TwsListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.widget.ListView;

public class ActionModeCheckListActivity extends TwsListActivity implements ActionModeCheckAdapter.Callback {

    private ActionMode mActionMode = null;
    private ActionModeCheckAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView mListView = getListView();
        ArrayList<String> mArrayList = new ArrayList<String>();
        String str = null;
        for (int i = 0; i < 101; i++) {
            str = new String("ListItemCheckBox " + i);
            mArrayList.add(str);
        }
        mAdapter = new ActionModeCheckAdapter(this, R.layout.check_list_item, R.id.text, mArrayList, this);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onAdapterCheckedListener() {
        // TODO Auto-generated method stub
        if (mActionMode == null) {
            mActionMode = startActionMode(mCallback);
        }
    }

    @Override
    public void onAdapterUnCheckedListener() {
        // TODO Auto-generated method stub
        if (!mAdapter.hasChecked() && mActionMode != null) {
            mActionMode.finish();
        }
    }

    private ActionMode.Callback mCallback = new ActionMode.Callback() {

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
            mActionMode = null;
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            Log.e(ActionModeNormal.TAG, "onCreateActionMode");
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            Log.e(ActionModeNormal.TAG, "onActionItemClicked");
            switch (item.getItemId()) {
                case R.id.delete:
                    mAdapter.removeSelected();
                    mode.finish();
                    return true;
                case R.id.selectall:
                    mAdapter.selectAll();
                    return true;
                case R.id.unselectall:
                    mAdapter.unselectAll();
                    mode.finish();
                    return true;
            }
            return false;
        }
    };

}
