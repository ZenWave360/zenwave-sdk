package io.zenwave360.sdk.templating;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.upperCase;

public class CustomHandlebarsHelpers {

    public static String date(Object props, Options options) {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(OffsetDateTime.now());
    }
    public static String populateProperty(Map property, Options options) {
        String type = (String) property.get("type");
        String format = (String) property.get("format");
        List<String> enums = (List) property.get("enum");
        if (enums != null && !enums.isEmpty()) {
            String otherEntity = (String) property.get("x--schema-name");
            String openApiModelNameSuffix = options.hash("openApiModelNameSuffix", "");
            if(otherEntity == null) {
                return "\"null\"";
            }
            return String.format("%s%s.%s", otherEntity, openApiModelNameSuffix, upperCase(enums.get(0)));
        }
        if ("date".equals(format)) {
            return "new Date()";
        }
        if ("date-time".equals(format)) {
            return "Instant.now()";
        }
        if ("integer".equals(type) && (StringUtils.isEmpty(format) || "int32".equals(format))) {
            return "1";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "1L";
        }
        if ("number".equals(type)) {
            return "BigDecimal.valueOf(0)";
        }
        if ("boolean".equals(type)) {
            return "true";
        }
        if ("array".equals(type)) {
            var items = (Map<String, Object>) property.get("items");
            var propertyName = (String) property.get("x--property-name");
            return "null";
        }
        if (property.get("x--schema-name") != null) {
            // root level #/component/schemas would be an entity or enum
            String otherEntity = (String) property.get("x--schema-name");
            String propertyName = (String) property.get("x--property-name");
            String openApiModelNameSuffix = options.hash("openApiModelNameSuffix", "");
            return "new " + otherEntity + openApiModelNameSuffix + "()";
        }
        return "\"aaa\"";
    }


    public static Object jsonPath(Object entity, Options options) throws IOException {
        String jsonPath = StringUtils.join(options.params, "");
        Object defaultValue = options.hash.get("default");
        return JSONPath.get(entity, jsonPath, defaultValue);
    }

    public static String partial(String context, Options options) {
        var baseDir = FilenameUtils.getPath(options.fn.filename());
        var baseDirTokens = List.of(baseDir.split("/"));
        while(context.startsWith("../")) {
            context = context.substring(3);
            baseDirTokens = baseDirTokens.subList(0, baseDirTokens.size() - 1);
        }
        var tokens = new ArrayList<String>();
        tokens.addAll(baseDirTokens);
        tokens.add(context);
        tokens.addAll(Arrays.stream(options.params).map(Object::toString).collect(Collectors.toList()));
        tokens.replaceAll(t -> t.replaceAll("^\\./", "").replaceAll("^/", "").replaceAll("/$", ""));
        return StringUtils.join(tokens, "/");
    }

    public static String concat(String context, Options options) {
        var tokens = new ArrayList<>();
        tokens.add(context);
        tokens.addAll(List.of(options.params));
        return StringUtils.join(tokens, "");
    }

    public static String path(String context, Options options) {
        var tokens = new ArrayList<>();
        tokens.add(context);
        tokens.addAll(List.of(options.params));
        return StringUtils.join(tokens, "/").replaceAll("/+", "/");
    }

    public static boolean eq(Object first, Options options) throws IOException {
        Object second = options.param(0);
        return StringUtils.equals(String.valueOf(first), String.valueOf(second));
    }

    public static boolean neq(Object first, Options options) throws IOException {
        return !eq(first, options);
    }

    public static boolean startsWith(String first, Options options) throws IOException {
        String second = options.param(0);
        return StringUtils.startsWith(first, second);
    }

    public static boolean endsWith(String first, Options options) throws IOException {
        String second = options.param(0);
        return StringUtils.endsWith(first, second);
    }

    public static boolean contains(String first, Options options) throws IOException {
        String second = options.param(0);
        boolean ignoreCase = options.hash("ignoreCase", false);
        return ignoreCase? StringUtils.containsIgnoreCase(first, second) : StringUtils.contains(first, second);
    }


    public static Object ifTruthy(final Object value, final Options options)
            throws IOException {
        if (isTruthy(value)) {
            return options.param(0, "");
        }
        return options.param(1, "");
    }

    public static boolean not(Object value, Options options) throws IOException {
        if (value == null) {
            return true;
        }
        if(value instanceof Boolean) {
            return !((Boolean) value);
        }
        return Boolean.valueOf(String.valueOf(value)) == false;
    }

    public static boolean or(Object first, Options options) throws IOException {
        return isTruthy(first) || Stream.of(options.params).anyMatch(CustomHandlebarsHelpers::isTruthy);
    }

    public static boolean and(Object first, Options options) throws IOException {
        return isTruthy(first) && Stream.of(options.params).allMatch(CustomHandlebarsHelpers::isTruthy);
    }

    public static boolean isTruthy(Object value, Options options) throws IOException {
        return isTruthy(value);
    }

    public static boolean isFalsy(Object value, Options options) throws IOException {
        return !isTruthy(value);
    }

    private static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if(value.toString().trim().equals("")) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return !"false".equalsIgnoreCase(String.valueOf(value));
    }

    public static Object size(List list, Options options) throws IOException {
        return list != null? list.size() : 0;
    }

    public static Object uncapFirst(String text, Options options) throws IOException {
        return StringUtils.uncapitalize(text);
    }

    public static Object assign(final String variableName, final Options options) throws IOException {
        var context = options.context;
        while(context.parent() != null && context.parent().model() instanceof Map) {
            context = context.parent();
        }
        var model = (Map) context.model();
        if (options.params.length == 1) {
            if (options.param(0) != null) {
                model.put(variableName, options.param(0));
            } else {

            }
        } else {
            CharSequence finalValue = options.apply(options.fn);
            model.put(variableName, finalValue.toString().trim());
        }
        return null;
    }

    public static String asInstanceName(String text, Options options) throws IOException {
        return NamingUtils.asInstanceName(text);
    }

    public static String asJavaTypeName(String text, Options options) throws IOException {
        return NamingUtils.asJavaTypeName(text);
    }

    public static String asPackageFolder(String text, Options options) throws IOException {
        return NamingUtils.asPackageFolder(text);
    }

    public static String camelCase(String text, Options options) throws IOException {
        return text != null ? NamingUtils.camelCase(text) : null;
    }

    public static String kebabCase(String text, Options options) throws IOException {
        return text != null ? NamingUtils.kebabCase(text) : null;
    }

    public static String snakeCase(String text, Options options) throws IOException {
        return text != null ? NamingUtils.snakeCase(text) : null;
    }

    public static String joinWithTemplate(Object context, Options options) throws IOException {
        Collection<?> items = context instanceof Map? ((Map<?, ?>) context).entrySet() : (Collection<?>) context;
        String delimiter = options.hash("delimiter", "\n");
        boolean removeDuplicates = options.hash("removeDuplicates", false);
        if (removeDuplicates) {
            items = items.stream().distinct().collect(Collectors.toList());
        }
        return items.stream().map(token -> {
            try {
                return options.fn(token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.joining(delimiter));
    }

    public static String indent(String text, Options options) {
        var spaces = (int) options.hash("spaces", 0);
        return text.lines().map(l -> repeat(" ", spaces) + l).collect(Collectors.joining("\n"));
    }
}
