package io.zenwave360.generator.processors.utils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class JSONPath {

    public static <T> T get(Object object, String jsonPath) {
        try {
            return (T) JsonPath.read(object, jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }
}
