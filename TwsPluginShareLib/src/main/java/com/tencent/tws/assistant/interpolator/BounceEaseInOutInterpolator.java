
package com.tencent.tws.assistant.interpolator;


public class BounceEaseInOutInterpolator extends BaseInterpolator {

    private BounceEaseOutInterpolator mBounceEaseOut;
    private BounceEaseInInterpolator mBounceEaseIn;

    public BounceEaseInOutInterpolator() {
        mBounceEaseOut = new BounceEaseOutInterpolator();
        mBounceEaseIn = new BounceEaseInInterpolator();
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        if (t < (d / 2)) {
            return mBounceEaseOut.calculate(t * 2, 0, c, d) * .5f + b;
        } else {
            return mBounceEaseIn.calculate(t * 2 - d, 0, c, d) * .5f + c * .5f + b;
        }
    }
}
