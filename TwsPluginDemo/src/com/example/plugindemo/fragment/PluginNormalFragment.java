package com.example.plugindemo.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.tencent.tws.framework.HostProxy;

/**
 * 此fragment没有使用特定的context,因此只能在插件中的activity，或者宿主中的特定activity中展示
 */
public class PluginNormalFragment extends Fragment implements OnClickListener {

	private ViewGroup mRoot;
	private LayoutInflater mInflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		getActivity().setTitle("测试插件中的Fragment，使用插件默认主题");

		mInflater = inflater;
		View scrollview = mInflater.inflate(R.layout.plugin_layout, null);

		mRoot = (ViewGroup) scrollview.findViewById(R.id.content);

		initViews();

		return scrollview;
	}

	public void initViews() {

		Button btn1 = (Button) mRoot.findViewById(R.id.plugin_test_btn1);
		btn1.setOnClickListener(this);
		btn1.setText("点击 添加plugin_layout视图");

		Button btn2 = (Button) mRoot.findViewById(R.id.plugin_test_btn2);
		btn2.setOnClickListener(this);
		btn2.setText("点击 添加share_main视图");

		Button btn3 = (Button) mRoot.findViewById(R.id.plugin_test_btn3);
		btn3.setOnClickListener(this);

		Button btn4 = (Button) mRoot.findViewById(R.id.plugin_test_btn4);
		btn4.setOnClickListener(this);
		btn4.setText("点击 设置共享资源文本");

	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.plugin_test_btn1) {
			View view = mInflater.inflate(R.layout.plugin_layout, null, false);
			mRoot.addView(view);
			Toast.makeText(this.getActivity(), getString(R.string.hello_world1), Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn2) {
			View view = mInflater.inflate(HostProxy.getShareLayoutId("share_main"), null, false);
			mRoot.addView(view);
			Toast.makeText(this.getActivity(), getString(HostProxy.getShareStringId("share_string_1")),
					Toast.LENGTH_SHORT).show();
		} else if (v.getId() == R.id.plugin_test_btn3) {
			View view = LayoutInflater.from(getActivity()).inflate(HostProxy.getShareLayoutId("share_main"), null,
					false);
			mRoot.addView(view);
		} else if (v.getId() == R.id.plugin_test_btn4) {
			((Button) v).setText(HostProxy.getShareStringId("share_string_1"));
		}
	}
}
