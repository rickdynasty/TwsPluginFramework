
package com.tencent.tws.assistant.interpolator;

public class SineEaseOutInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        return -c * (float) Math.cos(t / d * (Math.PI / 2)) + c + b;
    }
}
