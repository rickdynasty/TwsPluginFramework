
package com.tencent.tws.assistant.animation.ease;

public class QuintEaseOut extends BaseEase {

    public QuintEaseOut(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (t /= d) * t * t * t * t + b;
    }
}
