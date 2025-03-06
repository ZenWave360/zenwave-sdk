package io.zenwave360.sdk.zdl.layout;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.templating.HandlebarsEngine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectLayout {

    public String basePackage = "{{basePackage}}";

    // in case of modular project
    public String configPackage = "{{basePackage}}.config";
    public String commonPackage = "{{basePackage}}.common";
    public String modulesPackage = "{{basePackage}}.modules";

    // module specific
    public String moduleBasePackage = "{{basePackage}}";
    public String moduleConfigPackage = "{{moduleBasePackage}}.config";

    // domain entities and events
    public String entitiesPackage = "{{moduleBasePackage}}.core.domain";
    public String domainEventsPackage = "{{moduleBasePackage}}.core.domain.events";

    // inbound services / primary ports
    public String inboundPackage = "{{moduleBasePackage}}.core.inbound";
    public String inboundDtosPackage = "{{moduleBasePackage}}.core.inbound.dtos";

    // outbound / secondary ports
    public String outboundPackage = "{{moduleBasePackage}}.core.outbound";
    public String outboundRepositoryPackage = "{{moduleBasePackage}}.core.outbound.{{persistence}}";
    // outbound / secondary ports for events (internal and asyncapi)
    public String outboundEventsPackage = "{{moduleBasePackage}}.core.outbound.events";
    // asyncapi events dtos
    public String outboundEventsModelPackage = "{{moduleBasePackage}}.core.outbound.events.dtos";

    // core implementation / inner ring
    public String coreImplementationPackage = "{{moduleBasePackage}}.core.implementation";
    public String coreImplementationMappersPackage = "{{moduleBasePackage}}.core.implementation.mappers";

    // infrastructure / secondary adapters
    public String infrastructurePackage = "{{moduleBasePackage}}.infrastructure";
    public String infrastructureRepositoryPackage = "{{moduleBasePackage}}.infrastructure.{{persistence}}";
    // infrastructure / secondary adapters for events
    public String infrastructureEventsPackage = "{{moduleBasePackage}}.infrastructure.events";

    // primary adapters (web, events, commands)
    public String adaptersPackage = "{{moduleBasePackage}}.adapters";
    public String adaptersWebPackage = "{{moduleBasePackage}}.adapters.web";
    public String adaptersWebMappersPackage = "{{moduleBasePackage}}.adapters.web.mappers";
    public String adaptersCommandsPackage = "{{moduleBasePackage}}.adapters.commands";
    public String adaptersCommandsMappersPackage = "{{moduleBasePackage}}.adapters.commands.mappers";
    public String adaptersEventsPackage = "{{moduleBasePackage}}.adapters.events";
    public String adaptersEventsMappersPackage = "{{moduleBasePackage}}.adapters.events.mappers";

    // openapi generated packages
    public String openApiApiPackage = "{{adaptersWebPackage}}";
    public String openApiModelPackage = "{{adaptersWebPackage}}.dtos";

    @JsonAnySetter
    public Map<String, String> _additionalProperties;

    public Map<String, Object> asMap() {
        var map = new LinkedHashMap<String, Object>();
        var fields = this.getClass().getFields();
        for (Field field : fields) {
            try {
                var value = field.get(this);
                if (value instanceof String) {
                    map.put(field.getName(), (String) value);
                }
                if (value instanceof Map) {
                    map.putAll((Map) value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    public void processLayoutPlaceHolders(Map<String, Object> options) {
        HandlebarsEngine engine = new HandlebarsEngine();
        try {
            var layoutAsMap = asMap();
            layoutAsMap.putAll(filterLayoutOptions(options));
            var model = new HashMap<String, Object>();
            model.putAll(layoutAsMap);
            model.putAll(options);
            for (int i = 0; i < 3; i++) { // 3 iterations should be enough to resolve all placeholders
                for (Map.Entry<String, Object> entry : layoutAsMap.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        String processedValue = engine.processInline(value, model);
                        layoutAsMap.put(entry.getKey(), processedValue);
                    }
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.updateValue(this, layoutAsMap);

        } catch (IOException e) {
            throw new RuntimeException("Error processing layout placeholders", e);
        }
    }

    /* Filters layout options like "layout.outboundEventsPackage" and removes prefix */
    private Map<String, Object> filterLayoutOptions(Map<String, Object> options) {
        if (options == null) {
            return new HashMap<>();
        }
        Map<String, Object> layoutOptions = new HashMap<>();
        String prefix = "layout.";
        options.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                String newKey = key.substring(prefix.length());
                layoutOptions.put(newKey, value);
            }
        });
        return layoutOptions;
    }
}
