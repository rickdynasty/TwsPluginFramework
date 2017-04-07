
package com.tencent.tws.assistant.interpolator;

public class SineEaseInOutInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return -c / 2 * ((float) Math.cos(Math.PI * t / d) - 1) + b;
    }
}
