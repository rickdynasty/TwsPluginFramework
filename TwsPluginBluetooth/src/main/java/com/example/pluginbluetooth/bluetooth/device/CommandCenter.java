package com.example.pluginbluetooth.bluetooth.device;

import org.msgpack.value.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qrom.component.log.QRomLog;

public class CommandCenter {

    private static final String TAG = CommandCenter.class.getSimpleName();

    private static final boolean DEBUG = false;

    private final Map<Integer, String> mCommandMap = new HashMap<Integer, String>();
    private final Set<Integer> mKnownMaps = new HashSet<Integer>();

    public CommandCenter() {
        clear();
    }

    public Map<String, Value> translate(final Value value) {
        HashMap<String, Value> map = new HashMap<String, Value>();
        if (DEBUG) QRomLog.i(TAG, "parsed data: " + value.toString());

        for (Map.Entry<Value, Value> entry : value.asMapValue().entrySet()) {
            final int key = entry.getKey().asIntegerValue().asInt();
            map.put(String.valueOf(mCommandMap.get(key)), entry.getValue());
        }
        return map;
    }

    public void parseMap(final String command, final Value pages) {
        if (DEBUG) QRomLog.i(TAG, "parsed data: " + pages.toString());

        for (final Value page : pages.asArrayValue()) {
            for (final Map.Entry<Value, Value> entry : page.asMapValue().entrySet()) {
                final int key = entry.getKey().asIntegerValue().asInt();
                final String value = entry.getValue().asStringValue().asString();
                mCommandMap.put(key, value);
            }
        }

        mKnownMaps.add(getCommandNumber(command));
    }

    public Map<Integer, String> getCommandMap() {
        return mCommandMap;
    }

    public int getCommandNumber(final String command) {
        if (command != null) {
            for (Map.Entry<Integer, String> entry : mCommandMap.entrySet()) {
                if (entry.getValue().equals(command)) {
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

    public boolean hasCommand(final String command) {
        return getCommandNumber(command) != -1;
    }

    public boolean isMapKnown(final String definition) {
        final int commandNumber = getCommandNumber(definition);
        return mKnownMaps.contains(commandNumber);
    }

    public void clear() {
        mCommandMap.clear();
        mKnownMaps.clear();
        mCommandMap.put(0, "map_cmd");
    }
}
