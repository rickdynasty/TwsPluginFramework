
package com.tencent.tws.assistant.animation.ease;


public class BackEaseIn extends BaseEase {

    private float s = 1.70158f;

    public BackEaseIn(float duration) {
        super(duration);
    }

    public BackEaseIn(float duration, float back) {
        this(duration);
        s = back;
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
    }
}
