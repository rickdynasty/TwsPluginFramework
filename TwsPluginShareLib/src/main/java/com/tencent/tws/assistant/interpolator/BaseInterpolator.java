package com.tencent.tws.assistant.interpolator;

import android.view.animation.Interpolator;

public abstract class BaseInterpolator implements Interpolator{

    @Override
    public float getInterpolation(float input) {
        float t = 100 * input;
        float b = 0f;
        float c = 1.0f;
        float d = 100;
        return calculate(t, b, c, d);
    }

    public abstract Float calculate(float t, float b, float c, float d);
}
