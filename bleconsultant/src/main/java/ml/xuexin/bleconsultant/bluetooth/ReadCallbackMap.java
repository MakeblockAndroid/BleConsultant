package ml.xuexin.bleconsultant.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashMap;
import java.util.Map;

import ml.xuexin.bleconsultant.port.ReadCallback;
import ml.xuexin.bleconsultant.tool.BleLog;

/**
 * Created by xuexin on 2017/3/14.
 */

public class ReadCallbackMap implements Resettable {
    private Map<BluetoothGattCharacteristic, ReadCallback>
            requestCharacteristicCallbackMap = new HashMap<>();

    public ReadCallback getCallback(BluetoothGattCharacteristic characteristic) {
        return requestCharacteristicCallbackMap.get(characteristic);
    }

    public boolean setCallback(BluetoothGattCharacteristic characteristic,
                               ReadCallback callback,
                               boolean cover) {
        if (cover) {
            requestCharacteristicCallbackMap.put(characteristic, callback);
            return true;
        } else {
            if (requestCharacteristicCallbackMap.containsKey(characteristic)) {
                BleLog.e("This characteristic Already has read callback");
                return false;
            } else {
                requestCharacteristicCallbackMap.put(characteristic, callback);
                return true;
            }
        }
    }

    public void removeCallback(BluetoothGattCharacteristic characteristic) {
        requestCharacteristicCallbackMap.remove(characteristic);
    }

    @Override
    public void reset() {
        requestCharacteristicCallbackMap.clear();
    }
}
