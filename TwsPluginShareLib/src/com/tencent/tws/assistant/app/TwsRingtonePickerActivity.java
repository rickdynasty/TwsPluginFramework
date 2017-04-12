package com.tencent.tws.assistant.app;

import android.app.TwsActivity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.tencent.tws.assistant.widget.AdapterView;
import com.tencent.tws.assistant.widget.AdapterView.OnItemClickListener;
import com.tencent.tws.assistant.widget.ListView;
import com.tencent.tws.sharelib.R;

/**
 * The {@link TwsRingtonePickerActivity} allows the user to choose one from all
 * of the available ringtones. The chosen ringtone's URI will be persisted as a
 * string.
 * 
 * @see RingtoneManager#ACTION_RINGTONE_PICKER
 */
public class TwsRingtonePickerActivity extends TwsActivity implements Runnable {

	private static final String TAG = "RingtonePickerActivity";

	private static final int DELAY_MS_SELECTION_PLAYED = 300;

	private static final String SAVE_CLICKED_POS = "clicked_pos";

	private RingtoneManager mRingtoneManager;

	private Cursor mCursor;

	private Handler mHandler;

	/** The position in the list of the 'Silent' item. */
	private int mSilentPos = -1;

	/** The position in the list of the 'Default' item. */
	private int mDefaultRingtonePos = -1;

	/** The position in the list of the last clicked item. */
	private int mClickedPos = -1;

	/** The position in the list of the ringtone to sample. */
	private int mSampleRingtonePos = -1;

	/** Whether this list has the 'Silent' item. */
	private boolean mHasSilentItem;

	/** The Uri to place a checkmark next to. */
	private Uri mExistingUri;

	/** The number of static items in the list. */
	private int mStaticItemCount;

	/** Whether this list has the 'Default' item. */
	private boolean mHasDefaultItem;

	/** The Uri to play when the 'Default' item is clicked. */
	private Uri mUriForDefaultItem;

	/** the current ringtone */
	private Uri mUriCurrentItem;
	private String mCurrentTitle;
	private int mCurrentRingtonePos = -1;
	/**
	 * A Ringtone for the default ringtone. In most cases, the RingtoneManager
	 * will stop the previous ringtone. However, the RingtoneManager doesn't
	 * manage the default ringtone for us, so we should stop this one manually.
	 */
	private Ringtone mDefaultRingtone;
	// system ringtone
	private ListView mListView1 = null;
	// local ringtone
	private ListView mListView2 = null;
	// database column
	public String mLabelColumn = "title";
	// actionbar
	private ActionBar actionBar;
	// ringtone types
	private int mRinfTypes;
	// bottom button
	private TextView mTextView;
	// exclude id
	private Uri mBaseUri = null;
	// choose other ring flag
	private boolean bChooseOtherRing = false;
	// choose ringtone
	private Ringtone mChoosedRingtone;
	private int mPreviousClickedPos = -1;
	// local ringtone uri
	private Uri mRingUri = null;
	// mExistingUri is default?
	boolean mIsDefault = false;
	// SystemSettingsCall?
	boolean bSystemSettingsCall = false;
	// ring title
	private String mRingTitle;
	// query field
	private static final String[] projection = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
			"\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"", MediaStore.Audio.Media.TITLE_KEY };
	private static final String[] mCurrentprojection = new String[] { MediaStore.Audio.Media.TITLE };

	private static final int MENU_CHOOSE = Menu.FIRST;
	/**
	 * The column index (in the cursor returned by {@link #getCursor()} for the
	 * row ID.
	 */
	public static final int ID_COLUMN_INDEX = 0;

	/**
	 * The column index (in the cursor returned by {@link #getCursor()} for the
	 * title.
	 */
	public static final int TITLE_COLUMN_INDEX = 1;

	/**
	 * The column index (in the cursor returned by {@link #getCursor()} for the
	 * media provider's URI.
	 */
	public static final int URI_COLUMN_INDEX = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ring_single_choice_list);
		actionBar = getTwsActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setHomeButtonEnabledQQ(true);

		mListView1 = (ListView) findViewById(R.id.ringList1);
		mListView1.setOnItemClickListener(mOnItemClickListener);
		mListView1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		mListView2 = (ListView) findViewById(R.id.ringList2);
		mListView2.setOnItemClickListener(mOnItemClickListener);
		mListView2.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		mTextView = (TextView) findViewById(R.id.empty_message);
		mHandler = new Handler();

		Intent intent = getIntent();

		/*
		 * Get whether to show the 'Default' item, and the URI to play when the
		 * default is clicked
		 */
		bSystemSettingsCall = intent.getBooleanExtra("system_settings", false);
		mHasDefaultItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		mUriForDefaultItem = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
		if (mUriForDefaultItem == null) {
			mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
		}

		if (savedInstanceState != null) {
			mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, -1);
		}
		// Get whether to show the 'Silent' item
		mHasSilentItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

		// Give the Activity so it can do managed queries
		mRingtoneManager = new RingtoneManager(this);

		// Get whether to include DRM ringtones
		boolean includeDrm = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
		mRingtoneManager.setIncludeDrm(includeDrm);

		// Get the types of ringtones to show
		mRinfTypes = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
		if (mRinfTypes != -1) {
			mRingtoneManager.setType(mRinfTypes);
		}

		// set title
		if (mRinfTypes == RingtoneManager.TYPE_NOTIFICATION)
			actionBar.setTitle(R.string.choose_notification_title);
		else if (mRinfTypes == RingtoneManager.TYPE_ALARM)
			actionBar.setTitle(R.string.choose_alarm_title);
		else
			actionBar.setTitle(R.string.choose_ring_title);

		// The volume keys will control the stream that we are choosing a
		// ringtone for
		setVolumeControlStream(mRingtoneManager.inferStreamType());

		// Get the URI whose list item should have a checkmark
		mExistingUri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
		mCursor = mRingtoneManager.getCursor();
		mCursor.moveToFirst();
		onPrepareListView(mListView1);
		ListAdapter listAdapter = new SimpleCursorAdapter(this, R.layout.ring_single_choice_item, mCursor,
				new String[] { mLabelColumn }, new int[] { R.id.ringTtitle });
		mListView1.setAdapter(listAdapter);
		mListView2.setVisibility(View.GONE);
		mListView1.setVisibility(View.VISIBLE);
		/*
		 * if (mCurrentRingtonePos > -1) {
		 * mListView1.setItemChecked(mCurrentRingtonePos, true);
		 * mListView1.setSelection(mCurrentRingtonePos); }else
		 */if (mClickedPos > -1) {
			mListView1.setItemChecked(mClickedPos, true);
			mListView1.setSelection(mClickedPos);
		}
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			setActionBarStyle();
			// Save the position of most recently clicked item
			mClickedPos = position;
			// Play clip
			playRingtone(position, 0);
		}

	};

	public void onPrepareListView(ListView listView) {
		mIsDefault = RingtoneManager.isDefault(mExistingUri);
		mUriCurrentItem = RingtoneManager.getActualDefaultRingtoneUri(this, mRinfTypes);
		if (mExistingUri != null && !mIsDefault) {
			Cursor currentCursor = null;
			try {
				currentCursor = getContentResolver().query(mExistingUri, mCurrentprojection, null, null, null);
				if (currentCursor != null && currentCursor.getCount() > 0) {
					currentCursor.moveToFirst();
					mCurrentTitle = currentCursor.getString(0);
					if (mCurrentTitle != null) {
						mCurrentRingtonePos = addCurrentItem(listView);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (currentCursor != null) {
					currentCursor.close();
				}
			}
			mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
		}
		if (mHasDefaultItem) {
			mDefaultRingtonePos = addDefaultRingtoneItem(listView);

			if (mIsDefault) {
				mClickedPos = mDefaultRingtonePos;
			} else
				mClickedPos = mClickedPos + 1;
		}
		if (mHasSilentItem) {
			mSilentPos = addSilentItem(listView);

			// The 'Silent' item should use a null Uri
			if (mExistingUri == null) {
				mClickedPos = mSilentPos;
			} else
				mClickedPos = mClickedPos + 1;
		}

		if (mClickedPos == -1) {
			mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
		}

	}

	private int getPositionInListview(Uri baseUri, Uri mExistingUri) {
		if (mCursor != null && baseUri != null && mExistingUri != null) {
			mCursor.moveToFirst();
			int mCount = mCursor.getCount();
			for (int i = 0; i < mCount; i++) {
				// Log.d(TAG, "id = "+mCursor.getLong(ID_COLUMN_INDEX));
				if (mExistingUri.equals(ContentUris.withAppendedId(baseUri, mCursor.getLong(ID_COLUMN_INDEX)))) {
					return i;
				}
				if (!mCursor.moveToNext()) {
					Log.d(TAG, "not find");
					return -1;
				}
			}
		}

		return -1;
	}

	/**
	 * Adds a static item to the top of the list. A static item is one that is
	 * not from the RingtoneManager.
	 * 
	 * @param listView
	 *            The ListView to add to.
	 * @param textResId
	 *            The resource ID of the text for the item.
	 * @return The position of the inserted item.
	 */
	private int addStaticItem(ListView listView, int textResId) {
		TextView textView = (TextView) getLayoutInflater().inflate(R.layout.ring_single_choice_item, listView, false);
		textView.setText(textResId);
		listView.addHeaderView(textView);
		mStaticItemCount++;
		return listView.getHeaderViewsCount() - 1;
	}

	private int addDefaultRingtoneItem(ListView listView) {
		return addStaticItem(listView, R.string.ringtone_default);
	}

	private int addSilentItem(ListView listView) {
		return addStaticItem(listView, R.string.ringtone_silent);
	}

	private int addStaticItem(ListView listView, String textRes) {
		View customView = (View) getLayoutInflater().inflate(R.layout.current_ring_item, listView, false);
		TextView curRingTint = (TextView) customView.findViewById(R.id.currentRingTitleTint);
		TextView curRingTxt = (TextView) customView.findViewById(R.id.currentRingTitleTxt);
		curRingTint.setText(getString(R.string.current_ring));
		curRingTxt.setText(textRes);
		listView.addHeaderView(customView);
		mStaticItemCount++;
		return listView.getHeaderViewsCount() - 1;
	}

	private int addCurrentItem(ListView listView) {
		// return addStaticItem(listView, mCurrentTitle);
		return -1;
	}

	/*
	 * On click of Ok/Cancel buttons
	 */
	public void onButtonClick(int which) {
		boolean positiveResult = which == R.id.save_menu_item;
		if (bChooseOtherRing) {
			// Stop playing the previous ringtone
			if (mChoosedRingtone != null) {
				mChoosedRingtone.stop();
			}
			if (positiveResult) {
				// resetRingInUsbMode();
				ContentValues values = new ContentValues();
				if (mRinfTypes != -1) {
					if (mRinfTypes == RingtoneManager.TYPE_RINGTONE) {
						values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
					} else if (mRinfTypes == RingtoneManager.TYPE_NOTIFICATION) {
						values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
					} else if (mRinfTypes == RingtoneManager.TYPE_ALARM) {
						values.put(MediaStore.Audio.Media.IS_ALARM, true);
					}
					getContentResolver().update(mRingUri, values, null, null);
				}
				saveRingForRingType(mRinfTypes, getRingTitle(mRingUri));
				Intent resultIntent = new Intent();
				resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, mRingUri);
				setResult(RESULT_OK, resultIntent);
			} else {
				setResult(RESULT_CANCELED);
			}
		} else {
			// Stop playing the previous ringtone
			mRingtoneManager.stopPreviousRingtone();

			if (positiveResult) {
				// resetRingInUsbMode();
				Intent resultIntent = new Intent();
				Uri uri = null;
				Uri defaultUri = null;
				if (mClickedPos == mDefaultRingtonePos) {
					// Set it to the default Uri that they originally gave us
					uri = mUriForDefaultItem;
					if (mRinfTypes == RingtoneManager.TYPE_RINGTONE)
						defaultUri = Uri.parse(Settings.System.getString(getContentResolver(), "ringtone"));
					else if (mRinfTypes == RingtoneManager.TYPE_NOTIFICATION)
						defaultUri = Uri.parse(Settings.System.getString(getContentResolver(), "notification_sound"));
					else if (mRinfTypes == RingtoneManager.TYPE_ALARM)
						defaultUri = Uri.parse(Settings.System.getString(getContentResolver(), "alarm_alert"));
				} else if (mClickedPos == mSilentPos) {
					// A null Uri is for the 'Silent' item
					uri = null;
				} else if (mClickedPos == mCurrentRingtonePos && !mIsDefault) {
					// A null Uri is for the 'Current' item
					uri = mExistingUri;
				} else {
					uri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(mClickedPos));
				}
				if (mClickedPos == mDefaultRingtonePos)
					saveRingForRingType(mRinfTypes, getRingTitle(defaultUri));
				else
					saveRingForRingType(mRinfTypes, getRingTitle(uri));
				resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
				setResult(RESULT_OK, resultIntent);
			} else {
				setResult(RESULT_CANCELED);
			}
		}
		/*
		 * getWindow().getDecorView().post(new Runnable() { public void run() {
		 * mCursor.deactivate(); } });
		 */

		finish();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putInt(SAVE_CLICKED_POS, mClickedPos);
	}

	private void playRingtone(int position, int delayMs) {
		mHandler.removeCallbacks(this);
		mSampleRingtonePos = position;
		mHandler.postDelayed(this, delayMs);
	}

	public void run() {
		if (bChooseOtherRing) {
			if (mChoosedRingtone != null)
				mChoosedRingtone.stop();

			mPreviousClickedPos = mSampleRingtonePos;
			mCursor.moveToFirst();
			if (!mCursor.moveToPosition(mSampleRingtonePos)) {
				return;
			}
			mRingUri = ContentUris.withAppendedId(mBaseUri, mCursor.getLong(ID_COLUMN_INDEX));
			if (mRingUri == null)
				return;
			mChoosedRingtone = RingtoneManager.getRingtone(this, mRingUri);
			if (mChoosedRingtone != null) {
				mChoosedRingtone.play();
			}
		} else {
			if (mChoosedRingtone != null && mChoosedRingtone.isPlaying())
				mChoosedRingtone.stop();
			if (mSampleRingtonePos == mSilentPos) {
				mRingtoneManager.stopPreviousRingtone();
				return;
			}

			/*
			 * Stop the default ringtone, if it's playing (other ringtones will
			 * be stopped by the RingtoneManager when we get another Ringtone
			 * from it.
			 */
			if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
				mDefaultRingtone.stop();
				mDefaultRingtone = null;
			}

			Ringtone ringtone;
			if (mSampleRingtonePos == mDefaultRingtonePos) {
				if (mDefaultRingtone == null) {
					mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
				}
				ringtone = mDefaultRingtone;

				/*
				 * Normally the non-static RingtoneManager.getRingtone stops the
				 * previous ringtone, but we're getting the default ringtone
				 * outside of the RingtoneManager instance, so let's stop the
				 * previous ringtone manually.
				 */
				mRingtoneManager.stopPreviousRingtone();

			} else if (mSampleRingtonePos == mCurrentRingtonePos && mExistingUri != null && !mIsDefault) {
				mChoosedRingtone = RingtoneManager.getRingtone(this, mExistingUri);
				mRingtoneManager.stopPreviousRingtone();
				if (mChoosedRingtone != null) {
					mChoosedRingtone.play();
				}
				return;
			} else {
				ringtone = mRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos));
			}

			if (ringtone != null) {
				ringtone.play();
			}
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		registerSdcardReceiver();
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		stopAnyPlayingRingtone();
		if (mChoosedRingtone != null) {
			mChoosedRingtone.stop();
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		stopAnyPlayingRingtone();
		if (mChoosedRingtone != null) {
			mChoosedRingtone.stop();
		}
	}

	protected void onDestroy() {
		// TODO Auto-generated method stub
		bChooseOtherRing = false;
		unregisterSdcardReceiver();
		getWindow().getDecorView().post(new Runnable() {
			public void run() {
				if (mCursor != null)
					mCursor.deactivate();
			}
		});
		super.onDestroy();
	}

	private void stopAnyPlayingRingtone() {

		if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
			mDefaultRingtone.stop();
		}

		if (mRingtoneManager != null) {
			mRingtoneManager.stopPreviousRingtone();
		}
	}

	private int getRingtoneManagerPosition(int listPos) {
		return listPos - mStaticItemCount;
	}

	private int getListPosition(int ringtoneManagerPos) {

		// If the manager position is -1 (for not found), return that
		if (ringtoneManagerPos < 0)
			return ringtoneManagerPos;

		return ringtoneManagerPos + mStaticItemCount;
	}

	private void setActionBarStyle() {
		if (actionBar != null) {
			// Inflate a custom action bar that contains the "done" button for
			// saving changes
			// to the contact
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View customActionBarView = inflater.inflate(R.layout.ring_editor_custom_action_bar, null);
			View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
			saveMenuItem.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onButtonClick(v.getId());
				}
			});
			View discardMenuItem = customActionBarView.findViewById(R.id.discard_menu_item);
			discardMenuItem.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					onButtonClick(v.getId());
				}
			});
			// Show the custom action bar but hide the home icon and title
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
					| ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
			actionBar.setCustomView(customActionBarView);
		}
	}

	private void setActionBarStyle(int title) {
		if (actionBar != null) {
			actionBar.setDisplayShowCustomEnabled(false);
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(title);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		// return super.onCreateOptionsMenu(menu);
		MenuItem mChoose = menu.add(0, MENU_CHOOSE, 0, R.string.choose_other_ringtone);
		mChoose.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case MENU_CHOOSE:
			stopAnyPlayingRingtone();
			if (mChoosedRingtone != null) {
				mChoosedRingtone.stop();
			}
			actionBar.splitActionbar_hide();
			setActionBarStyle(R.string.pick_other_ring_title);
			bChooseOtherRing = true;
			stopAnyPlayingRingtone();
			mListView1.setVisibility(View.GONE);
			mListView2.setVisibility(View.VISIBLE);
			mTextView.setVisibility(View.GONE);
			setVolumeControlStream(mRingtoneManager.inferStreamType());
			// Get the external media cursor. First check to see if it is
			// mounted.
			final String status = Environment.getExternalStorageState();
			if (status.equals(Environment.MEDIA_MOUNTED) || status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
				// if(mCursor != null)
				// mCursor.close();
				mCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null,
						null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
				if (mCursor != null && mCursor.getCount() > 0) {
					int num = mCursor.getCount();
					mCursor.moveToFirst();
					mBaseUri = Uri.parse(mCursor.getString(URI_COLUMN_INDEX));
				} else {
					mListView1.setVisibility(View.GONE);
					mListView2.setVisibility(View.GONE);
					mTextView.setVisibility(View.VISIBLE);
					return true;
				}

			} else {
				mListView1.setVisibility(View.GONE);
				mListView2.setVisibility(View.GONE);
				mTextView.setVisibility(View.VISIBLE);
				return true;
			}
			ListAdapter listAdapter = new SimpleCursorAdapter(TwsRingtonePickerActivity.this,
					R.layout.ring_single_choice_item, mCursor, new String[] { mLabelColumn },
					new int[] { R.id.ringTtitle });
			mListView2.setAdapter(listAdapter);
			Uri mActualUri = RingtoneManager.getActualDefaultRingtoneUri(TwsRingtonePickerActivity.this, mRinfTypes);
			if (mExistingUri != null)
				mClickedPos = getPositionInListview(mBaseUri, mExistingUri);
			else
				mClickedPos = getPositionInListview(mBaseUri, mActualUri);
			if (mClickedPos > -1) {
				mListView2.setItemChecked(mClickedPos, true);
				mListView2.setSelection(mClickedPos);
			}
			return true;
		default:
			return false;
		}
		// return super.onOptionsItemSelected(item);
	}

	/*
	 * private void resetRingInUsbMode(){ final String status =
	 * Environment.getExternalStorageState();
	 * if(status.equals(Environment.MEDIA_UNMOUNTED)&&bSystemSettingsCall){
	 * if(mRinfTypes == RingtoneManager.TYPE_NOTIFICATION)
	 * Settings.System.putInt(getContentResolver(),
	 * Settings.System.RESET_NOTIFICATION_IN_USB, 1); else if(mRinfTypes ==
	 * RingtoneManager.TYPE_ALARM) Settings.System.putInt(getContentResolver(),
	 * Settings.System.RESET_ALARM_IN_USB, 1); else
	 * Settings.System.putInt(getContentResolver(),
	 * Settings.System.RESET_RING_IN_USB, 1);
	 * 
	 * } }
	 */
	private void registerSdcardReceiver() {
		initSdcardFilter();
		registerReceiver(broadcastRec, mSdcardFilter);
	}

	private void unregisterSdcardReceiver() {
		unregisterReceiver(broadcastRec);
	}

	private IntentFilter mSdcardFilter = new IntentFilter();

	private void initSdcardFilter() {
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_SHARED);
		mSdcardFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		mSdcardFilter.addDataScheme("file");
	}

	private BroadcastReceiver broadcastRec = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				if (mTextView != null) {
					mTextView.setText(R.string.update_sdcard);
				}
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
				// SD���Ѿ��ɹ�����
				if (bChooseOtherRing) {
					mListView1.setVisibility(View.GONE);
					mListView2.setVisibility(View.VISIBLE);
					mTextView.setVisibility(View.GONE);
					// if(mCursor != null)
					// mCursor.close();
					mCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null,
							null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
					if (mCursor != null && mCursor.getCount() > 0) {
						mCursor.moveToFirst();
						mBaseUri = Uri.parse(mCursor.getString(URI_COLUMN_INDEX));
					}
					ListAdapter listAdapter = new SimpleCursorAdapter(TwsRingtonePickerActivity.this,
							R.layout.ring_single_choice_item, mCursor, new String[] { mLabelColumn },
							new int[] { R.id.ringTtitle });
					mListView2.setAdapter(listAdapter);
					Uri mActualUri = RingtoneManager.getActualDefaultRingtoneUri(TwsRingtonePickerActivity.this,
							mRinfTypes);
					if (mExistingUri != null)
						mClickedPos = getPositionInListview(mBaseUri, mExistingUri);
					else
						mClickedPos = getPositionInListview(mBaseUri, mActualUri);
					if (mClickedPos > -1) {
						mListView2.setItemChecked(mClickedPos, true);
						mListView2.setSelection(mClickedPos);
					}
				}
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)// ����δ����״̬
					|| intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)
					|| intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)
					|| intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)
					|| intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
				if (bChooseOtherRing) {
					mListView1.setVisibility(View.GONE);
					mListView2.setVisibility(View.GONE);
					mTextView.setVisibility(View.VISIBLE);
					actionBar.setCustomView(null);
					setActionBarStyle(R.string.pick_other_ring_title);
				}
			}
		}
	};

	private void saveRingForRingType(int ringType, String ringTitle) {
		if (ringType != -1 && ringTitle != null) {
			if (ringType == RingtoneManager.TYPE_RINGTONE) {
				Settings.System.putString(getContentResolver(), "default_ring", ringTitle);
			} else if (ringType == RingtoneManager.TYPE_NOTIFICATION) {
				Settings.System.putString(getContentResolver(), "default_notification", ringTitle);
			} else if (ringType == RingtoneManager.TYPE_ALARM) {
				Settings.System.putString(getContentResolver(), "default_alarm", ringTitle);
			}
		}
	}

	private String getRingTitle(Uri uriRing) {
		Cursor mCurrentCursor = null;
		try {
			String mTitle = null;
			if (uriRing == null) {
				return mTitle;
			}
			mCurrentCursor = getContentResolver().query(uriRing, mCurrentprojection, null, null, null);
			if (mCurrentCursor != null && mCurrentCursor.getCount() > 0) {
				mCurrentCursor.moveToFirst();
				mTitle = mCurrentCursor.getString(0);
				mCurrentCursor.close();
			}
			return mTitle;
		} finally {
			// TODO: handle exception
			if (mCurrentCursor != null) {
				mCurrentCursor.close();
			}
		}

	}
}
