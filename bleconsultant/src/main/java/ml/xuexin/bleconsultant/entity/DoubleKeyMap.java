package ml.xuexin.bleconsultant.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xuexin on 2017/3/21.
 */

public class DoubleKeyMap<K, T> {
    private Map<K, Map<K, T>> map = new HashMap<>();

    public T get(K key1, K key2) {
        try {
            return map.get(key1).get(key2);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public T put(K key1, K key2, T value) {
        Map<K, T> tmpMap = map.get(key1);
        if (tmpMap == null) {
            tmpMap = new HashMap<>();
            map.put(key1, tmpMap);
        }
        return tmpMap.put(key2, value);
    }

    public void clear() {
        map.clear();
    }

    public T remove(K key1, K key2) {
        try {
            return map.get(key1).remove(key2);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
