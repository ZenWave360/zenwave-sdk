package io.zenwave360.generator.processors.utils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.path.CompiledPath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JSONPath {

    public static <T> T get(Object object, String jsonPath) {
        try {
            return (T) JsonPath.read(object, jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    public static <T> T get(Object object, String jsonPath, T defaultIfNull) {
        try {
            return (T) JsonPath.read(object, jsonPath);
        } catch (PathNotFoundException e) {
            return defaultIfNull;
        }
    }

    /**
     * This implementation has some limitations: object must be of type Map and path must use '.' as separator.
     * @param object
     * @param jsonPath
     * @param value
     */
    public static void set(Object object, String jsonPath, Object value) {
        if(jsonPath.contains(".")) {
            String[] tokens = jsonPath.split("\\.");
            Object nested = object;
            for (String token : tokens) {
                try {
                    JsonPath.read(nested, token);
                } catch (PathNotFoundException e) {
                    ((Map) nested).put(token, new LinkedHashMap());
                    nested = ((Map<?, ?>) nested).get(token);
                }
            }
        }
        JsonPath.parse(object).set(jsonPath, value);;
    }
}
