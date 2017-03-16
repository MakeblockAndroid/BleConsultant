package ml.xuexin.bleconsultant.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xuexin on 2017/3/8.
 */

public class CharacteristicMap implements Resettable{
    private Map<UUID, Map<UUID, BluetoothGattCharacteristic>> characteristicMap = new HashMap<>();

    public void setCharacteristics(List<BluetoothGattService> services) {
        if (services == null) {
            throw new RuntimeException("List of services is null");
        }
        for (BluetoothGattService bluetoothGattService : services) {
            Map<UUID, BluetoothGattCharacteristic> map = new HashMap<>();
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                    bluetoothGattService.getCharacteristics()) {
                map.put(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic);
            }
            characteristicMap.put(bluetoothGattService.getUuid(), map);
        }
    }

    /**
     * ignore NullPointerException
     *
     * @param serviceUuid
     * @param characteristicUuid
     * @return
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        try {
            return characteristicMap.get(serviceUuid).get(characteristicUuid);
        } catch (NullPointerException e) {
        }
        return null;
    }

    @Override
    public void reset() {
        characteristicMap.clear();

    }
}
