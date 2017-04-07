
package com.tencent.tws.assistant.animation.ease;

public class SineEaseIn extends BaseEase {

    public SineEaseIn(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (float) Math.sin(t / d * (Math.PI / 2)) + b;
    }
}
