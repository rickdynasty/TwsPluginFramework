
package com.tencent.tws.assistant.animation.ease;


public class BounceEaseInOut extends BaseEase {

    private BounceEaseOut mBounceEaseOut;
    private BounceEaseIn mBounceEaseIn;

    public BounceEaseInOut(float duration) {
        super(duration);
        mBounceEaseOut = new BounceEaseOut(duration);
        mBounceEaseIn = new BounceEaseIn(duration);
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
