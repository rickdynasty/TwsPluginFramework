package com.example.plugindemo.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.plugindemo.R;
import com.rick.tws.framework.HostProxy;

import qrom.component.log.QRomLog;

/**
 * 此fragment使用了特定的context,因此可以在在插件中的activity，或者宿主中的特定activity、
 * 或者宿主中的非特定activity中展示
 */

public class PluginSpecFragment extends Fragment implements OnClickListener {
    private static final String TAG = "rick_Print:PluginSpecFragment";

    private ViewGroup mRoot;
    private Context pluginContext;
    private LayoutInflater pluginInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QRomLog.i(TAG, "=========onCreate=========");

        if (null != getActivity().getActionBar()) {
            getActivity().getActionBar().setTitle("插件的 Spec Fragment");
        } else {
            getActivity().setTitle("插件的 Spec Fragment");
        }

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
        QRomLog.i(TAG, "=========onCreateView=========");

        View scrollview = pluginInflater.inflate(R.layout.plugin_layout, null);

        mRoot = (ViewGroup) scrollview.findViewById(R.id.content);
        mRoot.setBackgroundColor(Color.WHITE);

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
            Toast.makeText(this.getActivity(), pluginContext.getString(R.string.hello_world1), Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.plugin_test_btn2) {
            View view = pluginInflater.inflate(HostProxy.getShareLayoutId("share_main"), null, false);
            mRoot.addView(view);
            Toast.makeText(this.getActivity(), getString(HostProxy.getShareStringId("share_string_1")), Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.plugin_test_btn3) {
            View view = LayoutInflater.from(getActivity()).inflate(HostProxy.getShareLayoutId("share_main"), null, false);
            mRoot.addView(view);
        } else if (v.getId() == R.id.plugin_test_btn4) {
            ((Button) v).setText(HostProxy.getShareStringId("share_string_2"));
        }
    }

    @Override
    public void onStart() {
        QRomLog.i(TAG, "=========onStart=========");
        super.onStart();
    }

    @Override
    public void onResume() {
        QRomLog.i(TAG, "=========onResume=========");
        super.onResume();
    }

    @Override
    public void onPause() {
        QRomLog.i(TAG, "=========onPause=========");
        super.onPause();
    }

    @Override
    public void onStop() {
        QRomLog.i(TAG, "=========onStop=========");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        QRomLog.i(TAG, "=========onDestroyView=========");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        QRomLog.i(TAG, "=========onDestroy=========");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        QRomLog.i(TAG, "=========onDetach=========");
        super.onDetach();
    }
}
