package ml.xuexin.bleconsultant.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.UUID;

import ml.xuexin.bleconsultant.BleConsultant;
import ml.xuexin.bleconsultant.entity.BleStatus;
import ml.xuexin.bleconsultant.port.ConnectCallback;
import ml.xuexin.bleconsultant.port.ConnectionStateListener;
import ml.xuexin.bleconsultant.port.ReadCallback;
import ml.xuexin.bleconsultant.port.RequestRssiCallback;
import ml.xuexin.bleconsultant.tool.BleLog;

/**
 * Created by xuexin on 2017/3/8.
 */

public class Connector implements Resettable {
    private static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt bluetoothGatt;
    private CharacteristicMap characteristicMap = new CharacteristicMap();
    private ReadCallbackMap readCallbackMap = new ReadCallbackMap();
    private ConnectorHandler handler;
    private ConnectCallback connectCallback;
    private BleStatus connectStatus = BleStatus.DISCONNECTED;
    private ConnectionStateListener connectionStateListener;
    private RequestRssiCallback requestRssiCallback;
    private ReliableBluetoothGattCallback bluetoothGattCallback;

    public Connector(ConnectionStateListener connectionStateListener) {
        this.handler = new ConnectorHandler();
        this.connectionStateListener = connectionStateListener;
    }

    public void connectClient(BluetoothDevice bluetoothDevice, Context context,
                              ConnectCallback callback) {
        connectCallback = callback;
        handler.sendEmptyMessageDelayed(handler.CONNECT_OVERTIME_MESSAGE,
                connectCallback.getOvertimeTime());
        bluetoothGattCallback = new ReliableBluetoothGattCallback();
        bluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
        connectStatus = BleStatus.CONNECTING;
    }

    public void disconnect() {
        if (bluetoothGatt != null)
            bluetoothGatt.disconnect();
        reset();
    }

    private BluetoothGattCharacteristic getCharacteristic(String serviceUuid, String characteristicUuid) {
        return characteristicMap.get(serviceUuid, characteristicUuid);
    }

    public boolean writeToBle(String serviceUuid, String characteristicUuid, byte[] data) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic =
                getCharacteristic(serviceUuid, characteristicUuid);
        try {
            if (bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.setValue(data);
                return bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            }
        } catch (Exception e) {

        }
        return false;
    }


    public boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w("gatt or characteristic equal null");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            BleLog.e("Check characteristic property: false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d("setCharacteristicNotification----" + enable + "----success： " + success
                + '\n' + "characteristic.getUuid() :  " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR);
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    public boolean registerNotify(String serviceUuid,
                                  String characteristicUuid) {

        BluetoothGattCharacteristic bluetoothGattCharacteristic =
                getCharacteristic(serviceUuid, characteristicUuid);
        return setCharacteristicNotification(bluetoothGatt, bluetoothGattCharacteristic, true);
    }

    public boolean unregisterNotify(String serviceUuid,
                                    String characteristicUuid) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic =
                getCharacteristic(serviceUuid, characteristicUuid);
        return setCharacteristicNotification(bluetoothGatt, bluetoothGattCharacteristic, false);
    }

    private void onConnectCallback(int state) {
        if (connectionStateListener != null) {
            connectionStateListener.onStateChange(state);
        }
        if (connectCallback != null) {
            connectCallback.onStateChange(state);
        }
        switch (state) {
            case ConnectCallback.STATE_CONNECT_OVERTIME:
            case ConnectCallback.STATE_SERVICES_DISCOVERED_FAIL:
                if (bluetoothGatt != null) {
                    bluetoothGatt.disconnect();
                }
            case ConnectCallback.STATE_DISCONNECTED:
                reset();
            case ConnectCallback.STATE_SERVICES_DISCOVERED:
                handler.removeMessages(handler.CONNECT_OVERTIME_MESSAGE);
                connectCallback = null;
                break;
        }
    }

    public BleStatus getConnectStatus() {
        return connectStatus;
    }

    public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }

    @Override
    public void reset() {
        connectStatus = BleStatus.DISCONNECTED;
        connectCallback = null;
        characteristicMap.clear();
        readCallbackMap.clear();
        handler.removeMessages(handler.CONNECT_OVERTIME_MESSAGE);
        handler.removeMessages(handler.REQUEST_RSSI_OVERTIME);
    }

    public boolean requestRssi(RequestRssiCallback requestRssiCallback) {
        if (hasConnected()) {
            this.requestRssiCallback = requestRssiCallback;
            handler.sendEmptyMessageDelayed(handler.REQUEST_RSSI_OVERTIME,
                    requestRssiCallback.getOvertimeTime());
            return bluetoothGatt.readRemoteRssi();
        }
        return false;
    }

    public boolean readCharacteristic(String serviceUuid,
                                      String characteristicUuid,
                                      ReadCallback callback,
                                      boolean cover) {
        if (cover && readCallbackMap.get(serviceUuid, characteristicUuid) != null) {
            return false;
        }
        BluetoothGattCharacteristic characteristic =
                characteristicMap.get(serviceUuid, characteristicUuid);
        readCallbackMap.put(serviceUuid, characteristicUuid, callback);
        Message message = new Message();
        message.what = handler.READ_MESSAGE_OVERTIME;
        message.getData().putString(handler.SERVICE_UUID_KEY, serviceUuid);
        message.getData().putString(handler.CHARACTERISTIC_UUID_KEY, characteristicUuid);
        handler.sendMessageDelayed(message, callback.getOvertimeTime());
        return bluetoothGatt.readCharacteristic(characteristic);
    }

    //Sometimes system will not unregister callback, check whether is latest one
    private class ReliableBluetoothGattCallback extends BluetoothGattCallback {
        public ReliableBluetoothGattCallback() {
            super();
        }

        private boolean isLatestGattCallback() {
//            BleLog.e((bluetoothGattCallback == this) + "");
            return bluetoothGattCallback == this;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            int callbackState = ConnectCallback.STATE_UNINIT;
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    gatt.discoverServices();
                    callbackState = ConnectCallback.STATE_CONNECTED;
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    callbackState = ConnectCallback.STATE_CONNECTING;
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    callbackState = ConnectCallback.STATE_DISCONNECTED;
                    gatt.close();
                    if (bluetoothGatt == gatt) {
                        bluetoothGatt = null;
                    }
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    callbackState = ConnectCallback.STATE_DISCONNECTING;
                    break;
            }
            if (!isLatestGattCallback()) {
                return;
            }
            Message message = new Message();
            message.what = handler.CONNECT_MESSAGE;
            message.arg1 = callbackState;
            handler.sendMessage(message);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (!isLatestGattCallback()) {
                return;
            }
            int callbackState;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleLog.w("onServicesDiscovered");
                connectStatus = BleStatus.CONNECTED;
                characteristicMap.setCharacteristics(gatt.getServices());
                callbackState = ConnectCallback.STATE_SERVICES_DISCOVERED;
            } else {
                callbackState = ConnectCallback.STATE_SERVICES_DISCOVERED_FAIL;
            }
            Message message = new Message();
            message.what = handler.CONNECT_MESSAGE;
            message.arg1 = callbackState;
            handler.sendMessage(message);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (!isLatestGattCallback()) {
                return;
            }
            Message message = new Message();
            message.what = handler.READ_MESSAGE;
            message.obj = characteristic;
            message.getData().putByteArray(handler.RECEIVE_DATA_KEY, characteristic.getValue());
            message.arg1 = status;
            handler.sendMessage(message);
            BleLog.i("onCharacteristicRead");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (!isLatestGattCallback()) {
                return;
            }
            BleLog.i("onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (!isLatestGattCallback()) {
                return;
            }
            byte[] data = characteristic.getValue();
            Message message = new Message();
            message.getData().putByteArray(handler.RECEIVE_DATA_KEY, data);
            message.what = handler.RECEIVE_MESSAGE;
            String serviceUUID = characteristic.getService().getUuid().toString();
            String characteristicUUID = characteristic.getUuid().toString();
            message.getData().putString(handler.SERVICE_UUID_KEY, serviceUUID);
            message.getData().putString(handler.CHARACTERISTIC_UUID_KEY, characteristicUUID);
            handler.sendMessage(message);
            BleLog.d("onCharacteristicChanged, data:" + BleLog.parseByte(data) + ", serviceUUID:" + serviceUUID + ", characteristicUUID:" + characteristicUUID);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (!isLatestGattCallback()) {
                return;
            }
            Message message = new Message();
            message.what = handler.REQUEST_RSSI_MESSAGE;
            message.arg1 = rssi;
            message.arg2 = status;
            handler.sendMessage(message);
        }
    }

    private void dispatchNotifyData(String serviceUUID, String characteristicUUID, byte[] data) {
        BleConsultant.getInstance().onReceiveData(serviceUUID, characteristicUUID, data);
    }

    private boolean hasConnected() {
        return connectStatus == BleStatus.CONNECTED && bluetoothGatt != null;
    }


    private class ConnectorHandler extends Handler {
        public static final int RECEIVE_MESSAGE = 0;
        public static final String RECEIVE_DATA_KEY = "RECEIVE_DATA_KEY";
        public static final String SERVICE_UUID_KEY = "SERVICE_UUID_KEY";
        public static final String CHARACTERISTIC_UUID_KEY = "CHARACTERISTIC_UUID_KEY";

        public static final int CONNECT_MESSAGE = 1;
        public static final int CONNECT_OVERTIME_MESSAGE = 2;

        public static final int REQUEST_RSSI_MESSAGE = 3;
        public static final int REQUEST_RSSI_OVERTIME = 4;

        public static final int READ_MESSAGE = 5;
        public static final int READ_MESSAGE_OVERTIME = 6;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] data = msg.getData().getByteArray(RECEIVE_DATA_KEY);
            String serviceUUID = msg.getData().getString(SERVICE_UUID_KEY);
            String characteristicUUID = msg.getData().getString(CHARACTERISTIC_UUID_KEY);
            switch (msg.what) {
                case RECEIVE_MESSAGE:
                    if (data == null || serviceUUID == null || characteristicUUID == null)
                        break;
                    dispatchNotifyData(serviceUUID, characteristicUUID, data);
                    break;
                case CONNECT_MESSAGE:
                    onConnectCallback(msg.arg1);
                    break;
                case CONNECT_OVERTIME_MESSAGE:
                    onConnectCallback(ConnectCallback.STATE_CONNECT_OVERTIME);
                    break;
                case REQUEST_RSSI_MESSAGE:
                    if (requestRssiCallback != null) {
                        removeMessages(REQUEST_RSSI_OVERTIME);
                        requestRssiCallback.onReadRemoteRssi(msg.arg1, msg.arg2);
                    }
                    break;
                case REQUEST_RSSI_OVERTIME:
                    if (requestRssiCallback != null) {
                        requestRssiCallback.onOvertime();
                        requestRssiCallback = null;
                    }
                    break;
                case READ_MESSAGE: {
                    if (data == null || serviceUUID == null || characteristicUUID == null)
                        break;
                    ReadCallback callback = readCallbackMap.get(serviceUUID, characteristicUUID);
                    if (callback != null) {
                        callback.onCharacteristicRead(msg.arg1, data);
                    }
                    handler.removeMessages(READ_MESSAGE_OVERTIME);
                    readCallbackMap.remove(serviceUUID, characteristicUUID);
                }
                break;
                case READ_MESSAGE_OVERTIME:
                    ReadCallback callback = readCallbackMap.get(serviceUUID, characteristicUUID);
                    if (callback != null) {
                        callback.onOvertime();
                    }
                    readCallbackMap.remove(serviceUUID, characteristicUUID);
                    break;
            }
        }
    }
}
