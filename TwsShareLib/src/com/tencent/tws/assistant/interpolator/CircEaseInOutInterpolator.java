
package com.tencent.tws.assistant.interpolator;

public class CircEaseInOutInterpolator extends BaseInterpolator {

    @Override
    public Float calculate(float t, float b, float c, float d) {
        if ((t /= d / 2) < 1) {
            return -c / 2 * ((float) Math.sqrt(1 - t * t) - 1) + b;
        }

        return c / 2 * ((float) Math.sqrt(1 - (t -= 2) * t) + 1) + b;
    }
}
