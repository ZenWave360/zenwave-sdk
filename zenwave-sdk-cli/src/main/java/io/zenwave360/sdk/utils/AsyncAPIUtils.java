package io.zenwave360.sdk.utils;

import java.util.Map;

public class AsyncAPIUtils {

    public static boolean isV2(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("2.");
    }

    public static boolean isV3(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("3.");
    }
}
