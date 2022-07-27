package io.zenwave360.generator.templating;

import com.github.jknack.handlebars.Options;
import io.zenwave360.generator.utils.NamingUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
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
        if(options.params.length == 1) {
            if(options.param(0) != null) {
                options.context.combine(Map.of(variableName, options.param(0)));
            } else {

            }
        } else {
            CharSequence finalValue = options.apply(options.fn);
            options.context.combine(Map.of(variableName, finalValue.toString().trim()));
        }
        return null;
    }

    public static String asCapitalizedJavaProperty(String text, Options options) throws IOException {
        return NamingUtils.asJavaTypeName(text);
    }

    public static String asPackageFolder(String text, Options options) throws IOException {
        return text != null? text.replaceAll("\\.", "/") : null;
    }

    public static String joinWithTemplate(Collection<Object> context, Options options) throws IOException {
        String delimiter = options.params.length > 0? options.params[0].toString() : "\n";
        boolean removeDuplicates = options.hash("removeDuplicates", false);
        if(removeDuplicates) {
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
