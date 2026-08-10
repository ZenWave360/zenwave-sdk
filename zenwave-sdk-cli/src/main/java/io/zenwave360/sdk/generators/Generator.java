package io.zenwave360.sdk.generators;

import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

public abstract class Generator {

    @JsonIgnore
    public Plugin configuration;

    /**
     * Collects any configuration option that does not map to a declared field of the generator, so it can be forwarded
     * to templates. This is the single {@code @JsonAnySetter} for the whole {@link Generator} hierarchy. Entries are
     * flattened into the template model by {@link #asConfigurationMap(Object)}, so a pass-through option {@code id}
     * becomes {@code {{id}}} in templates. Values may be objects (Maps/Lists), e.g. an {@code x-server-id} extension.
     * <p>
     * {@code @JsonIgnore} keeps Jackson from rebinding the reference (a literal {@code additionalProperties} option is
     * captured as an entry via the any-setter instead), which is what lets subclasses alias this same map instance.
     */
    @JsonIgnore
    public Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalProperties.put(name, value);
    }

    @JsonIgnore
    private final HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    @JsonIgnore
    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    public abstract GeneratedProjectFiles generate(Map<String, Object> contextModel);

    public void onPropertiesSet() {
        // no op
    }

    public Map<String, Object> asConfigurationMap() {
        return Generator.asConfigurationMap(this);
    }

    public static Map<String, Object> asConfigurationMap(Object object) {
        Map<String, Object> config = new HashMap<>();
        Field[] fields = getAllFields(object.getClass());
        for (Field field : fields) {
            try {
                if (!isStatic(field.getModifiers()) && !isPrivate(field.getModifiers()) && field.canAccess(object) && !field.getName().startsWith("this$")) {
                    var value = field.get(object);
                    if (value instanceof ProjectLayout layout) {
                        config.put("layout", layout.asMap());
                    } else if ("additionalProperties".equals(field.getName()) && value instanceof Map) {
                        // flatten pass-through options to the top level so templates can use {{id}} / {{[x-server-id]}}
                        // Declared generator properties always take precedence, regardless of reflection field order.
                        ((Map<String, Object>) value).forEach(config::putIfAbsent);
                    } else {
                        config.put(field.getName(), field.get(object));
                    }
                }
            } catch (IllegalAccessException e) {
                config.put(field.getName(), e.getMessage());
            }
        }
        TemplateEngine templateEngine = new HandlebarsEngine();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            try {
                if (entry.getValue() instanceof String) {
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
