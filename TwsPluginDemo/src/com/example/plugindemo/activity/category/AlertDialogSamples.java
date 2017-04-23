/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.plugindemo.activity.category;

import android.app.TwsActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.AlertDialog;
import com.tencent.tws.assistant.app.CheckBoxDialog;
import com.tencent.tws.assistant.app.EditTextDialog;
import com.tencent.tws.assistant.app.ProgressDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.widget.ListView;

public class AlertDialogSamples extends TwsActivity {
	private static final int DIALOG_YES_NO_MESSAGE = 1;
	private static final int DIALOG_YES_NO_LONG_MESSAGE = 2;
	private static final int DIALOG_LIST = 3;
	private static final int DIALOG_PROGRESS = 4;
	private static final int DIALOG_SINGLE_CHOICE = 5;
	private static final int DIALOG_MULTIPLE_CHOICE = 6;
	private static final int DIALOG_TEXT_ENTRY = 7;
	private static final int DIALOG_MULTIPLE_CHOICE_CURSOR = 8;
	private static final int DIALOG_YES_NO_ULTRA_LONG_MESSAGE = 9;
	private static final int DIALOG_YES_NO_OLD_SCHOOL_MESSAGE = 10;
	private static final int DIALOG_YES_NO_HOLO_LIGHT_MESSAGE = 11;
	private static final int DIALOG_NOT_DISMISS = 12;
	private static final int DIALOG_BOTTOM = 13;
	private static final int DIALOG_BOTTOM_SINGLE = 14;
	private static final int DIALOG_BOTTOM_MULT = 15;
	private static final int DIALOG_BOTTOM_BUTTON = 16;

	private static final int MAX_PROGRESS = 100;

	private ProgressDialog mProgressDialog;
	private int mProgress;
	private Handler mProgressHandler;
	TwsDialog qdialog;
	String[] dataList;
	private Context mContext;

	class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return dataList.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return dataList[arg0];
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {

			Holder holder;
			if (null == convertView) {
				holder = new Holder();
				convertView = (LinearLayout) LayoutInflater.from(AlertDialogSamples.this).inflate(
						R.layout.item_listview, null);
				holder.textView = (TextView) convertView.findViewById(R.id.textView1);
				holder.button = (Button) convertView.findViewById(R.id.button1);
				convertView.setTag(holder);
			} else {
				holder = (Holder) convertView.getTag();
			}
			holder.textView.setText(dataList[arg0]);
			holder.button.setText("click me");
			holder.button.setVisibility(View.GONE);
			return convertView;
		}

		class Holder {
			public TextView textView;
			public Button button;

		}

	}

	@Override
	protected TwsDialog onCreateTwsDialog(int id) {
		switch (id) {
		case DIALOG_BOTTOM_BUTTON:
			AlertDialog dialog = new AlertDialog(mContext, true);
			dialog.setTitle("title");
			dialog.setMessage("msg content");
			dialog.setButton("ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.setButton2("cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			String btn[] = new String[] { "ok", "cancel" };
			dialog.setBottomButtons(btn, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			dialog.setDialogDimAmount(0);
			return dialog;
		case DIALOG_BOTTOM_MULT:
			qdialog = new AlertDialog.Builder(AlertDialogSamples.this, true)
					.setMessage("bottom content msg")
					.setBottomButtonMultiChoiceItems(R.array.select_dialog_items,
							new boolean[] { false, true, false, true, false, false, false },
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {

								}
							}).show(true);
			return qdialog;
		case DIALOG_BOTTOM_SINGLE:
			qdialog = new AlertDialog.Builder(AlertDialogSamples.this, true).setBottomButtonSingleChoiceItems(
					R.array.select_dialog_items2, 0, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create(true);
			return qdialog;
		case DIALOG_BOTTOM:
			int colors[] = { AlertDialog.BOTTOM_BUTTON_COLOR_BLACK, AlertDialog.BOTTOM_BUTTON_COLOR_BLUE,
					AlertDialog.BOTTOM_BUTTON_COLOR_RED, AlertDialog.BOTTOM_BUTTON_COLOR_BLUE };

			qdialog = new AlertDialog.Builder(AlertDialogSamples.this, true).setTitle("bottom dialog titles")
					.setBottomButtonItems(R.array.select_dialog_items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							/* User clicked so do some stuff */
							String[] items = getResources().getStringArray(R.array.select_dialog_items);
							new AlertDialog.Builder(AlertDialogSamples.this).setMessage(
									"You selected: " + which + " , " + items[which]).show();
						}
					}).setBottomButtonColorItems(colors).create(true);
			qdialog.setTitleTextColor(Color.RED);
			return qdialog;
		case DIALOG_NOT_DISMISS:
			qdialog = new AlertDialog.Builder(AlertDialogSamples.this)
					.setPositiveButton("强制取消", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							qdialog.setTwsDismissDialog(true);
						}
					}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create();
			qdialog.setTwsDismissDialog(false);

			return qdialog;
		case DIALOG_YES_NO_MESSAGE:
			ListView lv = (ListView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.listview, null,
					false);
			dataList = getResources().getStringArray(R.array.date);
			lv.setAdapter(new MyAdapter());
			return new AlertDialog.Builder(AlertDialogSamples.this).setView(lv)
					.setPositiveButton(R.string.alert_dialog_two_buttons_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					})
					.setNegativeButton(R.string.alert_dialog_two_buttons_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create();
		case DIALOG_YES_NO_OLD_SCHOOL_MESSAGE:
			return new AlertDialog.Builder(AlertDialogSamples.this)
					.setMessage(R.string.alert_dialog_two_buttons_old_school_msg)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					}).create();
		case DIALOG_YES_NO_HOLO_LIGHT_MESSAGE:
			CheckBoxDialog cbd = new CheckBoxDialog(AlertDialogSamples.this);
			cbd.setTitle(R.string.alert_dialog_two_buttons_holo_light_title);
			cbd.setMessage(getString(R.string.alert_dialog_two_buttons_holo_light_msg));
			cbd.setCheckBoxMsg(getString(R.string.alert_dialog_two_buttons_holo_light_sub_msg));
			cbd.setButton(getString(R.string.alert_dialog_two_buttons_holo_light_ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			cbd.setButton2(getString(R.string.alert_dialog_two_buttons_holo_light_cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			return cbd;

		case DIALOG_YES_NO_LONG_MESSAGE:
			return new AlertDialog.Builder(AlertDialogSamples.this).setIconAttribute(android.R.attr.alertDialogIcon)
					.setTitle(R.string.alert_dialog_two_buttons_msg).setMessage(R.string.alert_dialog_two_buttons2_msg)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked OK so do some stuff */
						}
					}).setNeutralButton(R.string.alert_dialog_something, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Something so do some stuff */
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					}).create();
		case DIALOG_YES_NO_ULTRA_LONG_MESSAGE:
			return new AlertDialog.Builder(AlertDialogSamples.this).setIconAttribute(android.R.attr.alertDialogIcon)
					.setTitle(R.string.alert_dialog_two_buttons_msg)
					.setMessage(R.string.alert_dialog_two_buttons2ultra_msg)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked OK so do some stuff */
						}
					}).setNeutralButton(R.string.alert_dialog_something, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Something so do some stuff */
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					}).create();
		case DIALOG_LIST:
			return new AlertDialog.Builder(AlertDialogSamples.this).setTitle(R.string.select_dialog)
					.setItems(R.array.select_dialog_items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							/* User clicked so do some stuff */
							String[] items = getResources().getStringArray(R.array.select_dialog_items);
							new AlertDialog.Builder(AlertDialogSamples.this).setMessage(
									"You selected: " + which + " , " + items[which]).show();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Cancel so do some stuff */
						}
					}).create();
		case DIALOG_PROGRESS:
			mProgressDialog = new ProgressDialog(AlertDialogSamples.this);
			mProgressDialog.setIconAttribute(android.R.attr.alertDialogIcon);
			mProgressDialog.setTitle(R.string.select_dialog);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMax(MAX_PROGRESS);
			mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(R.string.alert_dialog_hide),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Yes so do some stuff */
						}
					});
			mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getText(R.string.alert_dialog_cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked No so do some stuff */
						}
					});
			return mProgressDialog;
		case DIALOG_SINGLE_CHOICE:
			return new AlertDialog.Builder(AlertDialogSamples.this).setIconAttribute(android.R.attr.alertDialogIcon)
					.setTitle(R.string.alert_dialog_single_choice)
					.setSingleChoiceItems(R.array.select_dialog_items2, 0, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked on a radio button do some stuff */
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked No so do some stuff */
						}
					}).create();
		case DIALOG_MULTIPLE_CHOICE:
			return new AlertDialog.Builder(AlertDialogSamples.this)
					.setIcon(R.drawable.ic_popup_reminder)
					.setTitle(R.string.alert_dialog_multi_choice)
					.setMultiChoiceItems(R.array.select_dialog_items3,
							new boolean[] { false, true, false, true, false, false, false },
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {

									/* User clicked on a check box do some stuff */
								}
							}).setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked Yes so do some stuff */
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

							/* User clicked No so do some stuff */
						}
					}).create();
		case DIALOG_MULTIPLE_CHOICE_CURSOR:
			String[] projection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME,
					ContactsContract.Contacts.SEND_TO_VOICEMAIL };
			Cursor cursor = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
			return new AlertDialog.Builder(AlertDialogSamples.this)
					.setIcon(R.drawable.ic_popup_reminder)
					.setTitle(R.string.alert_dialog_multi_choice_cursor)
					.setMultiChoiceItems(cursor, ContactsContract.Contacts.SEND_TO_VOICEMAIL,
							ContactsContract.Contacts.DISPLAY_NAME, new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
									Toast.makeText(AlertDialogSamples.this,
											"Readonly Demo Only - Data will not be updated", Toast.LENGTH_SHORT).show();
								}
							}).create();
		case DIALOG_TEXT_ENTRY:
			EditTextDialog dialog2 = new EditTextDialog(AlertDialogSamples.this);
			dialog2.setTitle("edittext dialog");
			dialog2.setMessage("msg");
			dialog2.setEditTextText("hello");
			return dialog2;
		}
		return null;
	}

	/**
	 * Initialization of the Activity after it is first created. Must at least
	 * call {@link android.app.Activity#setContentView(int)} to describe what is
	 * to be displayed in the screen.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.alert_dialog);

		getTwsActionBar().setTitle("Alert对话框");
		/*
		 * Display a text message with yes/no buttons and handle each message as
		 * well as the cancel action
		 */
		Button twoButtonsTitle = (Button) findViewById(R.id.two_buttons);
		twoButtonsTitle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_YES_NO_MESSAGE);
			}
		});

		/*
		 * Display a long text message with yes/no buttons and handle each
		 * message as well as the cancel action
		 */
		Button twoButtons2Title = (Button) findViewById(R.id.two_buttons2);
		twoButtons2Title.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_YES_NO_LONG_MESSAGE);
			}
		});

		/*
		 * Display an ultra long text message with yes/no buttons and handle
		 * each message as well as the cancel action
		 */
		Button twoButtons2UltraTitle = (Button) findViewById(R.id.two_buttons2ultra);
		twoButtons2UltraTitle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_YES_NO_ULTRA_LONG_MESSAGE);
			}
		});

		/* Display a list of items */
		Button selectButton = (Button) findViewById(R.id.select_button);
		selectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_LIST);
			}
		});

		/* Display a custom progress bar */
		Button progressButton = (Button) findViewById(R.id.progress_button);
		progressButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_PROGRESS);
				mProgress = 0;
				mProgressDialog.setProgress(0);
				mProgressHandler.sendEmptyMessage(0);
			}
		});

		/* Display a radio button group */
		Button radioButton = (Button) findViewById(R.id.radio_button);
		radioButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_SINGLE_CHOICE);
			}
		});

		/* Display a list of checkboxes */
		Button checkBox = (Button) findViewById(R.id.checkbox_button);
		checkBox.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_MULTIPLE_CHOICE);
			}
		});

		/* Display a list of checkboxes, backed by a cursor */
		Button checkBox2 = (Button) findViewById(R.id.checkbox_button2);
		checkBox2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_MULTIPLE_CHOICE_CURSOR);
			}
		});

		/* Display a text entry dialog */
		Button textEntry = (Button) findViewById(R.id.text_entry_button);
		textEntry.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// showTwsDialog(DIALOG_TEXT_ENTRY);
				EditTextDialog dialog2 = new EditTextDialog(AlertDialogSamples.this);
				dialog2.setTitle("edittext dialog");
				dialog2.setMessage("msg");
				// dialog2.setEditTextText("hello2");
				dialog2.show();
				Window window = dialog2.getWindow();
				LayoutParams params = window.getAttributes();
				params.softInputMode = LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;// 显示dialog的时候,就显示软键盘
				window.setAttributes(params);
				dialog2.getEditText().setText("hello22");
				dialog2.getEditText().setFocusable(true);
				dialog2.getEditText().requestFocus();
				dialog2.getEditText().selectAll();
				// dialog2.getEditText().setSelection(0, 5);
			}
		});

		/* Two points, in the traditional theme */
		Button twoButtonsOldSchoolTitle = (Button) findViewById(R.id.two_buttons_old_school);
		twoButtonsOldSchoolTitle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_YES_NO_OLD_SCHOOL_MESSAGE);
			}
		});

		/* Two points, in the light holographic theme */
		Button twoButtonsHoloLightTitle = (Button) findViewById(R.id.two_buttons_holo_light);
		twoButtonsHoloLightTitle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_YES_NO_HOLO_LIGHT_MESSAGE);
			}
		});

		Button btNotDismiss = (Button) findViewById(R.id.alert_dialog_not_dismiss);
		btNotDismiss.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_NOT_DISMISS);
			}
		});
		Button btnBottom = (Button) findViewById(R.id.alert_dialog_bottom);
		btnBottom.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_BOTTOM);
			}
		});
		Button btnBottomSingle = (Button) findViewById(R.id.alert_dialog_bottom_single);
		btnBottomSingle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_BOTTOM_SINGLE);
			}
		});
		Button btnBottomMult = (Button) findViewById(R.id.alert_dialog_bottom_mult);
		btnBottomMult.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_BOTTOM_MULT);
			}
		});
		Button btnBottomButton = (Button) findViewById(R.id.alert_dialog_bottom_botton);
		btnBottomButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwsDialog(DIALOG_BOTTOM_BUTTON);
			}
		});

		mProgressHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (mProgress >= MAX_PROGRESS) {
					mProgressDialog.dismiss();
				} else {
					mProgress++;
					mProgressDialog.incrementProgressBy(1);
					mProgressHandler.sendEmptyMessageDelayed(0, 100);
				}
			}
		};

	}

	/*
	 * @Override protected void onPrepareTwsDialog(int id, TwsDialog dialog) {
	 * super.onPrepareTwsDialog(id, dialog); switch (id) { case
	 * DIALOG_TEXT_ENTRY: ((EditTextDialog)
	 * dialog).getEditText().setFocusable(true); ((EditTextDialog)
	 * dialog).getEditText().requestFocus(); ((EditTextDialog)
	 * dialog).getEditText().setSelection(0, 5); break;
	 * 
	 * default: break; }
	 * 
	 * }
	 */
}
