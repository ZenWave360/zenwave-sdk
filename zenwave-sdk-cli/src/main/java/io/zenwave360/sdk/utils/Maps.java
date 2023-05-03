package io.zenwave360.sdk.utils;

import java.util.*;

/**
 * Mutable version of {@link java.util.Map} static methods and other utilities.
 */
public interface Maps {

    static <K, V> Map<K, V> of(K key, V value, Object... keyValues) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(key, value);
        if (keyValues != null) {
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
            source = new LinkedHashMap<>((Map) source);
            ((HashMap<String, Object>) source).entrySet().forEach(e -> e.setValue(deepCopy(e.getValue())));
        } else if(source instanceof List) {
            source = new ArrayList<>((List) source);
        }
        return source;
    }

    static Map deepMerge(Map target, Map extra) {
        for (Object key : extra.keySet()) {
            if (extra.get(key) instanceof Map) {
                Map subTarget = (Map) target.get(key);
                if (subTarget == null) {
                    subTarget = new HashMap();
                    target.put(key, subTarget);
                }
                deepMerge(subTarget, (Map) extra.get(key));
            } else {
                target.put(key, extra.get(key));
            }
        }
        return target;
    }
}
