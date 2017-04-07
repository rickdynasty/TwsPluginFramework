
package com.tencent.tws.assistant.interpolator;

public class CubicEaseInInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return c * ((t = t / d - 1) * t * t + 1) + b;
    }
}
