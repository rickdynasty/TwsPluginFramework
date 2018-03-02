package com.example.pluginbluetooth.watch;

import com.example.pluginbluetooth.behaviour.Behaviour;

import java.util.Map;

public class BaseWatchProviderListener implements WatchProvider.WatchProviderListener {

    @Override
    public void onAlarmEvent(final int alarmState) {

    }

    @Override
    public void onButtonClicked(final int slot, final String behaviour, final int action) {

    }

    @Override
    public void onConnectionChanged(final boolean isConnected) {

    }

    @Override
    public void onDeviceDebugDisconnect(final Map<String, String> params) {

    }

    @Override
    public void onStepsNow(final int stepsToday, final int dayOfMonth) {

    }

    @Override
    public void onDaily() {

    }

    @Override
    public void onStillnessEvent(final int stillnessEvent) {

    }

    @Override
    public void onRssiEvent(int onRssiEvent) {

    }

    @Override
    public void onDiagEvent(Map<String, String> diagEvent) {

    }

    @Override
    public void onWroteDeviceSettings() {

    }

    @Override
    public void onOnboardingStarted() {
    }

    @Override
    public void onOnboardingFinished(final boolean isFinished) {

    }

    @Override
    public void onTriggerSet(final int slot, final Behaviour behaviour) {
    }

    @Override
    public void onConnIntChange(final int currentConnInt, final int slaveLatency, final int timeout) {
    }

    @Override
    public void onCalibrationTimeout() {
    }
}
