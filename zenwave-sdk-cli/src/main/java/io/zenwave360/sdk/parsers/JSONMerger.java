package io.zenwave360.sdk.parsers;

import java.util.Map;

public class JSONMerger {

    public static Map<String, Object> merge(Map<String, Object> source, Map<String, Object> target) {
        for (String key : source.keySet()) {
            if (source.get(key) instanceof Map && target.get(key) instanceof Map) {
                Map<String, Object> nestedSource = (Map<String, Object>) source.get(key);
                Map<String, Object> nestedTarget = (Map<String, Object>) target.get(key);
                target.put(key, merge(nestedSource, nestedTarget));
            } else {
                target.put(key, source.get(key));
            }
        }
        return target;
    }
}
