package com.example.plugindemo.fragment;

import android.app.TwsActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.tencent.tws.assistant.support.v4.app.Fragment;
import com.tencent.tws.framework.HostProxy;

/**
 * 此fragment使用了特定的context,因此可以在在插件中的activity，或者宿主中的特定activity、
 * 或者宿主中的非特定activity中展示
 */

public class PluginSpecTwsFragment extends Fragment implements OnClickListener {

	private ViewGroup mRoot;
	private Context pluginContext;
	private LayoutInflater pluginInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 默认是宿主程序Application主题
		try {
			pluginContext = getActivity().createPackageContext("com.example.plugindemo", 0);
			pluginInflater = (LayoutInflater) pluginContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View scrollview = pluginInflater.inflate(R.layout.plugin_layout, null);

		mRoot = (ViewGroup) scrollview.findViewById(R.id.content);
		boolean hasOverlayActionbar = getActivity().getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		if (hasOverlayActionbar) {
			int top = (int) getResources().getDimension(HostProxy.getTwsActionBarHeightID());
			if (getActivity() instanceof TwsActivity) {
				top += TwsActivity.getStatusBarHeight();
			}
			mRoot.setPadding(0, top, 0, 0);
		}

		initViews();

		return scrollview;
	}

	public void initViews() {

		Button btn1 = (Button) mRoot.findViewById(R.id.plugin_test_btn1);
		btn1.setOnClickListener(this);

		Button btn2 = (Button) mRoot.findViewById(R.id.plugin_test_btn2);
		btn2.setOnClickListener(this);

		Button btn3 = (Button) mRoot.findViewById(R.id.plugin_test_btn3);
		btn3.setOnClickListener(this);

		Button btn4 = (Button) mRoot.findViewById(R.id.plugin_test_btn4);
		btn4.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		Log.v("v.click MainFragment", "" + v.getId());
		if (v.getId() == R.id.plugin_test_btn1) {
			View view = pluginInflater.inflate(R.layout.plugin_layout, null, false);
			mRoot.addView(view);
			Toast.makeText(this.getActivity(), pluginContext.getString(R.string.hello_world1), Toast.LENGTH_SHORT)
					.show();
		} else if (v.getId() == R.id.plugin_test_btn2) {
			View view = pluginInflater.inflate(HostProxy.getShareLayoutId("share_main"), null, false);
			mRoot.addView(view);
			Toast.makeText(this.getActivity(), getString(HostProxy.getShareStringId("share_string_1")),
					Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn3) {
			View view = LayoutInflater.from(getActivity()).inflate(HostProxy.getShareLayoutId("share_main"), null,
					false);
			mRoot.addView(view);
		} else if (v.getId() == R.id.plugin_test_btn4) {
			((Button) v).setText(HostProxy.getShareStringId("share_string_2"));
		}
	}
}
