package com.tencent.tws.assistant.interpolator;

public class BackEaseInInterpolator extends BaseInterpolator {

    private float s = 1.70158f;

    public BackEaseInInterpolator() {
    }

    public BackEaseInInterpolator(float back) {
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
    }
}
