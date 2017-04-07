
package com.tencent.tws.assistant.animation.ease;

public class ExpoEaseOut extends BaseEase {

    public ExpoEaseOut(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return (t == 0) ? b : c * (float) Math.pow(2, 10 * (t / d - 1)) + b;
    }
}
