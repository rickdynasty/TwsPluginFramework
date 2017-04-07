package android.widget;

import com.tencent.tws.assistant.support.v4.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TwsFactoryContentStrategy2FragmentV4 extends Fragment {
	
	private View mTabContent;
	
	public TwsFactoryContentStrategy2FragmentV4() {
	}
	
	public TwsFactoryContentStrategy2FragmentV4(View tabContent) {
		mTabContent = tabContent;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return mTabContent;
	}

}
