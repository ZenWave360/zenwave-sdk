package io.zenwave360.generator.processors.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StringInterpolator {

    private ObjectMapper oMapper = new ObjectMapper();
    private static final StringInterpolator instance = new StringInterpolator();

    private Map<String, Object> flatenMap(Map<String, Object> context) {
        Map<String, Object> mapValue = new HashMap<>(context);
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getValue() instanceof Iterable) {
                flattenList(entry.getKey(), (Iterable) entry.getValue(), mapValue);
            } else if (!(entry.getValue() instanceof String) && !(entry.getValue() instanceof Boolean) && !(entry.getValue() instanceof Number)) {
                flattenObject(entry.getKey(), entry.getValue(), mapValue);
            }
        }
        return mapValue;
    }

    private void flattenList(String key, Iterable iterable, Map<String, Object> mapValue) {
        int i = 0;
        for (Object value : iterable) {
            String arrayKey = key + "[" + (i++) + "]";
            if (value instanceof Iterable) {
                flattenList(arrayKey, (Iterable) value, mapValue);
            }
            if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Map) {
                mapValue.put(arrayKey, value);
            } else {
                flattenObject(arrayKey, value, mapValue);
            }
        }
    }

    private void flattenObject(String key, Object object, Map<String, Object> mapValue) {
        Map<String, Object> map = oMapper.convertValue(object, Map.class);
        if (map == null) {
            return;
        }
        for (Map.Entry<String, Object> nestedEntry : map.entrySet()) {
            String nestedKey = key + "." + nestedEntry.getKey();
            if (nestedEntry.getValue() instanceof Iterable) {
                flattenList(nestedKey, (Iterable) nestedEntry.getValue(), mapValue);
            } else {
                mapValue.put(nestedKey, nestedEntry.getValue());
            }
        }
    }

    private String replaceAll(String source, Map<String, Object> context) {
        for (Map.Entry<String, Object> variable : context.entrySet()) {
            source = StringUtils.replace(source, "${" + variable.getKey() + "}", String.valueOf(variable.getValue()));
        }
        return source;
    }

    public static String interpolate(String source, Map<String, Object> context) {
        return instance.replaceAll(source, instance.flatenMap(context));
    }
}
