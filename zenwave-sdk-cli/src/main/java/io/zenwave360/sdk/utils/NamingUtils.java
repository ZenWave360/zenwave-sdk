package io.zenwave360.sdk.utils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public class NamingUtils {

    public static String plural(String name) {
        return name + "s"; // good enough for now
    }

    public static String asJavaTypeName(String name) {
        return RegExUtils.removePattern(camelCase(name), "^(\\d+)");
    }

    public static String asInstanceName(String name) {
        if(name == null) {
            return null;
        }
        return StringUtils.uncapitalize(camelCase(name)).replaceAll("<", "").replaceAll(">", "");
    }

    public static String asPackageFolder(String text) {
        return text != null ? text.replaceAll("\\.", "/") : null;
    }

    public static String camelCase(String name) {
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

    public static String humanReadable(String value) {
        // value = value.replaceAll("([A-Z])([a-z])", "-$1$2").toLowerCase();
        value = value.replaceAll("([a-z])([A-Z])", "$1-$2");
        value = value.replaceAll(" ", "-");
        value = value.replaceAll("--", "-");
        value = value.replaceAll("-", " ");
        return value;
    }


    public static String snakeCase(String value) {
        return kebabCase(value).replaceAll("-", "_");
    }
}
