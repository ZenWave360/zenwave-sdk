package io.zenwave360.sdk.plugins.kotlin;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.generators.JsonSchemaToJsonFaker;
import io.zenwave360.sdk.utils.JSONPath;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringWebTestClientKotlinHelpers {

    private String openApiModelNamePrefix = "";
    private String openApiModelNameSuffix = "";
    private final JsonSchemaToJsonFaker jsonSchemaToJsonFaker = new JsonSchemaToJsonFaker();

    public SpringWebTestClientKotlinHelpers(String openApiModelNamePrefix, String openApiModelNameSuffix) {
        this.openApiModelNamePrefix = openApiModelNamePrefix;
        this.openApiModelNameSuffix = openApiModelNameSuffix;
    }

    /**
     * Creates a new property object based on its type.
     */
    public String newPropertyObject(Map<String, Object> property, Options options) throws IOException {
        boolean isObject = "object".equals(property.get("type"));
        boolean isArray = "array".equals(property.get("type"));
        String schemaName = (String) property.get("x--schema-name");
        return isArray? "mutableListOf()" : isObject? String.format("%s()", asDtoName(schemaName)) : "null";
    }

    /**
     * Helper method to format a schema name as a DTO name.
     */
    private String asDtoName(String name) {
        return openApiModelNamePrefix + name + openApiModelNameSuffix;
    }
}
