package io.zenwave360.generator.utils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public class NamingUtils {

    public static String plural(String name) {
        return name + "s"; // good enough for now
    }

    public static String asJavaTypeName(String name) {
        if(name == null) {
            return null;
        }
        String[] tokens = RegExUtils.replaceAll(name, "[\\s-.]", " ").split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if(StringUtils.isAllUpperCase(tokens[i])) {
                tokens[i] = tokens[i].toLowerCase();
            }
            tokens[i] = StringUtils.capitalize(tokens[i]);
        }
        return RegExUtils.removePattern(StringUtils.join(tokens), "^(\\d+)");
    }

    public static String asInstanceName(String name) {
        return StringUtils.uncapitalize(name);
    }

    public static String asJavaPropertyName(String name) {
        return StringUtils.uncapitalize(asJavaTypeName(name));
    }
}
