package ml.xuexin.bleconsultant.entity;

/**
 * Created by xuexin on 2017/3/21.
 */

public class UuidMap<T> extends DoubleKeyMap<String, T> {
    @Override
    public T get(String serviceUUID, String characteristicUUID) {
        return super.get(serviceUUID, characteristicUUID);
    }

    @Override
    public T put(String serviceUUID, String characteristicUUID, T value) {
        return super.put(serviceUUID, characteristicUUID, value);
    }

    @Override
    public T remove(String serviceUUID, String characteristicUUID) {
        return super.remove(serviceUUID, characteristicUUID);
    }
}
