package io.zenwave360.generator;

import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

public class Utils {

    public static Map<String, Object> asConfigurationMap(Object object) {
        Map<String, Object> config = new HashMap<>();
        Field[] fields = getAllFields(object.getClass());
        for(Field field: fields) {
            try {
                if(!isStatic(field.getModifiers()) && field.canAccess(object) && !field.getName().startsWith("this$")) {
                    config.put(field.getName(), field.get(object));
                }
            } catch (IllegalAccessException e) {
                config.put(field.getName(), e.getMessage());
            }
        }
        TemplateEngine templateEngine = new HandlebarsEngine();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            try {
                if(entry.getValue() instanceof String) {
                    String value = templateEngine.processInline((String) entry.getValue(), config);
                    entry.setValue(value);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }
}
