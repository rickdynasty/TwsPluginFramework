package com.example.pluginbluetooth.app.animation;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class WatchHandsAnimation {

    private View mWatchHandMinutesView;
    private View mWatchHandHoursView;
    private WatchHandModel mWatchHandModel;
    private ObjectAnimator mWatchHandMinutesAnimator;
    private ObjectAnimator mWatchHandHoursAnimator;

    private static final float MIN_ANIMATION_TIME_MS = 700;
    private static final float ANIMATION_TIME_PER_FULL_LAP_MS = 2000;
    private static final float ANIMATION_TIME_PER_DEGREE_MS = ANIMATION_TIME_PER_FULL_LAP_MS / 360f;

    public WatchHandsAnimation(View watchHandMinutesView, View watchHandHoursView, WatchHandModel watchHandModel) {
        mWatchHandMinutesView = watchHandMinutesView;
        mWatchHandHoursView = watchHandHoursView;
        mWatchHandModel = watchHandModel;
    }

    public void setWatchHandModel(WatchHandModel watchHandModel) {
        mWatchHandModel = watchHandModel;
    }

    public void update(boolean animate) {
        cancelOngoingAnimations();
        if (animate) {
            startAnimations();
        } else {
            mWatchHandMinutesView.setRotation(mWatchHandModel.getMinutesInDegrees());
            mWatchHandHoursView.setRotation(mWatchHandModel.getHoursInDegrees());
        }
    }

    private void startAnimations() {
        cancelOngoingAnimations();

        float currentRotation = mWatchHandMinutesView.getRotation();

        if (currentRotation > 360) {
            currentRotation -= 360;
        }
        float targetRotation = mWatchHandModel.getMinutesInDegrees();

        if (targetRotation < (currentRotation - 0.0001f)) {
            targetRotation += 360;
        }

        float duration = Math.max(MIN_ANIMATION_TIME_MS, Math.abs(currentRotation - targetRotation) *
                ANIMATION_TIME_PER_DEGREE_MS);
        mWatchHandMinutesAnimator = ObjectAnimator.ofFloat(mWatchHandMinutesView,
                "rotation", currentRotation, targetRotation);
        mWatchHandMinutesAnimator.setDuration((long) duration);
        mWatchHandMinutesAnimator.setInterpolator(new DecelerateInterpolator());
        mWatchHandMinutesAnimator.start();

        currentRotation = mWatchHandHoursView.getRotation();
        if (currentRotation > 360) {
            currentRotation -= 360;
        }

        targetRotation = mWatchHandModel.getHoursInDegrees();
        if (targetRotation < (currentRotation - 0.0001f)) {
            targetRotation += 360;
        }

        duration = Math.max(MIN_ANIMATION_TIME_MS, Math.abs(currentRotation - targetRotation) *
                ANIMATION_TIME_PER_DEGREE_MS);
        mWatchHandHoursAnimator = ObjectAnimator.ofFloat(mWatchHandHoursView,
                "rotation", currentRotation, targetRotation);
        mWatchHandHoursAnimator.setDuration((long) duration);
        mWatchHandHoursAnimator.setInterpolator(new DecelerateInterpolator());
        mWatchHandHoursAnimator.start();
    }

    private void cancelOngoingAnimations() {
        if (mWatchHandMinutesAnimator != null && mWatchHandMinutesAnimator.isRunning()) {
            mWatchHandMinutesAnimator.cancel();
        }
        if (mWatchHandHoursAnimator != null && mWatchHandHoursAnimator.isRunning()) {
            mWatchHandHoursAnimator.cancel();
        }
    }

    public interface WatchHandModel {

        float getMinutesInDegrees();

        float getHoursInDegrees();
    }
}
