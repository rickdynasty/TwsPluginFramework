
package com.tencent.tws.assistant.animation.ease;

public class BackEaseOut extends BaseEase {

    private float s = 1.70158f;

    public BackEaseOut(float duration) {
        super(duration);
    }

    public BackEaseOut(float duration, float back) {
        this(duration);
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (t /= d) * t * ((s + 1) * t - s) + b;
    }
}
