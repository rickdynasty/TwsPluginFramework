
package com.tencent.tws.assistant.animation.ease;

public class QuadEaseIn extends BaseEase {

    public QuadEaseIn(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return -c * (t /= d) * (t - 2) + b;
    }
}
