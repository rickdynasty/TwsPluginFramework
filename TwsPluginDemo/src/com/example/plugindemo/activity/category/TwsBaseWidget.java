package com.example.plugindemo.activity.category;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.app.TwsActivity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TwsSearchView;
import android.widget.TwsSearchView.onCancelClickListener;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.app.AlertDialog;
import com.tencent.tws.assistant.app.TwsDialog;
import com.tencent.tws.assistant.utils.TwsRippleUtils;
import com.tencent.tws.assistant.widget.AutoCompleteTextView;
import com.tencent.tws.assistant.widget.CheckBox;
import com.tencent.tws.assistant.widget.RadioButton;
import com.tencent.tws.assistant.widget.RadioGroup;
import com.tencent.tws.assistant.widget.SeekBar;
import com.tencent.tws.assistant.widget.Switch;
import com.tencent.tws.assistant.widget.Toast;
import com.tencent.tws.framework.HostProxy;

public class TwsBaseWidget extends TwsActivity {

	private Button btnDialog, btnToast, btnPopMenu;
	private TwsSearchView titaSearchView;
	private EditText mEditText;

	static final String[] COUNTRIES = new String[] { // 这里用一个字符串数组来当数据匹配源
	"Afghanistan", "Albania", "Algeria", "American Samoa", "Andorra", "Angola", "Anguilla", "Antarctica",
			"Antigua and Barbuda", "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan", "Bahrain",
			"Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia",
			"Bosnia and Herzegovina", "Botswana", "Bouvet Island", "Brazil", "British Indian Ocean Territory",
			"British Virgin Islands", "Brunei", "Bulgaria", "Burkina Faso", "Burundi", "Cote d'Ivoire", "Cambodia",
			"Cameroon", "Canada", "Cape Verde", "Cayman Islands", "Central African Republic", "Chad", "Chile", "China",
			"Christmas Island", "Cocos (Keeling) Islands", "Colombia", "Comoros", "Congo", "Cook Islands",
			"Costa Rica", "Croatia", "Cuba", "Cyprus", "Czech Republic", "Democratic Republic of the Congo", "Denmark",
			"Djibouti", "Dominica", "Dominican Republic", "East Timor", "Ecuador", "Egypt", "El Salvador",
			"Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia", "Faeroe Islands", "Falkland Islands", "Fiji",
			"Finland", "Former Yugoslav Republic of Macedonia", "France", "French Guiana", "French Polynesia",
			"French Southern Territories", "Gabon", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland",
			"Grenada", "Guadeloupe", "Guam", "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti",
			"Heard Island and McDonald Islands", "Honduras", "Hong Kong", "Hungary", "Iceland", "India", "Indonesia",
			"Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya",
			"Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya",
			"Liechtenstein", "Lithuania", "Luxembourg", "Macau", "Madagascar", "Malawi", "Malaysia", "Maldives",
			"Mali", "Malta", "Marshall Islands", "Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico",
			"Micronesia", "Moldova", "Monaco", "Mongolia", "Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia",
			"Nauru", "Nepal", "Netherlands", "Netherlands Antilles", "New Caledonia", "New Zealand", "Nicaragua",
			"Niger", "Nigeria", "Niue", "Norfolk Island", "North Korea", "Northern Marianas", "Norway", "Oman",
			"Pakistan", "Palau", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn Islands",
			"Poland", "Portugal", "Puerto Rico", "Qatar", "Reunion", "Romania", "Russia", "Rwanda",
			"Sqo Tome and Principe", "Saint Helena", "Saint Kitts and Nevis", "Saint Lucia",
			"Saint Pierre and Miquelon", "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Saudi Arabia",
			"Senegal", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia",
			"South Africa", "South Georgia and the South Sandwich Islands", "South Korea", "Spain", "Sri Lanka",
			"Sudan", "Suriname", "Svalbard and Jan Mayen", "Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan",
			"Tajikistan", "Tanzania", "Thailand", "The Bahamas", "The Gambia", "Togo", "Tokelau", "Tonga",
			"Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Turks and Caicos Islands", "Tuvalu",
			"Virgin Islands", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States",
			"United States Minor Outlying Islands", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City", "Venezuela",
			"Vietnam", "Wallis and Futuna", "Western Sahara", "Yemen", "Yugoslavia", "Zambia", "Zimbabwe" };

	public void onPopupButtonClick(View button) {
		PopupMenu popup = new PopupMenu(this, button);
		popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());

		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Toast.makeText(TwsBaseWidget.this, "Clicked popup menu item " + item.getTitle(), Toast.LENGTH_SHORT)
						.show();
				return true;
			}
		});

		popup.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_base_widget);

		getTwsActionBar().setTitle("基础控件");

		btnDialog = (Button) findViewById(R.id.btnDialog);
		btnToast = (Button) findViewById(R.id.btnToast);
		mEditText = (EditText) findViewById(R.id.editText1);
		titaSearchView = (TwsSearchView) findViewById(R.id.twssearch);
		// setOpenFoucusable()是否打开默认焦点，true：获取焦点，false:失去焦点
		// titaSearchView.setOpenFoucusable(true);
		titaSearchView.setOnCancelClickListener(new onCancelClickListener() {

			@Override
			public void onClick(boolean isCancel) {
				// TODO Auto-generated method stub
				Log.d("hlx", "isCancel = " + isCancel);
			}
		});

		btnPopMenu = (Button) findViewById(R.id.btnPopMenu);
		testReflect();

		btnPopMenu.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onPopupButtonClick(v);

			}
		});

		btnDialog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Toast.makeText(TwsBaseWidget.this, TwsBaseWidget.this.getString(R.string.app_name), Toast.LENGTH_SHORT)
						.show();
				TwsDialog mDialog = new AlertDialog.Builder(TwsBaseWidget.this)
						.setTitle(getString(R.string.preference_common_dialogtittle))
						.setMessage(getString(R.string.preference_common_summary))
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
				Window window = mDialog.getWindow();
				window.setGravity(Gravity.BOTTOM);
				// window.setWindowAnimations(com.tencent.tws.sharelib.R.style.Animation_tws_Dialog_Bottom);
				mDialog.show();

			}
		});

		btnToast.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// Toast.makeText(TwsBaseWidget.this, getString(R.string.Toast),
				// Toast.LENGTH_SHORT).show();
				new AlertDialog.Builder(TwsBaseWidget.this)
						.setTitle(getString(R.string.preference_common_dialogtittle))
						.setMessage(getString(R.string.preference_common_summary))
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).setNegativeButton(android.R.string.cancel, null).create().show();
			}
		});

		final Drawable d = getResources().getDrawable(R.drawable.search_custom_background);
		titaSearchView.setBackground(d);

		titaSearchView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				titaSearchView.setActivated(true);
				if (d.getCurrent() instanceof TransitionDrawable) {
					((TransitionDrawable) d.getCurrent()).startTransition(1000);
				}
			}
		});

		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);// 定义AutoCompleteTextView控件
		ArrayAdapter adapter = new ArrayAdapter(this, // 定义匹配源的adapter
				android.R.layout.simple_dropdown_item_1line, COUNTRIES);
		textView.setAdapter(adapter); // 设置 匹配源的adapter 到 AutoCompleteTextView控件

		Switch switch1 = (Switch) findViewById(R.id.switchbutton);
		final Switch switch2 = (Switch) findViewById(R.id.switchbutton2);

		switch1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				switch2.setEnabled(isChecked);
			}
		});
		Button button = (Button) findViewById(R.id.showdialogbtn);
		button.setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(this));
		Button button2 = (Button) findViewById(R.id.button2);
		button2.setBackgroundDrawable(TwsRippleUtils.getDefaultDrawable(this));
		CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox2);
		checkBox.setVisibility(View.VISIBLE);
		CheckBox checkBox3 = (CheckBox) findViewById(R.id.checkbox3);
		checkBox3.setTintButtonDrawable(R.color.base_widget_checkbox);

		RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroup);
		rg.setVisibility(View.VISIBLE);
		RadioButton rb1 = (RadioButton) findViewById(R.id.radioButton1);
		RadioButton rb2 = (RadioButton) findViewById(R.id.radioButton2);
		RadioButton rb3 = (RadioButton) findViewById(R.id.radioButton3);
		rb1.setVisibility(View.VISIBLE);
		rb2.setVisibility(View.VISIBLE);
		rb3.setVisibility(View.VISIBLE);

		SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar1);
		seekBar.setThumb(getResources().getDrawable(R.drawable.seek_thumb), R.color.green);
		seekBar.setProgressDrawable(
				getResources().getDrawable(HostProxy.getShareDrawableId("seekbar_progress_holo_light")), R.color.red,
				R.color.yellow);
	}

	public void testReflect() {
		try {
			Class c = Class.forName("android.app.TwsDialog");
			Class[] pType = new Class[] { Class.forName("android.content.Context"), int.class, boolean.class };
			Constructor ctor = c.getConstructor(pType);
			Object[] obj = new Object[] { TwsBaseWidget.this, 0, false };
			ctor.setAccessible(true);
			AlertDialog dialog = (AlertDialog) ctor.newInstance(obj);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
