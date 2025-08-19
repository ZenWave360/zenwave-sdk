package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.generators.JsonSchemaToJsonFaker;
import io.zenwave360.sdk.utils.JSONPath;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringWebTestClientHelpers {

    private String openApiModelNamePrefix = "";
    private String openApiModelNameSuffix = "";
    private final JsonSchemaToJsonFaker jsonSchemaToJsonFaker = new JsonSchemaToJsonFaker();

    public SpringWebTestClientHelpers(String openApiModelNamePrefix, String openApiModelNameSuffix) {
        this.openApiModelNamePrefix = openApiModelNamePrefix;
        this.openApiModelNameSuffix = openApiModelNameSuffix;
    }

    /**
     * Generates a JSON example from a schema.
     */
    public String requestExample(Object schema, Options options) throws IOException {
        return jsonSchemaToJsonFaker.generateExampleAsJson((Map) schema);
    }

    /**
     * Formats a schema name as a DTO name.
     */
    public String asDtoName(String dtoSchemaName, Options options) throws IOException {
        if (dtoSchemaName == null) {
            return null;
        }
        return asDtoName(dtoSchemaName);
    }

    /**
     * Creates a new property object based on its type.
     */
    public String newPropertyObject(Map<String, Object> property, Options options) throws IOException {
        boolean isObject = "object".equals(property.get("type"));
        boolean isArray = "array".equals(property.get("type"));
        String schemaName = (String) property.get("x--schema-name");
        return isArray? "new java.util.ArrayList<>()" : isObject? String.format("new %s()", asDtoName(schemaName)) : "null";
    }

    /**
     * Gets query parameters from an operation.
     */
    public List queryParams(Object operation, Options options) throws IOException {
        return JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream()
                .filter(p -> "query".equals(p.get("in")))
                .collect(Collectors.toList());
    }

    /**
     * Gets path parameters from an operation.
     */
    public List pathParams(Object operation, Options options) throws IOException {
        return JSONPath.get(operation, "parameters", Collections.<Map>emptyList()).stream()
                .filter(p -> "path".equals(p.get("in")))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to format a schema name as a DTO name.
     */
    private String asDtoName(String name) {
        return openApiModelNamePrefix + name + openApiModelNameSuffix;
    }
}
