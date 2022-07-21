package io.zenwave360.generator.processors.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable version of {@link java.util.Map} static methods and other utilities.
 */
public interface Maps {

    static <K, V> Map<K, V> of(K key, V value, Object ...keyValues) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(key, value);
        if(keyValues != null) {
            int i = 0;
            while (i < keyValues.length) {
                K k = (K) keyValues[i++];
                V v = (V) keyValues[i++];
                map.put(k, v);
            }
        }
        return map;
    }

}
