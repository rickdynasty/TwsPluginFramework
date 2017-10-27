
package com.tencent.tws.assistant.animation.ease;

public class SineEaseOut extends BaseEase {

    public SineEaseOut(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return -c * (float) Math.cos(t / d * (Math.PI / 2)) + c + b;
    }
}
