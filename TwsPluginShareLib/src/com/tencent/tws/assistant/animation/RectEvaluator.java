
package com.tencent.tws.assistant.animation;

import android.animation.TypeEvaluator;
import android.graphics.Rect;

/**
 * This evaluator can be used to perform type interpolation between <code>Rect</code> values.
 */
public class RectEvaluator implements TypeEvaluator<Rect> {

    /**
     * This function returns the result of linearly interpolating the start and
     * end Rect values, with <code>fraction</code> representing the proportion
     * between the start and end values. The calculation is a simple parametric
     * calculation on each of the separate components in the Rect objects
     * (left, top, right, and bottom).
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue The start Rect
     * @param endValue   The end Rect
     * @return A linear interpolation between the start and end values, given the
     * <code>fraction</code> parameter.
     */
    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
        return new Rect(startValue.left + (int) ((endValue.left - startValue.left) * fraction),
                startValue.top + (int) ((endValue.top - startValue.top) * fraction),
                startValue.right + (int) ((endValue.right - startValue.right) * fraction),
                startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction));
    }
}
