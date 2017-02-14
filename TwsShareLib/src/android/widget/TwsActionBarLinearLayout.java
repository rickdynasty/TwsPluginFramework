package android.widget;

import com.tencent.tws.sharelib.R;

import android.content.Context;
import android.widget.LinearLayout;
import android.util.AttributeSet;
import android.graphics.Rect;

public class TwsActionBarLinearLayout extends LinearLayout {
	public TwsActionBarLinearLayout(Context context) {
		super(context);
	}
	
	public TwsActionBarLinearLayout(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}
	
	protected boolean fitSystemWindows(Rect insets) {
		if (android.os.Build.VERSION.SDK_INT > 18 && getResources().getBoolean(R.bool.config_statusbar_state)) {
			insets.top = 0;
		}
		super.fitSystemWindows(insets);
    	return true;
    }

}