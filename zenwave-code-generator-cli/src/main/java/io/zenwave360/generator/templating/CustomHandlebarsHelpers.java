package io.zenwave360.generator.templating;

import com.github.jknack.handlebars.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomHandlebarsHelpers {

    public static Object size(List list, Options options) throws IOException {
        return list.size();
    }

    public static Object uncapFirst(String text, Options options) throws IOException {
        return StringUtils.uncapitalize(text);
    }

    public static Object assign(final String variableName, final Options options) throws IOException {
        CharSequence finalValue = options.apply(options.fn);
        options.context.combine(Map.of(variableName, finalValue.toString().trim()));
//        ((Map) options.context.model()).put(variableName, finalValue.toString().trim());
        return null;
    }

    public static String capitalize(String text, Options options) throws IOException {
        return StringUtils.capitalize(text);
    }

    public static String asJavaProperty(String text, Options options) throws IOException {
        return text;
    }

    public static String asCapitalizedJavaProperty(String text, Options options) throws IOException {
        return StringUtils.capitalize(text);
    }

    public static String joinWithTemplate(List<Object> context, Options options) throws IOException {
        String delimiter = options.params.length > 0? options.params[0].toString() : "\n";
        return context.stream().map(token -> {
            try {
                return options.apply(options.fn, token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.joining(delimiter));
    }
}
