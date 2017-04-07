
package com.tencent.tws.assistant.interpolator;

public class BounceEaseOutInterpolator extends BaseInterpolator {

    private BounceEaseInInterpolator mBounceEaseIn;

    public BounceEaseOutInterpolator() {
        mBounceEaseIn = new BounceEaseInInterpolator();
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c - mBounceEaseIn.calculate(d - t, 0, c, d) + b;
    }
}
