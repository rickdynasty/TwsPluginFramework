package com.example.plugindemo.activity.actionbar;

import android.app.TwsActivity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.ActionBar.OverflowClickListener;
import com.tencent.tws.assistant.gaussblur.JNIBlur;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.assistant.widget.ToggleButton;

public class ActionModeNormal extends TwsActivity {

	public static final String TAG = "ACTION_MODE_TEST";
	private ActionMode mActionMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.actionmode_main);
		setupButtonLongClick();
		setupCheckBox();
		setupToggleSplitClick();
		getTwsActionBar().setShowHideAnimationEnabled(true);

		Button listUp = (Button) findViewById(R.id.list_up);
		listUp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(ActionModeNormal.this, ActionModeMultiChoiceListActivity.class));
			}
		});

		Button listDown = (Button) findViewById(R.id.list_down);
		View listDown1 = findViewById(R.id.list_down);
		LayoutParams lp = listDown1.getLayoutParams();
		lp.height = listDown1.getMeasuredHeight() + 20;
		listDown1.setLayoutParams(lp);

		listDown.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(ActionModeNormal.this, ActionModeCheckListActivity.class));
			}
		});

		ImageView imageView = (ImageView) findViewById(R.id.blur_img);
		JNIBlur mBlur = new JNIBlur(ActionModeNormal.this);
		imageView.setImageBitmap(mBlur.blur(
				((BitmapDrawable) (getResources().getDrawable(R.drawable.blurtestimg))).getBitmap(), true));

		LinearLayout layout = (LinearLayout) findViewById(R.id.blur_layout);
		layout.setBackground(new BitmapDrawable(mBlur.blur(
				((BitmapDrawable) (getResources().getDrawable(R.drawable.bgbg))).getBitmap(), true)));
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getTwsActionBar().setTitle("一二三");
		getTwsActionBar().setOverflowDelay(false, true);
		// getTwsActionBar().setIsTransPopup(false, true);
		getTwsActionBar().setOverflowClickListener(new OverflowClickListener() {
			@Override
			public void doClick() {
			}
		}, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void setupButtonLongClick() {
		Button longClickButton = (Button) findViewById(R.id.longclick);
		longClickButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				// must have before startActionMode()
				mActionMode = startActionMode(mActionModeCallback);
				getTwsActionBar().setIsTransPopup(true, true);
				getTwsActionBar().setOverflowClickListener(new OverflowClickListener() {

					@Override
					public void doClick() {
						Log.v(TAG, "bbbbb");
					}
				}, true);
				mActionMode.setTitle("一二三");
				((Button) getTwsActionBar().getCloseView(true)).setText("打开");
				ToggleButton rightButton = (ToggleButton) getTwsActionBar().getMultiChoiceView();
				rightButton.setChecked(false);
				rightButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Toast.makeText(ActionModeNormal.this, "ActionMode Right Button Pressed", Toast.LENGTH_LONG)
								.show();
					}
				});

				return true;
			}
		});
	}

	private void setupToggleSplitClick() {
		final Button toggleSplit = (Button) findViewById(R.id.toggle_split);
		toggleSplit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean isHide = getTwsActionBar().splitActionbarIsHide();
				if (isHide) {
					getTwsActionBar().splitActionbar_show();
					toggleSplit.setText(R.string.toggle_split_off);
				} else {
					getTwsActionBar().splitActionbar_hide();
					toggleSplit.setText(R.string.toggle_split_on);
				}
			}
		});
	}

	private void setupCheckBox() {
		CheckBox mCheckBox = (CheckBox) findViewById(R.id.check);
		mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					if (mActionMode == null) {
						mActionMode = startActionMode(mActionModeCallback);
					}
				} else {
					if (mActionMode != null) {
						mActionMode.finish();
					}
				}
			}
		});
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onPrepareActionMode");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onDestroyActionMode");
			getTwsActionBar().twsSetActionModeBackOnClickListener(null);
			mActionMode = null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			// TODO Auto-generated method stub
			// mode.getMenuInflater().inflate(R.menu.activity_main, menu);
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
			Log.e(TAG, "onCreateActionMode");
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onActionItemClicked");
			Toast.makeText(ActionModeNormal.this, "clicked", Toast.LENGTH_SHORT).show();
			// mode.finish();
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "subject");
			intent.putExtra(Intent.EXTRA_TEXT, "text");
			startActivity(Intent.createChooser(intent, "subject"));
			return true;
		}
	};

	private OnClickListener mActionModeBackOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Toast.makeText(ActionModeNormal.this, "ActionMode Back Clicked", Toast.LENGTH_SHORT).show();
			// if (mActionMode != null) {
			// mActionMode.finish();
			// }
		}
	};

	private void setViewHeight(View view) {

		final View mView = view;

		ViewTreeObserver vto = mView.getViewTreeObserver();

		vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			public boolean onPreDraw() {

				int height = mView.getMeasuredHeight();
				LayoutParams lp = mView.getLayoutParams();
				lp.height = height + 20;
				mView.setLayoutParams(lp);
				return true;
			}
		});

	}

}
