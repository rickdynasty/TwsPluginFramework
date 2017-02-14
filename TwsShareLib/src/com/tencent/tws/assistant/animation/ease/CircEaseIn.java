
package com.tencent.tws.assistant.animation.ease;

public class CircEaseIn extends BaseEase {

    public CircEaseIn(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (float) Math.sqrt(1 - (t = t / d - 1) * t) + b;
    }
}
