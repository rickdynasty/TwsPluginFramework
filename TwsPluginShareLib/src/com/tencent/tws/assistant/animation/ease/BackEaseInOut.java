
package com.tencent.tws.assistant.animation.ease;

public class BackEaseInOut extends BaseEase {

    private float s = 1.70158f;

    public BackEaseInOut(float duration) {
        super(duration);
    }

    public BackEaseInOut(float duration, float back) {
        this(duration);
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        float s1 = s;
        if ((t /= d / 2) < 1) return c / 2 * (t * t * (((s1 *= (1.525)) + 1) * t - s1)) + b;
        return c / 2 * ((t -= 2) * t * (((s1 *= (1.525)) + 1) * t + s1) + 2) + b;
    }
}
