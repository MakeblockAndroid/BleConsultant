package ml.xuexin.bleconsultant;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ml.xuexin.bleconsultant.bluetooth.BleFlowValve;
import ml.xuexin.bleconsultant.bluetooth.Connector;
import ml.xuexin.bleconsultant.bluetooth.ScanUtil;
import ml.xuexin.bleconsultant.entity.BleDevice;
import ml.xuexin.bleconsultant.entity.BleStatus;
import ml.xuexin.bleconsultant.entity.WaitSendData;
import ml.xuexin.bleconsultant.port.CharacteristicNotifyListener;
import ml.xuexin.bleconsultant.port.ConnectCallback;
import ml.xuexin.bleconsultant.port.ConnectionStateListener;
import ml.xuexin.bleconsultant.port.ReadCallback;
import ml.xuexin.bleconsultant.port.RequestRssiCallback;
import ml.xuexin.bleconsultant.port.ScanDevicesHelper;
import ml.xuexin.bleconsultant.tool.BleLog;
import ml.xuexin.bleconsultant.tool.ThreadUtil;

import static android.content.Context.BLUETOOTH_SERVICE;

/**
 * Created by xuexin on 2017/2/28.
 */

public class BleConsultant {
    private static BleConsultant instance;
    private Handler handler;
    private ScanCallBackRunnable scanCallBackRunnable;
    private ScanUtil scanUtil;
    private BluetoothAdapter bluetoothAdapter;
    private Context applicationContext;
    private Connector connector;
    private BleFlowValve bleFlowValve;
    private ConnectionStateListener connectionStateListener;
    public static final int DATA_MAX_LENGTH = 20;
    public static final int TIME_GAP = 10;

    public static BleConsultant getInstance() {
        if (instance == null) {
            synchronized (BleConsultant.class) {
                if (instance == null) {
                    instance = new BleConsultant();
                }
            }
        }
        return instance;
    }

    /**
     * should be called on the main thread
     *
     * @param context
     */
    public void init(Context context) {
        init(context, DATA_MAX_LENGTH, TIME_GAP);
    }

    /**
     * should be called on the main thread
     *
     * @param context
     */
    public void init(Context context, int dataMaxLength, int timeGap) {
        //check
        if (!ThreadUtil.isMainThread()) {
            throw new RuntimeException("Please call init on main thread");
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new RuntimeException("Device don't support BLE");
        }
        handler = new Handler();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            throw new RuntimeException("Init BluetoothAdapter fail");
        }
        scanUtil = new ScanUtil(bluetoothAdapter);
        connector = new Connector(connectionStateListener);
        bleFlowValve = new BleFlowValve(connector, dataMaxLength, timeGap);
        applicationContext = context.getApplicationContext();
    }

    public boolean setScanDevicesHelper(@Nullable ScanDevicesHelper scanDevicesHelper) {
        if (bluetoothAdapter.isEnabled()) {
            try {
                stopScan();
                if (scanDevicesHelper != null) {
                    scanCallBackRunnable = new ScanCallBackRunnable(scanDevicesHelper);
                    handler.postDelayed(scanCallBackRunnable, scanDevicesHelper.getReportPeriod());
                    startScan();
                }
                return true;
            } catch (Exception e) {
                BleLog.e(e.getMessage());
            }
        }
        return false;
    }


    private void startScan() {
        BleLog.w("startScan");
        scanUtil.startScan();
    }

    private void stopScan() {
        BleLog.w("stopScan");
        if (scanCallBackRunnable != null) {
            handler.removeCallbacks(scanCallBackRunnable);
            scanCallBackRunnable = null;
        }
        scanUtil.stopScan();
    }


    public boolean connect(BleDevice bleDevice, ConnectCallback connectCallback) {
        stopScan();
        return connect(bleDevice.getBluetoothDevice(), connectCallback);
    }

    private boolean connect(BluetoothDevice bluetoothDevice, ConnectCallback connectCallback) {
        if (connector.getConnectStatus() == BleStatus.DISCONNECTED) {
            connector.connectDevice(bluetoothDevice, applicationContext, connectCallback);
            return true;
        }
        return false;
    }

    public void disconnect() {
        connector.disconnect();
        reset();
    }

    private void reset() {
        bleFlowValve.reset();
        handler.removeCallbacks(scanCallBackRunnable);
        scanUtil.reset();

    }

    public boolean sendToBle(UUID serviceUuid, UUID characteristicUuid, byte[] data) {
        if (hasConnected()) {
            return bleFlowValve.sendData(new WaitSendData(data, serviceUuid, characteristicUuid));
        }
        return false;
    }

    /**
     * This method should be called after discovering services successfully.
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @param listener
     * @param monopoly           Other listeners will not receive data until removing monopoly listener
     * @return
     */
    public boolean addNotifyListener(UUID serviceUuid,
                                     UUID characteristicUuid,
                                     CharacteristicNotifyListener listener,
                                     boolean monopoly) {
        if (hasConnected()) {
            return connector.addNotifyListener(serviceUuid, characteristicUuid, listener, monopoly);
        }
        return false;
    }

    /**
     * This method should be called after discovering services successfully.
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @param listener
     * @return
     */
    public boolean removeNotifyListener(UUID serviceUuid,
                                        UUID characteristicUuid,
                                        CharacteristicNotifyListener listener) {
        if (hasConnected()) {
            connector.removeNotifyListener(serviceUuid, characteristicUuid, listener);
            return true;
        }
        return false;
    }

    public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
        if (connector != null) {
            connector.setConnectionStateListener(connectionStateListener);
        }
    }


    public boolean hasConnected() {
        if (connector != null) {
            return connector.getConnectStatus() == BleStatus.CONNECTED;
        }
        return false;
    }

    public List<BleDevice> getScanDevices() {
        return scanUtil.getDevices();
    }


    private class ScanCallBackRunnable implements Runnable {

        private final ScanDevicesHelper scanDevicesHelper;

        public ScanCallBackRunnable(ScanDevicesHelper scanDevicesHelper) {
            super();
            this.scanDevicesHelper = scanDevicesHelper;
        }

        @Override
        public void run() {
            List<BleDevice> scanDevices = getScanDevices();
            List<BleDevice> list = new ArrayList<>();
            for (BleDevice bleDevice : scanDevices) {
                if (scanDevicesHelper.deviceFilter(bleDevice)) {
                    list.add(bleDevice);
                }
            }
            handler.postDelayed(this, scanDevicesHelper.getReportPeriod());
            scanDevicesHelper.reportDevices(list);
        }
    }

    public boolean openBluetoothSilently() {
        if (bluetoothAdapter == null) {
            throw new RuntimeException("bluetoothAdapter is null");
        }
        return bluetoothAdapter.enable();
    }


    public void printDebugLog(boolean isPrint) {
        BleLog.DEBUG = isPrint;
    }

    public boolean requestCurrentRssi(RequestRssiCallback requestRssiCallback) {
        if (hasConnected()) {
            return connector.requestRssi(requestRssiCallback);
        }
        return false;
    }

    /**
     * @param serviceUuid
     * @param characteristicUuid
     * @param callback
     * @param cover              will remove (if exist) callback of the characteristic
     * @return true if success
     */
    public boolean readCharacteristic(UUID serviceUuid,
                                      UUID characteristicUuid,
                                      ReadCallback callback,
                                      boolean cover) {
        if (hasConnected()) {
            return connector.readCharacteristic(serviceUuid, characteristicUuid, callback, cover);
        }
        return false;
    }

    public BleStatus getBleStatus() {
        return connector.getConnectStatus();
    }
}
