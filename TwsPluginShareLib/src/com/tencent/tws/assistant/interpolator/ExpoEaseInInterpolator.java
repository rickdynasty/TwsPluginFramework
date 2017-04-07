
package com.tencent.tws.assistant.interpolator;

public class ExpoEaseInInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return (t == d) ? b + c : c * (-(float) Math.pow(2, -10 * t / d) + 1) + b;
    }
}
