package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.plugindemo.R;
import com.example.plugindemo.activity.category.listview.CheckListView;
import com.example.plugindemo.activity.category.listview.ListViewComplex;
import com.example.plugindemo.activity.category.listview.ListViewTest;
import com.example.plugindemo.activity.category.listview.MultipleChoiceList;
import com.example.plugindemo.activity.category.listview.NormalListView;
import com.example.plugindemo.activity.category.listview.SimpleListView;

public class ListViewSamples extends TwsActivity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_listview_samples);

		findViewById(R.id.listview_simple).setOnClickListener(this);
		findViewById(R.id.listview_normal).setOnClickListener(this);
		findViewById(R.id.listview_check).setOnClickListener(this);
		findViewById(R.id.listview_expandable_custom).setOnClickListener(this);
		findViewById(R.id.listview_expandable_default).setOnClickListener(this);
		findViewById(R.id.listview_checklist).setOnClickListener(this);
		findViewById(R.id.listview_default).setOnClickListener(this);
		findViewById(R.id.listview_custom).setOnClickListener(this);
		findViewById(R.id.listview_multi).setOnClickListener(this);

		getTwsActionBar().setTitle("Pickers示例");
	}

	@Override
	public void onClick(View view) {
		Intent intent = null;
		switch (view.getId()) {
		case R.id.listview_simple:
			intent = new Intent();
			intent.setClassName(this, SimpleListView.class.getName());
			break;
		case R.id.listview_normal:
			intent = new Intent();
			intent.setClassName(this, NormalListView.class.getName());
			break;
		case R.id.listview_check:
			break;
		case R.id.listview_expandable_custom:
			break;
		case R.id.listview_expandable_default:
			break;
		case R.id.listview_checklist:
			intent = new Intent();
			intent.setClassName(this, CheckListView.class.getName());
			break;
		case R.id.listview_default:
			intent = new Intent();
			intent.setClassName(this, ListViewTest.class.getName());
			break;
		case R.id.listview_custom:
			intent = new Intent();
			intent.setClassName(this, ListViewComplex.class.getName());
			break;
		case R.id.listview_multi:
			intent = new Intent();
			intent.setClassName(this, MultipleChoiceList.class.getName());
			break;
		default:
			break;
		}

		if (intent != null) {
			startActivity(intent);
		}
	}
}
