package com.example.plugindemo.activity.category.tab;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.tws.assistant.support.v4.app.Fragment;

/**
 *  放在viewPager中的内容页，必须使用外部类，不能在viewPagerActivity页面写内部类，
 *  必须包含一个空的public构造函数，如果需要传入参数，请使用下面单例方式通过bundle传入
 * 
 */
public class ViewPagerFragment extends Fragment {
    public ViewPagerFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setGravity(Gravity.CENTER);
        TextView tv=new TextView(getActivity());
        tv.setTextSize(40);
        ll.addView(tv);
        tv.setText(getArguments().getString("tagStr"));
        return ll;
    }

    public static ViewPagerFragment newInstance(String tag) {
        ViewPagerFragment myFragment = new ViewPagerFragment();
        Bundle args = new Bundle();
        args.putString("tagStr", tag);
        myFragment.setArguments(args);
        return myFragment;
    }
}
