package com.example.pluginbluetooth.bluetooth.device.readwrite;

import com.example.pluginbluetooth.BuildConfig;
import com.example.pluginbluetooth.bluetooth.device.CommandCenter;
import com.example.pluginbluetooth.bluetooth.device.UUIDStorage;
import com.example.pluginbluetooth.bluetooth.gatt.GattDevice;
import com.example.pluginbluetooth.future.Future;
import com.example.pluginbluetooth.future.Promise;
import com.example.pluginbluetooth.utils.Callback;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import qrom.component.log.QRomLog;

public class DeviceWriter {

    protected static final String TAG = DeviceWriter.class.getSimpleName();

    private boolean mIsDebugEnabled = BuildConfig.DEBUG;

    private final GattDevice mDevice;
    private final CommandCenter mCmdCenter;

    public DeviceWriter(final GattDevice device, final CommandCenter commandCenter) {
        mDevice = device;
        mCmdCenter = commandCenter;
    }

    public Command createCommand(final String name, final Value value,
                                 final Promise<Void> promise) throws IOException {
        final int nbr = mCmdCenter.getCommandNumber(name);
        final Value encodedValue = encodeWriteCommand(nbr, value);

        return createCommand(name, nbr, encodedValue, promise);
    }

    public Command createCommand(final String name, final List<Value> values,
                                 final Promise<Void> promise) throws IOException {
        final int nbr = mCmdCenter.getCommandNumber(name);
        final Value encodedValue = encodeWriteCommand(nbr, values);

        return createCommand(name, nbr, encodedValue, promise);
    }

    public Future<Void> write(final Command cmd) {
        return write(cmd, true);
    }

    public void setDebugMode(final boolean enable) {
        mIsDebugEnabled = enable;
    }

    private Future<Void> write(final Command cmd, final boolean useCommandCache) {
        QRomLog.d(TAG, "Initiate write - " + cmd.toString());

        final Promise<Void> promise = new Promise<Void>();

        try {
            if (mDevice == null) {
                throw new IOException("No device connected.");
            }

            if (!mDevice.hasGattService(UUIDStorage.ANIMA_SERVICE)) {
                throw new IOException("Anima service not found.");
            }

            final String name = cmd.getName();
            final byte[] data = cmd.getData();

            mDevice.write(UUIDStorage.ANIMA_SERVICE,
                    UUIDStorage.ANIMA_CHAR,
                    data,
                    new Callback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                            if (mIsDebugEnabled) {
                                QRomLog.d(TAG, "Wrote name: " + name + ", data: " + data);
                            }

                            promise.resolve(result);
                        }

                        @Override
                        public void onError(final Throwable error) {
                            promise.reject(error);
                        }
                    });
        } catch (IOException e) {
            if (mIsDebugEnabled) {
                QRomLog.d(TAG, e.getMessage());
            }

            promise.reject(e);
        }

        return promise.getFuture();
    }

    private Value encodeWriteCommand(final int commandNumber,
                                     final Value data) throws IOException {
        /* Empty command - no data */
        if (data == null) {
            return ValueFactory.newInteger(commandNumber);
        }

        final ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();

        map.put(ValueFactory.newInteger(commandNumber), data);

        return map.build();
    }

    private Value encodeWriteCommand(final int commandNumber, final List<Value> values) {
        final List<Value> paramsList = new ArrayList<Value>();

        paramsList.add(ValueFactory.newInteger(commandNumber));
        paramsList.addAll(values);

        return ValueFactory.newArray(paramsList);
    }

    private byte[] encodeMsgPackValue(final Value value) throws IOException {
        final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        packer.packValue(value);

        return packer.toByteArray();
    }

    private Command createCommand(final String name, final int nbr, final Value value,
                                  final Promise<Void> promise) throws IOException {
        if (mIsDebugEnabled) {
            QRomLog.d(TAG, "Create command name: " + name + ", value: " + value);
        }

        if (nbr < 0) {
            throw new IOException("No such command found: " + name);
        }

        if (value == null) {
            throw new IOException("Couldn't encode value: " + name);
        }

        final byte[] data = encodeMsgPackValue(value);

        return new Command(name, data, promise);
    }
}