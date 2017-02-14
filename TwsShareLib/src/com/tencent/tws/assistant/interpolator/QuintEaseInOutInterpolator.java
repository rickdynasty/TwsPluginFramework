
package com.tencent.tws.assistant.interpolator;

public class QuintEaseInOutInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        if ((t /= d / 2) < 1) return c / 2 * t * t * t * t * t + b;
        return c / 2 * ((t -= 2) * t * t * t * t + 2) + b;
    }
}
