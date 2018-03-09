package com.example.pluginbluetooth.bluetooth.device;

import org.msgpack.core.MessageTypeCastException;
import org.msgpack.value.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qrom.component.log.QRomLog;

public class CapabilityCenter {

    private static final String TAG = CapabilityCenter.class.getSimpleName();

    /*  Array of NUM_OF_CLOCKFACES length, containing arrays of NUM_OF_HANDS for
        each face where each entry * is the number of steps for a full turn.
        Order: hour hand, minute hand*/
    private List<List<Integer>> mWatchFaces = new ArrayList<List<Integer>>();

    private boolean mVibrator;
    private int mNumOfButtons;
    private boolean mStepCounter;
    private boolean mRemoteDataFix;
    private boolean mVolumeUpDown;
    private boolean mCallRepeatsAlert;
    private boolean mHasMagicKeyOne;

    public CapabilityCenter(final Value value) {

        Map<Integer, Value> map = new HashMap<Integer, Value>();

        try {
            for (Map.Entry<Value, Value> entry :
                    value.asMapValue().entrySet()) {
                map.put(entry.getKey().asIntegerValue().asInt(), entry.getValue());
            }

            if (map.containsKey(0)) {
                mVibrator = map.get(0)
                        .asBooleanValue().getBoolean();
            }

            if (map.containsKey(1)) {
                for (Value faces : map.get(1).asArrayValue().list()) {
                    ArrayList<Integer> handsList = new ArrayList<Integer>();
                    mWatchFaces.add(handsList);
                    for (Value hands : faces.asArrayValue().list()) {
                        handsList.add(hands.asIntegerValue().asInt());
                    }
                }
            }

            if (map.containsKey(2)) {
                mNumOfButtons = map.get(2)
                        .asIntegerValue().asInt();
            }

            if (map.containsKey(3)) {
                mStepCounter = map.get(3)
                        .asBooleanValue().getBoolean();
            }

            if (map.containsKey(4)) {
                mRemoteDataFix = map.get(4)
                        .asBooleanValue().getBoolean();
            }

            if (map.containsKey(5)) {
                mVolumeUpDown = map.get(5)
                        .asBooleanValue().getBoolean();
            }

            if (map.containsKey(6)) {
                mCallRepeatsAlert = map.get(6)
                        .asBooleanValue().getBoolean();
            }

            if (map.containsKey(7)) {
                mHasMagicKeyOne = map.get(7)
                        .asBooleanValue().getBoolean();
            }

        } catch (MessageTypeCastException exception) {
            QRomLog.i(TAG, "Failed to cast data from capabiltyMap");
        }
    }

    public boolean hasVibrator() {
        return mVibrator;
    }

    public List<List<Integer>> getWatchFaces() {
        return mWatchFaces;
    }

    public int getNumOfButtons() {
        return mNumOfButtons;
    }

    public boolean hasStepCounter() {
        return mStepCounter;
    }

    public boolean hasRemoteDataFix() {
        return mRemoteDataFix;
    }

    public boolean hasVolumeUpDown() {
        return mVolumeUpDown;
    }

    public boolean hasCallRepeatsAlert() {
        return mCallRepeatsAlert;
    }

    public boolean hasMagicKeyOne() {
        return mHasMagicKeyOne;
    }
}
