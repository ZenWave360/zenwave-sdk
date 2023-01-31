package io.zenwave360.sdk.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class JSONPath {

    private static final Configuration config = Configuration.defaultConfiguration();

    public static <T> T get(Object object, String jsonPath) {
        try {
            return (T) JsonPath.using(config).parse(object).read(jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    public static <T> T get(Object object, String jsonPath, T defaultIfNull) {
        try {
            return ObjectUtils.firstNonNull(JsonPath.using(config).parse(object).read(jsonPath), defaultIfNull);
        } catch (PathNotFoundException e) {
            return defaultIfNull;
        }
    }

    /**
     * This implementation has some limitations: object must be of type Map and path must use '.' as separator.
     * 
     * @param object
     * @param jsonPath
     * @param value
     */
    public static void set(Object object, String jsonPath, Object value) {
        if (jsonPath.contains(".")) {
            String[] tokens = jsonPath.split("\\.");
            Object nested = object;
            for (String token : tokens) {
                try {
                    nested = JsonPath.read(nested, token);
                } catch (PathNotFoundException e) {
                    ((Map) nested).put(token, new LinkedHashMap());
                    nested = ((Map<?, ?>) nested).get(token);
                }
            }
        }
        JsonPath.parse(object).set(jsonPath, value);
    }
}
