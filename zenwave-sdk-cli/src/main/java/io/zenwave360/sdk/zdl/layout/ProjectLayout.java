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

    public String basePackage = "basePackage";

    // in case of modular project
    public String configPackage = "configPackage";
    public String commonPackage = "commonPackage";
    public String modulesPackage = "modulesPackage";

    // module specific
    public String moduleBasePackage = "moduleBasePackage";
    public String moduleConfigPackage = "moduleConfigPackage";

    // domain entities and events
    public String entitiesPackage = "entitiesPackage";
    public String domainEventsPackage = "domainEventsPackage";

    // inbound services / primary ports
    public String inboundPackage = "inboundPackage";
    public String inboundDtosPackage = "inboundDtosPackage";

    // outbound / secondary ports
    public String outboundPackage = "outboundPackage";
    public String outboundRepositoryPackage = "outboundRepositoryPackage";
    // outbound / secondary ports for events (internal and asyncapi)
    public String outboundEventsPackage = "outboundEventsPackage";
    // asyncapi events dtos
    public String outboundEventsModelPackage = "outboundEventsModelPackage";

    // core implementation / inner ring
    public String coreImplementationPackage = "coreImplementationPackage";
    public String coreImplementationMappersPackage = "coreImplementationMappersPackage";

    // infrastructure / secondary adapters
    public String infrastructurePackage = "infrastructurePackage";
    public String infrastructureRepositoryPackage = "infrastructureRepositoryPackage";
    // infrastructure / secondary adapters for events
    public String infrastructureEventsPackage = "infrastructureEventsPackage";

    // primary adapters (web, events, commands)
    public String adaptersPackage = "adaptersPackage";
    public String adaptersWebPackage = "adaptersWebPackage";
    public String adaptersWebMappersPackage = "adaptersWebMappersPackage";
    public String adaptersCommandsPackage = "adaptersCommandsPackage";
    public String adaptersCommandsMappersPackage = "adaptersCommandsMappersPackage";
    public String adaptersEventsPackage = "adaptersEventsPackage";
    public String adaptersEventsMappersPackage = "adaptersEventsMappersPackage";

    // openapi generated packages
    public String openApiApiPackage = "openApiApiPackage";
    public String openApiModelPackage = "openApiModelPackage";

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
