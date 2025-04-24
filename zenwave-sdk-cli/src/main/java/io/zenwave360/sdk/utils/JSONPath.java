package io.zenwave360.sdk.utils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONPath {

    private static final Configuration config = Configuration.defaultConfiguration();

    private static final Configuration configForRemove = new Configuration.ConfigurationBuilder()
            .build().addOptions(Option.AS_PATH_LIST);

    public static <T> T get(Object object, String jsonPath) {
        if(object == null) {
            return null;
        }
        try {
            jsonPath = escapeByteArrayType(jsonPath);
            return (T) JsonPath.using(config).parse(object).read(jsonPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    public static <T> T getFirst(Object object, String... jsonPaths) {
        if(jsonPaths != null) {
            for (String jsonPath : jsonPaths) {
                var value = get(object, jsonPath);
                if(value != null) {
                    return (T) value;
                }
            }
        }
        return null;
    }

    public static <T> T get(Object object, String jsonPath, T defaultIfNull) {
        if(object == null) {
            return null;
        }
        try {
            jsonPath = escapeByteArrayType(jsonPath);
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

    public static int remove(Object object, String jsonPath) {
        int count = 0;
        if(object == null) {
            return count;
        }
        try {
            jsonPath = escapeByteArrayType(jsonPath);
            List<String> paths = JsonPath.using(configForRemove).parse(object).read(jsonPath);
            for (String path : paths) {
                String parentPath = path.substring(0, path.lastIndexOf('['));
                String lastToken = path.substring(path.lastIndexOf('['));
                Object parent = JsonPath.read(object, parentPath);
                if (parent instanceof Map) {
                    lastToken = lastToken.replaceAll("\\[\'(.+?)\'\\]", "$1");
                    var element = ((Map) parent).remove(lastToken);
                    if(element != null) {
                        count++;
                    }
                } else if (parent instanceof List) {
                    lastToken = lastToken.replaceAll("\\[(\\d+)\\]", "$1");
                    var element =((List) parent).remove(Integer.parseInt(lastToken));
                    if(element != null) {
                        count++;
                    }
                }
            }
        } catch (PathNotFoundException e) {
            // do nothing
        }
        return count;
    }


    /** 'byte[]' is a valid field type in ZDL so we need to test for it in 'entities', 'enums'... */
    private static String escapeByteArrayType(String jsonPath) {
        if(jsonPath == null) {
            return null;
        }
        return jsonPath.replace(".byte[]", "['byte[]']");
    }
}
