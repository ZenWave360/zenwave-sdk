package io.zenwave360.generator.templating;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Options;

import io.zenwave360.generator.utils.NamingUtils;

public class CustomHandlebarsHelpers {

    public static String partial(String context, Options options) {
        var baseDir = FilenameUtils.getPath(options.fn.filename());
        var tokens = new ArrayList<String>();
        tokens.add(context);
        ((List) tokens).addAll(List.of(options.params));
        tokens.replaceAll(t -> t.replaceAll("^\\./", "").replaceAll("^/", "").replaceAll("/$", ""));
        return baseDir + StringUtils.join(tokens, "/");
    }

    public static String concat(String context, Options options) {
        var tokens = new ArrayList<>();
        tokens.add(context);
        tokens.addAll(List.of(options.params));
        return StringUtils.join(tokens, "");
    }

    public static boolean eq(String first, Options options) throws IOException {
        String second = options.param(0);
        return StringUtils.equals(first, second);
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

    public static Object size(List list, Options options) throws IOException {
        return list.size();
    }

    public static Object uncapFirst(String text, Options options) throws IOException {
        return StringUtils.uncapitalize(text);
    }

    public static Object assign(final String variableName, final Options options) throws IOException {
        if (options.params.length == 1) {
            if (options.param(0) != null) {
                options.context.combine(Map.of(variableName, options.param(0)));
            } else {

            }
        } else {
            CharSequence finalValue = options.apply(options.fn);
            options.context.combine(Map.of(variableName, finalValue.toString().trim()));
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
        return text != null ? text.replaceAll("\\.", "/") : null;
    }

    public static String kebabCase(String text, Options options) throws IOException {
        return text != null ? NamingUtils.kebabCase(text) : null;
    }

    public static String snakeCase(String text, Options options) throws IOException {
        return text != null ? NamingUtils.snakeCase(text) : null;
    }

    public static String joinWithTemplate(Collection<Object> context, Options options) throws IOException {
        String delimiter = options.params.length > 0 ? options.params[0].toString() : "\n";
        boolean removeDuplicates = options.hash("removeDuplicates", false);
        if (removeDuplicates) {
            context = context.stream().distinct().collect(Collectors.toList());
        }
        return context.stream().map(token -> {
            try {
                return options.apply(options.fn, token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.joining(delimiter));
    }
}
