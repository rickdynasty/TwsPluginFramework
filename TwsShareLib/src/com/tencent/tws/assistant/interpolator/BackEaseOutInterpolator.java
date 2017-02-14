
package com.tencent.tws.assistant.interpolator;

public class BackEaseOutInterpolator extends BaseInterpolator {

    private float s = 1.70158f;

    public BackEaseOutInterpolator() {
    }
    public BackEaseOutInterpolator(float back) {
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (t /= d) * t * ((s + 1) * t - s) + b;
    }
}
