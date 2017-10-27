
package com.tencent.tws.assistant.animation.ease;

import android.animation.TypeEvaluator;

public abstract class BaseEase implements TypeEvaluator<Number> {

    protected float mDuration;

    public BaseEase(float duration) {
        mDuration = duration;
    }

    public void setDuration(float duration) {
        mDuration = duration;
    }

    @Override
    public final Float evaluate(float fraction, Number startValue, Number endValue) {
        float t = mDuration * fraction;
        float b = startValue.floatValue();
        float c = endValue.floatValue() - startValue.floatValue();
        float d = mDuration;
        return calculate(t, b, c, d);
    }

    public abstract Float calculate(float t, float b, float c, float d);
}
