package io.zenwave360.generator.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    static Map copy(Map source) {
        return (Map) deepCopy((Object) source);
    }

    private static Object deepCopy(Object source) {
        if(source instanceof Map) {
            source = new HashMap<>((Map) source);
            ((HashMap<String, Object>) source).entrySet().forEach(e -> e.setValue(deepCopy(e.getValue())));
        } else if(source instanceof List) {
            source = ((List<?>) source).stream().map(e -> deepCopy(e));
        }
        return source;
    }
}
