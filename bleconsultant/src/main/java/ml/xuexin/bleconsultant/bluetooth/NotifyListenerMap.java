package ml.xuexin.bleconsultant.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.xuexin.bleconsultant.port.CharacteristicNotifyListener;

/**
 * Created by xuexin on 2017/3/8.
 */

public class NotifyListenerMap implements Resettable {
    private Map<BluetoothGattCharacteristic, List<CharacteristicNotifyListener>>
            notifyListenerMap = new HashMap<>();

    private Map<BluetoothGattCharacteristic, CharacteristicNotifyListener>
            monopolyMap = new HashMap<>();


    public void addNotifyListener(BluetoothGattCharacteristic characteristic,
                                  CharacteristicNotifyListener callback,
                                  boolean monopoly) {
        if (monopoly) {
            if (monopolyMap.containsKey(characteristic)) {
                throw new RuntimeException("Can not have multiple monopoly");
            }
            monopolyMap.put(characteristic, callback);
        } else {
            if (notifyListenerMap.containsKey(characteristic)) {
                notifyListenerMap.get(characteristic).add(callback);
            } else {
                ArrayList<CharacteristicNotifyListener> list = new ArrayList<>();
                notifyListenerMap.put(characteristic, list);
                list.add(callback);
            }
        }
    }

    public void removeNotifyListener(BluetoothGattCharacteristic characteristic,
                                     CharacteristicNotifyListener callback) {
        monopolyMap.remove(characteristic);
        List<CharacteristicNotifyListener> list = notifyListenerMap.get(characteristic);
        if (list != null) {
            list.remove(callback);
        }
    }

    public boolean hasNotifyListener(BluetoothGattCharacteristic characteristic) {
        boolean has = monopolyMap.containsKey(characteristic);
        if (has)
            return true;
        if (notifyListenerMap.containsKey(characteristic)) {
            List<CharacteristicNotifyListener> list = notifyListenerMap.get(characteristic);
            if (list != null) {
                return list.size() > 0;
            }
        }
        return false;
    }


    public List<CharacteristicNotifyListener> getListenerList(
            BluetoothGattCharacteristic characteristic) {
        if (monopolyMap.get(characteristic) != null) {
            ArrayList<CharacteristicNotifyListener> list = new ArrayList<>(1);
            list.add(monopolyMap.get(characteristic));
            return list;
        } else {
            List<CharacteristicNotifyListener> list = notifyListenerMap.get(characteristic);
            if (list == null) {
                list = new ArrayList<>();
            }
            return list;
        }
    }

    @Override
    public void reset() {
        monopolyMap.clear();
        notifyListenerMap.clear();
    }
}
