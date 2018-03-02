package com.example.pluginbluetooth.bluetooth.gatt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.content.ContextCompat;

import java.lang.ref.WeakReference;

import qrom.component.log.QRomLog;

public class DeviceScanner {

    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final Handler mHandler = new Handler();
    private final Context mContext;
    private final WeakReference<ScanListener> mListener;
    private boolean mShouldScan = false;
    private boolean mIsScanning = false;

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            QRomLog.i("kaelpu", "ScanResult " + device.getAddress());
            final byte[] data = AdvDataParser.decodeManufacturerData(scanRecord);
            final int deviceType = AdvDataParser.parseDeviceType(data);
            final int itemId = AdvDataParser.parseItemId(data);
            final int valueRssi = rssi;
            final BluetoothDevice valueDevice = device;
            if (deviceType == GattDevice.TYPE_SECONDO || deviceType == GattDevice.TYPE_GARBO) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onScanResultParsed(valueDevice, deviceType, itemId, valueRssi);
                    }
                });
            }
        }
    };

//     适配4.4 蓝牙扫描
//    private final ScanCallback mScanCallback =
//            new ScanCallback() {
//                @Override
//                public void onScanResult(final int callbackType, final ScanResult result) {
//                    // These static methods should be safe to run on any thread
//                    if (result == null || result.getScanRecord() == null) {
//                        return;
//                    }
//                    QRomLog.i("kaelpu","ScanResult "+ result.getDevice().getAddress());
//                    final byte[] data = AdvDataParser.decodeManufacturerData(result.getScanRecord().getBytes());
//                    final int deviceType = AdvDataParser.parseDeviceType(data);
//                    final int itemId = AdvDataParser.parseItemId(data);
//                    // Post the result to the main thread if it's one of the types we want
//                    if (deviceType == GattDevice.TYPE_SECONDO || deviceType == GattDevice.TYPE_GARBO) {
//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                onScanResultParsed(result.getDevice(), deviceType, itemId, result.getRssi());
//                            }
//                        });
//                    }
//
//                }
//            };

    private final BroadcastReceiver mAdapterStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            final int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
            if (state != prevState && state == BluetoothAdapter.STATE_ON) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onBluetoothTurnedOn();
                    }
                });
            }
        }
    };

    public DeviceScanner(final Context context, final ScanListener listener) {
        mContext = context.getApplicationContext();
        mListener = new WeakReference<ScanListener>(listener);
    }

    public void start() {
        mShouldScan = true;
        mContext.registerReceiver(mAdapterStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        maybeStartScan();
    }

    public void stop() {
        mContext.unregisterReceiver(mAdapterStateReceiver);
        stopScan();
        mShouldScan = false;
    }

    private void maybeStartScan() {
        if (!mIsScanning && hasPermissions() && mShouldScan) {
            if (mAdapter != null && mAdapter.isEnabled()) {
                // 适配4.4 蓝牙扫描

//                final BluetoothLeScanner leScanner = mAdapter.getBluetoothLeScanner();
//                if (leScanner != null) {
//                    leScanner.startScan(mScanCallback);
//                    mIsScanning = true;
//                }

                mAdapter.startLeScan(mLeScanCallback);
                mIsScanning = true;
            }
        }
    }

    private void stopScan() {
        if (mIsScanning && mAdapter != null && mAdapter.isEnabled()) {

            // 适配 4.4 蓝牙扫描
//            final BluetoothLeScanner leScanner = mAdapter.getBluetoothLeScanner();
//            if (leScanner != null) {
//                leScanner.stopScan(mScanCallback);
//            }

            mAdapter.stopLeScan(mLeScanCallback);

        }
        mIsScanning = false;
    }

    /**
     * Restart the scan when Bluetooth was turned on (otherwise it won't work)
     */
    private void onBluetoothTurnedOn() {
        stopScan();
        maybeStartScan();
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager
                .PERMISSION_GRANTED;
    }

    public void receivedLocationPermission() {
        maybeStartScan();
    }

    private void onScanResultParsed(final BluetoothDevice device, final int deviceType, final int itemId,
                                    final int rssi) {
        final ScanListener listener = mListener.get();
        if (listener != null && mIsScanning) {
            /*if(BuildConfig.DUBUG_TOOL){
                QRomLog.i("kaelpu",BuildConfig.MAC + "     " + device.getAddress());
                if(!BuildConfig.MAC.isEmpty()){
                    if(BuildConfig.MAC.equals(device.getAddress())){
                        listener.onScanResult(new GattDevice(mContext, device, deviceType, itemId, rssi));
                    }
                }else{
                    listener.onScanResult(new GattDevice(mContext, device, deviceType, itemId, rssi));
                }
            }else{*/
            listener.onScanResult(new GattDevice(mContext, device, deviceType, itemId, rssi));
            //}
        }
    }
}
