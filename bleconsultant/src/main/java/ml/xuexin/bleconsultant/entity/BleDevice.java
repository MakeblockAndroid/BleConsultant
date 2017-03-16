package ml.xuexin.bleconsultant.entity;

import android.bluetooth.BluetoothDevice;

/**
 * Created by xuexin on 2017/3/3.
 */

public class BleDevice {
    private final BluetoothDevice bluetoothDevice;
    private int rssi;
    private long rssiUpdateTime;

    public BleDevice(BluetoothDevice bluetoothDevice, int rssi, long rssiUpdateTime) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.rssiUpdateTime = rssiUpdateTime;
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getRssiUpdateTime() {
        return rssiUpdateTime;
    }

    public void setRssiUpdateTime(long rssiUpdateTime) {
        this.rssiUpdateTime = rssiUpdateTime;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("name:" + getName())
                .append(", address:" + getAddress())
                .append(", rssi:" + getRssi())
                .toString();
    }
}
