
package com.tencent.tws.assistant.animation.ease;

public class ExpoEaseIn extends BaseEase {

    public ExpoEaseIn(float duration) {
        super(duration);
    }

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return (t == d) ? b + c : c * (-(float) Math.pow(2, -10 * t / d) + 1) + b;
    }
}
