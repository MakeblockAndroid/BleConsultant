package ml.xuexin.bleconsultantsample;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import java.util.List;

import ml.xuexin.bleconsultant.BleConsultant;
import ml.xuexin.bleconsultant.entity.BleClient;
import ml.xuexin.bleconsultant.port.CharacteristicNotifyListener;
import ml.xuexin.bleconsultant.port.ConnectCallback;
import ml.xuexin.bleconsultant.port.ConnectionStateListener;
import ml.xuexin.bleconsultant.port.ReadCallback;
import ml.xuexin.bleconsultant.port.RequestRssiCallback;
import ml.xuexin.bleconsultant.port.ScanClientsHelper;
import ml.xuexin.bleconsultant.tool.BleLog;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SERVICE_UUID1 = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String READ_UUID = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private static final String WRITE_UUID = "0000ffe3-0000-1000-8000-00805f9b34fb";

    protected static final String SERVICE_UUID2 = "0000ffe4-0000-1000-8000-00805f9b34fb";
    protected static final String PIO4_UUID = "0000ffe5-0000-1000-8000-00805f9b34fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BleConsultant.getInstance().init(getApplication());
        findViewById(R.id.search_button).setOnClickListener(this);
        findViewById(R.id.send_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        BleConsultant.getInstance().printDebugLog(true);
        BleConsultant.getInstance().openBluetoothSilently();
        BleConsultant.getInstance().setConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void onStateChange(int state) {
                BleLog.w("State:" + state);
            }
        });
    }

    private byte[] getCmd() {
        int index = 130;
        int length = 3;
        int action = 1;
        int device = 0;
        byte[] cmd = new byte[6];
        cmd[0] = (byte) 0xff;
        cmd[1] = (byte) 0x55;
        cmd[2] = (byte) length;
        cmd[3] = (byte) index;
        cmd[4] = (byte) action;
        cmd[5] = (byte) device;
        return cmd;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.disconnect_button:
                disconnect();
                break;
            case R.id.send_button:
                sendData1();
//                readCharacteristic();
                break;
            case R.id.search_button:
                searchAndConnect();
                break;
        }
    }

    private void sendData2() {
        BleConsultant.getInstance().sendToBle(SERVICE_UUID2, PIO4_UUID, new byte[]{1});
    }

    private void sendData1() {
        BleConsultant.getInstance().sendToBle(SERVICE_UUID1, WRITE_UUID, getCmd());
    }

    private void requestRssi() {
        boolean success = BleConsultant.getInstance().requestCurrentRssi(new RequestRssiCallback() {
            @Override
            public void onReadRemoteRssi(int rssi, int status) {
                Log.e("xuexin", "rssi:" + rssi + ", status:" + status);

            }

            @Override
            public void onOvertime() {
                Log.e("xuexin", "onOvertime");
            }

            @Override
            public long getOvertimeTime() {
                return 500;
            }
        });
    }

    private static final int LOCATION = 1;

    private void methodRequiresTwoPermission() {

    }

    @AfterPermissionGranted(LOCATION)
    private void searchAndConnect() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            BleConsultant.getInstance().setScanClientsHelper(new ScanClientsHelper() {
                @Override
                public void reportClients(List<BleClient> bleClientList) {
                    for (final BleClient bleClient : bleClientList) {
                        connectClient(bleClient);
                        BleLog.w("address:" + bleClient.getAddress() + ", rssi:" + bleClient.getRssi());
                    }
                }

                @Override
                public boolean clientFilter(BleClient bleClient) {
                    if (bleClient.getName() != null && bleClient.getName().contains("Makeblock")
                            && bleClient.getRssi() > -40)
                        return true;
                    return false;
                }

                @Override
                public long getReportPeriod() {
                    return 1000;
                }
            });
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "需要位置权限,否则无法搜索",
                    LOCATION, perms);
        }

    }

    private void connectClient(BleClient bleClient) {
        BleConsultant.getInstance().connect(bleClient, new ConnectCallback() {
            @Override
            public void onStateChange(int state) {
                switch (state) {
                    case ConnectCallback.STATE_DISCONNECTED:
                        // TODO: 2017/3/10 fail
                        break;
                    case ConnectCallback.STATE_CONNECT_OVERTIME:
                        // TODO: 2017/3/10 overtime
                        break;
                    case ConnectCallback.STATE_SERVICES_DISCOVERED:
                        // TODO: 2017/3/13 success, add listener and send data?
                        registerListener();
                        break;
                    case ConnectCallback.STATE_CONNECTED:
                        // TODO: 2017/3/10 connected, wait discover services
                        break;
                    case ConnectCallback.STATE_CONNECTING:
                    case ConnectCallback.STATE_DISCONNECTING:
                    default:
                        break;

                }
            }

            @Override
            public long getOvertimeTime() {
                return 10000;
            }

            @Override
            public void onOvertime() {

            }
        });
    }

    private void registerListener() {
        BleConsultant.getInstance().registerNotify(SERVICE_UUID1, READ_UUID);
        BleConsultant.getInstance().setNotifyListener(new CharacteristicNotifyListener() {
            @Override
            public void onReceive(String serviceUUID, String characteristicUUID, byte[] value) {
                Log.e("BleConsultant", "receive:" + BleLog.parseByte(value));
            }

        });
    }

    private void readCharacteristic() {
        boolean success = BleConsultant.getInstance().readCharacteristic(SERVICE_UUID2, PIO4_UUID, new ReadCallback() {
            @Override
            public void onCharacteristicRead(int status, byte[] data) {
                Log.e("xuexin", "status:" + status + ", data:" + BleLog.parseByte(data));
            }

            @Override
            public void onOvertime() {
                Log.e("xuexin", "overtime");
            }

            @Override
            public long getOvertimeTime() {
                return 1000;
            }
        }, false);
    }

    private void disconnect() {
        BleConsultant.getInstance().disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
