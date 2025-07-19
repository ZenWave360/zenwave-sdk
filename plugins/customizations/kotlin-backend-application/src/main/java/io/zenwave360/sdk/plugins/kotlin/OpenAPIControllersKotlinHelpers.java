package io.zenwave360.sdk.plugins.kotlin;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLHttpUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.upperCase;

public class OpenAPIControllersKotlinHelpers {

    public final String openApiModelNamePrefix;
    public final String openApiModelNameSuffix;

    public OpenAPIControllersKotlinHelpers(String openApiModelNamePrefix, String openApiModelNameSuffix) {
        this.openApiModelNamePrefix = openApiModelNamePrefix;
        this.openApiModelNameSuffix = openApiModelNameSuffix;
    }

    public String methodParameters(Map operation, Options options) {
        var requiredFields = JSONPath.get(operation, "$.requestBody.content..schema.required[*]", List.<String>of());
        var fieldsWitDefault = JSONPath.get(operation, "$.requestBody.content..schema.properties[*][?(@.default && !(@.default == 'null'))].x--property-name", List.of());
        var requiredParams = JSONPath.get(operation, "parameters[?(@.required == true)].name", List.<String>of());
        var paramsWithDefault = JSONPath.get(operation, "$.parameters[*][?(@.schema.default)].name", List.<String>of());
        var required = Stream.of(requiredFields, fieldsWitDefault, requiredParams, paramsWithDefault, List.of("reqBody", "input")).flatMap(List::stream).distinct().toList();
        return ZDLHttpUtils.methodParameters(operation, openApiModelNamePrefix, openApiModelNameSuffix).stream().map(param -> {
            return param.getValue() + ": " + param.getKey() + (required.contains(param.getValue()) ? "" : "?");
        }).collect(Collectors.joining(", "))
                .replaceAll("Integer", "Int")
                .replaceAll("Map", "PatchMap");
    }

    public String voidUnit(String returnType, Options options) {
        return returnType.replace("void", "Unit").replace("Void", "Unit");
    }

    public CharSequence asMethodParametersInitializer(Map<String, Object> operation, Options options) throws IOException {
        var openapi = (Map) options.get("openapi");
        var methodParams = ZDLHttpUtils.methodParameters((Map) operation, openApiModelNamePrefix, openApiModelNameSuffix);
        if(methodParams.isEmpty()) {
            return "";
        }
        return methodParams.stream()
                .map(param -> "val " + param.getValue() + ": " + param.getKey() + " = " + instantiateVar(openapi, param.getValue(), param.getKey()))
                .collect(Collectors.joining("\n"))
                .replaceAll("Map = mutableMapOf", "Map<String, Any?> = mutableMapOf")
                .replaceAll(" Integer ", " Int ");
    }

    private String instantiateVar(Map<String, Object> api, String varName, String type) {
        // Handle primitive types
        if ("Int".equals(type) || "Integer".equals(type)) {
            return "0";
        }
        if ("Long".equals(type)) {
            return "0L";
        }
        if ("String".equals(type)) {
            return "\"\"";
        }
        if ("Boolean".equals(type)) {
            return "false";
        }
        if ("Double".equals(type)) {
            return "0.0";
        }
        if ("Float".equals(type)) {
            return "0.0f";
        }
        if ("LocalDate".equals(type)) {
            return "java.time.LocalDate.now()";
        }

        // Handle collections
        if (type.startsWith("List<") || type.startsWith("MutableList<")) {
            return "mutableListOf()";
        }
        if (type.startsWith("Set<") || type.startsWith("MutableSet<")) {
            return "mutableSetOf()";
        }
        if ("Map".equals(type) || type.startsWith("Map<") || type.startsWith("MutableMap<") || type.startsWith("PatchMap<")) {
            return "mutableMapOf()";
        }

        // Handle special types
        if (type.contains("MultipartFile")) {
            return "org.springframework.mock.web.MockMultipartFile(\"file\", \"test.txt\", \"text/plain\", \"test content\".toByteArray())";
        }
        if (type.contains("ByteArray")) {
            return "byteArrayOf()";
        }

        // Handle DTOs and other objects
        if (type.endsWith("DTO") || type.endsWith(openApiModelNameSuffix)) {
            return type + "(" + populateConstructor(api, type) + ")";
        }

        // Default to null for unknown types
        return "null";
    }

    private String populateConstructor(Map api, String type) {
        var dtoName = type.substring(openApiModelNamePrefix.length(), type.length() - openApiModelNameSuffix.length());
        var schema = JSONPath.get(api, "$.components.schemas." + dtoName);
        if(schema == null) {
            return "";
        }
        var required = JSONPath.get(schema, "$.required", List.of());
        var properties = JSONPath.get(schema, "$.properties", Map.<String, Map<String, Object>>of());
        return properties.entrySet().stream()
                .filter(entry -> required.contains(entry.getKey()))
                .map(entry -> entry.getKey() + " = " + populateProperty(entry.getValue(), openApiModelNamePrefix, openApiModelNameSuffix))
                .collect(Collectors.joining(", "));
    }

    public static String populateProperty(Map property, String openApiModelNamePrefix, String openApiModelNameSuffix) {
        String type = (String) property.get("type");
        String format = (String) property.get("format");
        List<String> enums = (List) property.get("enum");
        if (enums != null && !enums.isEmpty()) {
            String otherEntity = (String) property.get("x--schema-name");
            if(otherEntity == null) {
                return "null";
            }
            return String.format("%s%s.%s", otherEntity, openApiModelNameSuffix, upperCase(enums.get(0)));
        }
        if ("date".equals(format)) {
            return "LocalDate.now()";
        }
        if ("date-time".equals(format)) {
            return "java.time.Instant.now()";
        }
        if ("integer".equals(type) && (StringUtils.isEmpty(format) || "int32".equals(format))) {
            return "1";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "1L";
        }
        if ("number".equals(type)) {
            return "java.math.BigDecimal.valueOf(0)";
        }
        if ("boolean".equals(type)) {
            return "true";
        }
        if ("array".equals(type)) {
            var items = (Map<String, Object>) property.get("items");
            var propertyName = (String) property.get("x--property-name");
            return "mutableListOf()";
        }
        if (property.get("x--schema-name") != null) {
            // root level #/component/schemas would be an entity or enum
            String otherEntity = (String) property.get("x--schema-name");
            String propertyName = (String) property.get("x--property-name");
            return otherEntity + openApiModelNameSuffix + "()";
        }
        return "\"aaa\"";
    }

}
