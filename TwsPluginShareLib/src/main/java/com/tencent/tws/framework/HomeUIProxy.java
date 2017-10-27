package com.tencent.tws.framework;

import android.content.Context;

public interface HomeUIProxy {
	public void switchToFragment(String classId, int extras);

	public void setHighlightCellItem(String classId, boolean needHighlight);
	
	public Context getHostFitContext();
}
