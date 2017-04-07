
package com.tencent.tws.assistant.animation.ease;

public class BounceEaseOut extends BaseEase {

    private BounceEaseIn mBounceEaseIn;

    public BounceEaseOut(float duration) {
        super(duration);
        mBounceEaseIn = new BounceEaseIn(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c - mBounceEaseIn.calculate(d - t, 0, c, d) + b;
    }
}
