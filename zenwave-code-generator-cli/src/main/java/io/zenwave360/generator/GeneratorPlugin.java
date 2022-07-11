package io.zenwave360.generator;

import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateOutput;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

public interface GeneratorPlugin {

    enum RoleType {
        PROVIDER, CLIENT
    }

    List<TemplateOutput> generate(Map<String, Object> contextModel);

    default Map<String, Object> asConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        Field[] fields = getAllFields(this.getClass());
        for(Field field: fields) {
            try {
                if(field.canAccess(this) && !field.getName().startsWith("this$")) {
                    config.put(field.getName(), field.get(this));
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
