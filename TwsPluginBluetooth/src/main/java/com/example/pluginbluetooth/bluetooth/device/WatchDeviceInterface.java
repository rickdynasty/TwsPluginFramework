package com.example.pluginbluetooth.bluetooth.device;

import com.example.pluginbluetooth.future.Future;

import org.msgpack.value.Value;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/3/1.
 */

public interface WatchDeviceInterface {

    interface ProgressCallback {
        void onProgress(int partial, int total);
    }

    /* Helpers */
    Future<Void> checkIsDfuReady();

    void setDebugMode(final boolean enable);

    void setUseRefreshServices(final boolean enable);

    boolean getDebugMode();

    boolean isBonded();

    /* Write */
    Future<Void> writeAlarms(final List<DeviceAlarm> alarms);

    Future<Void> writeAlert(int alert);

    Future<Void> writeAlertConfig(int[] alertConfigBitmasks);

    Future<Void> writeBaseConfig(final int timeResolutionMinutes, final int enableStepcounter);

    Future<Void> writeBaseConfig(final int timeResolutionMinutes);

    Future<Void> writeComplicationMode(final int primaryFaceMode);

    Future<Void> writeComplicationMode(final int primaryFaceMode,
                                       final int secondaryFaceMode);

    Future<Void> writeComplicationModes(final int mainMode,
                                        final int alternateMode);

    Future<Void> writeComplicationModes(final int mainMode,
                                        final int alternateMode,
                                        final int otherMode);

    Future<Void> writeComplicationModes(final int mainMode,
                                        final int alternateMode,
                                        final int secondaryFaceMainMode,
                                        final int secondaryFaceAlternateMode);

    Future<Void> writeComplicationModes(final int mainMode,
                                        final int alternateMode,
                                        final int otherMode,
                                        final int primaryFaceMainMode,
                                        final int primaryFaceAlternateMode,
                                        final int primaryFaceOtherMode);

    Future<Void> writeConfigSettings(final Map<String, Integer> settings);

    Future<Void> writeConfigVibrator(int[]... patterns);

    Future<Void> writeCrash();

    Future<Void> writeDateTime(final int year, final int month, final int day, final int hour,
                               final int min, final int sec, final int weekday);

    Future<Void> writeDebug(final Value command);

    Future<Void> writeDebug(final Value command, final Value data);

    Future<Void> writeDebugAppError(final int errorCode);

    Future<Void> writeDebugConfig(final List<Integer> config);

    Future<Void> writeDebugConfig(final boolean timeCompress,
                                  final boolean enableUart,
                                  final boolean enableTemperature,
                                  final boolean enableLedAndVibrationOnDisconnect,
                                  final boolean deprecate,
                                  final int onErrorRebootTimeout,
                                  final int millisPerMinuteTick,
                                  final boolean rssiNotification);

    Future<Void> writeDebugConfig(final boolean demoMode,
                                  final boolean enableUart,
                                  final boolean enableTemperature,
                                  final boolean enableLedOnDisconnect,
                                  final boolean enableMinuteTick,
                                  final int onErrorRebootTimeout,
                                  final boolean rssiNotification);

    Future<Void> writeDebugHardFault();

    Future<Void> writeDebugReset(final int resetType);

    Future<Void> writeEinkImg(final byte[] data);

    Future<Void> writeEinkImgCmd(final int cmd);

    Future<Void> writeForgetDevice();

    Future<Void> writeIncomingCall(final int number, final boolean isRinging,
                                   final Integer alert);

    Future<Void> writeOnboardingDone(final boolean finished);

    Future<Void> writeMotor(final int motor, final int value);

    Future<Void> writeMotorDelay(final int value);

    Future<Void> writePostMortem();

    Future<Void> writeRecalibrate(final boolean enable);

    Future<Void> writeRecalibrateMove(final int motor, final int steps);

    Future<Void> writeStartVibrator();

    Future<Void> writeStartVibratorWithPattern(final int[] pattern);

    Future<Void> writeStepperExecPredef(final int handNo1, final int handNo2, final int patternIndex2,
                                        final int patternIndex3);

    Future<Void> writeSteps(final int total, final int[] weekdays);

    Future<Void> writeStepsDay(final int steps, final int dayOfMonth);

    Future<Void> writeStepsTarget(final int stepsTarget);

    Future<Void> writeStillness(final int timeout, final int window, final int start, final int end);

    Future<Void> writeStopVibrator();

    Future<Void> writeTest(final int testCase, final int val);

    Future<Void> writeTimeZone(final String timeZoneId);

    Future<Void> writeTriggers(final int upperTrigger, final int lowerTrigger);

    Future<Void> writeVbat();

    Future<Void> writeVbatSim(final int mv);

    Future<Void> writeWatchTime();

    /* Read */
    Future<Map<String, String>> readBuildInfo();

    Future<Map<String, String>> readBuildInfoBl();

    Future<List<Integer>> readCoil();

    Future<Integer> readCurrentConnInt();

    Future<Boolean> readDateTime();

    Future<List<Integer>> readDebugDisconnect();

    Future<Map<String, String>> readDeviceInformation();

    Future<String> readDeviceItemNumber();

    Future<Map<String, Value>> readDiagnostics();

    Future<List<Integer>> readFcte();

    String readFirmwareVersion();

    Future<Boolean> readOnboardingDone();

    Future<Value> readPostMortem();

    Future<byte[]> readPostMortemData();

    Future<Value> readUartDump(final ProgressCallback progress);

    Future<byte[]> readUartDumpData(final ProgressCallback progress);

    Future<Integer> readRssi();

    Future<String> readSerialNumber();

    Future<List<Integer>> readSteps();

    Future<List<Integer>> readStepsDay(final int day);

    Future<Integer> readStepsTarget();

    Future<List<Integer>> readStillness();

    Future<Integer> readVbat();
}
