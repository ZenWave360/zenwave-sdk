package io.zenwave360.generator.plugins;

import io.zenwave360.generator.templating.TemplateOutput;

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

    List<TemplateOutput> generate(Map<String, ?> contextModel);

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

        return config;
    }
}
