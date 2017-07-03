package ml.xuexin.bleconsultant.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import ml.xuexin.bleconsultant.entity.BleDevice;
import ml.xuexin.bleconsultant.tool.BleLog;

/**
 * Created by xuexin on 2017/3/3.
 */

public class ScanUtil implements Resettable {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private ScanCallback scanCallback;
    private HashMap<BluetoothDevice, BleDevice> deviceMap;
    private Vector<BleDevice> deviceVector;

    public ScanUtil(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        deviceMap = new LinkedHashMap<>();
        deviceVector = new Vector<>();
    }

    public void startScan() {
        deviceMap.clear();
        deviceVector.clear();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (leScanCallback == null) {
                leScanCallback = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        onBleScanResult(device, rssi);
                    }
                };
            }
            boolean success = bluetoothAdapter.startLeScan(leScanCallback);
            if (!success) {
                onBleScanFail(-1);
            }
        } else {
            int scanMode;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //keep away from this bug: https://code.google.com/p/android/issues/detail?id=82463
                //Google fix it at Android 6.0
                scanMode = ScanSettings.SCAN_MODE_LOW_POWER;
            } else {
                scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
            }
            if (scanCallback == null) {
                scanCallback = new ScanCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        onBleScanResult(result.getDevice(), result.getRssi());
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        onBleScanFail(errorCode);
                    }
                };
            }
            bluetoothAdapter.getBluetoothLeScanner().
                    startScan(null, new ScanSettings.Builder().setScanMode(scanMode).build(),
                            scanCallback);

        }
    }

    public void stopScan() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (leScanCallback != null)
                bluetoothAdapter.stopLeScan(leScanCallback);
        } else {
            if (scanCallback != null) {
                BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner != null)
                    bluetoothLeScanner.stopScan(scanCallback);
            }
        }
    }

    private void onBleScanResult(BluetoothDevice bluetoothDevice, int rssi) {
        long currentTime = System.currentTimeMillis();
        BleDevice bleDevice = deviceMap.get(bluetoothDevice);
        if (bleDevice == null) {
            bleDevice = new BleDevice(bluetoothDevice, rssi, currentTime);
            deviceMap.put(bluetoothDevice, bleDevice);
            deviceVector.add(bleDevice);
        } else {
            bleDevice.setRssi(rssi);
            bleDevice.setRssiUpdateTime(currentTime);
        }
        BleLog.w(bleDevice.toString());
    }

    private void onBleScanFail(int errorCode) {

    }

    public List<BleDevice> getDevices() {
        return new ArrayList<>(deviceVector);
    }

    @Override
    public void reset() {
        deviceMap.clear();
        deviceVector.clear();
    }
}
