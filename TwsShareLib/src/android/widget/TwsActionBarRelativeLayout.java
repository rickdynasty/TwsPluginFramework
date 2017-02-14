package android.widget;

import android.content.Context;
import android.widget.RelativeLayout;
import android.util.AttributeSet;
import android.graphics.Rect;

public class TwsActionBarRelativeLayout extends RelativeLayout {
	public TwsActionBarRelativeLayout(Context context) {
		super(context);
	}
	
	public TwsActionBarRelativeLayout(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}
	
	protected boolean fitSystemWindows(Rect insets) {
		insets.top = 0;
		super.fitSystemWindows(insets);
    	return true;
    }

}