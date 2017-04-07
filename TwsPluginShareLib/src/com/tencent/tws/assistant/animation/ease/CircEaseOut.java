
package com.tencent.tws.assistant.animation.ease;


public class CircEaseOut extends BaseEase {

    public CircEaseOut(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return -c * ((float) Math.sqrt(1 - (t /= d) * t) - 1) + b;
    }
}
