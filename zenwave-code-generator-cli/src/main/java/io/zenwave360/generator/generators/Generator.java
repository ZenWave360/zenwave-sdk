package io.zenwave360.generator.generators;

import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateOutput;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

public interface Generator {

    List<TemplateOutput> generate(Map<String, Object> contextModel);

    default Map<String, Object> asConfigurationMap() {
        return Generator.asConfigurationMap(this);
    }

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
