
package com.tencent.tws.assistant.animation.ease;

public class CubicEaseIn extends BaseEase {

    public CubicEaseIn(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * ((t = t / d - 1) * t * t + 1) + b;
    }
}
