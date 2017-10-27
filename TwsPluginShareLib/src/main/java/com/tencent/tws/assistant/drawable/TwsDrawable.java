package com.tencent.tws.assistant.drawable;

import android.graphics.drawable.Drawable;
import android.graphics.Rect;

public abstract class TwsDrawable extends Drawable {
    //tws-start rippleDrawable hotSpot::2014-11-28
    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public void twsSetHotspot(float x, float y) {}

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void twsSetHotspotBounds(int left, int top, int right, int bottom) {}

    /** @hide For internal use only. Individual results may vary. */
    public void twsGetHotspotBounds(Rect outRect) {
        outRect.set(getBounds());
    }

    public Rect twsGetDirtyBounds() {
        return getBounds();
    }

    public int twsGetAlpha() {
        return 0xFF;
    }
    //tws-end rippleDrawable hotSpot::2014-11-28
}
