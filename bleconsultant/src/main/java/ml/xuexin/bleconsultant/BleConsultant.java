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

import ml.xuexin.bleconsultant.bluetooth.BleFlowValve;
import ml.xuexin.bleconsultant.bluetooth.Connector;
import ml.xuexin.bleconsultant.bluetooth.ScanUtil;
import ml.xuexin.bleconsultant.entity.BleClient;
import ml.xuexin.bleconsultant.entity.BleStatus;
import ml.xuexin.bleconsultant.entity.WaitSendData;
import ml.xuexin.bleconsultant.exception.BleNotSupportException;
import ml.xuexin.bleconsultant.exception.InitBluetoothException;
import ml.xuexin.bleconsultant.exception.ThreadException;
import ml.xuexin.bleconsultant.port.CharacteristicNotifyListener;
import ml.xuexin.bleconsultant.port.ConnectCallback;
import ml.xuexin.bleconsultant.port.ConnectionStateListener;
import ml.xuexin.bleconsultant.port.ReadCallback;
import ml.xuexin.bleconsultant.port.RequestRssiCallback;
import ml.xuexin.bleconsultant.port.ScanClientsHelper;
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
    private CharacteristicNotifyListener characteristicNotifyListener;

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
            throw new ThreadException();
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new BleNotSupportException();
        }
        handler = new Handler();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (!isSupportBle()) {
            throw new InitBluetoothException();
        }
        scanUtil = new ScanUtil(bluetoothAdapter);
        connector = new Connector(connectionStateListener);
        bleFlowValve = new BleFlowValve(connector, dataMaxLength, timeGap);
        applicationContext = context.getApplicationContext();
    }

    /**
     * This method will drop the buffer
     * @param interval
     */
    public void resetBleProperty(int dataMaxLength, int interval) {
        bleFlowValve.destory();
        bleFlowValve = new BleFlowValve(connector, dataMaxLength, interval);
    }

    public boolean setScanClientsHelper(@Nullable ScanClientsHelper scanClientsHelper) {
        if (bluetoothAdapter.isEnabled()) {
            try {
                stopScan();
                if (scanClientsHelper != null) {
                    scanCallBackRunnable = new ScanCallBackRunnable(scanClientsHelper);
                    handler.postDelayed(scanCallBackRunnable, scanClientsHelper.getReportPeriod());
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


    public boolean connect(BleClient bleClient, ConnectCallback connectCallback) {
        stopScan();
        return connect(bleClient.getBluetoothDevice(), connectCallback);
    }

    private boolean connect(BluetoothDevice bluetoothDevice, ConnectCallback connectCallback) {
        if (connector.getConnectStatus() == BleStatus.DISCONNECTED) {
            connector.connectClient(bluetoothDevice, applicationContext, connectCallback);
            return true;
        }
        return false;
    }

    public void disconnect() {
        bleFlowValve.reset();
        connector.disconnect();
        reset();
    }

    private void reset() {
        bleFlowValve.reset();
        handler.removeCallbacks(scanCallBackRunnable);
        scanUtil.reset();

    }

    public boolean sendToBle(String serviceUuid, String characteristicUuid, byte[] data) {
        if (hasConnected()) {
            return bleFlowValve.sendData(new WaitSendData(data, serviceUuid, characteristicUuid));
        }
        return false;
    }

    public void setNotifyListener(CharacteristicNotifyListener listener) {
        characteristicNotifyListener = listener;

    }

    public boolean registerNotify(String serviceUuid, String characteristicUuid) {
        if (hasConnected()) {
            return connector.registerNotify(serviceUuid, characteristicUuid);
        }
        return false;
    }

    public boolean unregisterNotify(String serviceUuid, String characteristicUuid) {
        if (hasConnected()) {
            return connector.unregisterNotify(serviceUuid, characteristicUuid);
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

    public List<BleClient> getScanClients() {
        return scanUtil.getClients();
    }

    public void onReceiveData(String serviceUUID, String characteristicUUID, byte[] data) {
        if (characteristicNotifyListener != null) {
            characteristicNotifyListener.onReceive(serviceUUID, characteristicUUID, data);
        }
    }


    private class ScanCallBackRunnable implements Runnable {

        private final ScanClientsHelper scanClientsHelper;

        public ScanCallBackRunnable(ScanClientsHelper scanClientsHelper) {
            super();
            this.scanClientsHelper = scanClientsHelper;
        }

        @Override
        public void run() {
            List<BleClient> scanClients = getScanClients();
            List<BleClient> list = new ArrayList<>();
            for (BleClient bleClient : scanClients) {
                if (scanClientsHelper.clientFilter(bleClient)) {
                    list.add(bleClient);
                }
            }
            handler.postDelayed(this, scanClientsHelper.getReportPeriod());
            scanClientsHelper.reportClients(list);
        }
    }

    public boolean openBluetoothSilently() {
        if (!isSupportBle()) {
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
    public boolean readCharacteristic(String serviceUuid,
                                      String characteristicUuid,
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

    public boolean isEnabled() {
        if (isSupportBle())
            return bluetoothAdapter.isEnabled();
        return false;
    }

    public boolean isDiscovering() {
        if (isSupportBle())
            return bluetoothAdapter.isDiscovering();
        return false;
    }

    public boolean isSupportBle() {
        return bluetoothAdapter != null;
    }

    public boolean writeDataForTest(String serviceUuid, String characteristicUuid, byte[] data) {
        return connector.writeToBle(serviceUuid, characteristicUuid, data);
    }

}
