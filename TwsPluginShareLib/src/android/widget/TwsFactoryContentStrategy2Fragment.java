package android.widget;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TwsFactoryContentStrategy2Fragment extends Fragment {
	
	private View mTabContent;
	
	public TwsFactoryContentStrategy2Fragment() {
	}
	
	public TwsFactoryContentStrategy2Fragment(View tabContent) {
		mTabContent = tabContent;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return mTabContent;
	}

}
