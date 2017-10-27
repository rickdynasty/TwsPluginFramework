package com.tencent.tws.sharelib.util;

public interface HomeUIProxy {
	public void switchToFragment(String classId, int extras);

	public void setHighlightCellItem(String classId, boolean needHighlight);
}
