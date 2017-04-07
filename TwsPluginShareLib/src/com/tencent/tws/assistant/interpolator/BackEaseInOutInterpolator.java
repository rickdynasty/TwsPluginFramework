package com.tencent.tws.assistant.interpolator;

public class BackEaseInOutInterpolator extends BaseInterpolator {

    private float s = 1.70158f;

    public BackEaseInOutInterpolator() {
    }
    
    public BackEaseInOutInterpolator(float back) {
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        float s1 = s;
        if ((t /= d / 2) < 1)
            return c / 2 * (t * t * (((s1 *= (1.525)) + 1) * t - s1)) + b;
        return c / 2 * ((t -= 2) * t * (((s1 *= (1.525)) + 1) * t + s1) + 2) + b;
    }
}
