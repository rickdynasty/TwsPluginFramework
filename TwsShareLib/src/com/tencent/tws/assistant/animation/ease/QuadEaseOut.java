
package com.tencent.tws.assistant.animation.ease;

public class QuadEaseOut extends BaseEase {

    public QuadEaseOut(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (t /= d) * t + b;
    }
}
