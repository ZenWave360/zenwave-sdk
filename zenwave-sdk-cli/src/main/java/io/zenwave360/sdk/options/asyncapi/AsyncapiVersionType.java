package io.zenwave360.sdk.options.asyncapi;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.Map;

public enum AsyncapiVersionType {
    v2,v3;

    public static boolean isV2(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("2.");
    }

    public static boolean isV3(Map apiModel) {
        return JSONPath.get(apiModel, "$.asyncapi", "2.0.0").startsWith("3.");
    }
}
