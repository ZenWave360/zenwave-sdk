package io.zenwave360.generator.utils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public class NamingUtils {

    public static String plural(String name) {
        return name + "s"; // good enough for now
    }

    public static String asJavaTypeName(String name) {
        return RegExUtils.removePattern(asCamelCase(name), "^(\\d+)");
    }

    public static String asInstanceName(String name) {
        return StringUtils.uncapitalize(asCamelCase(name));
    }

    public static String asCamelCase(String name) {
        if (name == null) {
            return null;
        }
        String[] tokens = RegExUtils.replaceAll(name, "[\\s-.]", " ").split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (StringUtils.isAllUpperCase(tokens[i])) {
                tokens[i] = tokens[i].toLowerCase();
            }
            tokens[i] = StringUtils.capitalize(tokens[i]);
        }
        return StringUtils.join(tokens);
    }

    public static String kebabCase(String value) {
        // value = value.replaceAll("([A-Z])([a-z])", "-$1$2").toLowerCase();
        value = value.replaceAll("([a-z])([A-Z])", "$1-$2");
        value = value.replaceAll(" ", "-");
        value = value.replaceAll("--", "-");
        return value.toLowerCase();
    }

    public static String snakeCase(String value) {
        return kebabCase(value).replaceAll("-", "_");
    }
}
