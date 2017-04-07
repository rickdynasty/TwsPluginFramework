
package com.tencent.tws.assistant.interpolator;

public class CircEaseInInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * (float) Math.sqrt(1 - (t = t / d - 1) * t) + b;
    }
}
