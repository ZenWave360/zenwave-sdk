package io.zenwave360.sdk.zdl.layouts;

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
    // asyncapi generated packages (not in use yet)
    public String asyncApiModelPackage = "asyncApiModelPackage"; // right now is outboundEventsModelPackage
    public String asyncApiProducerApiPackage = "asyncApiProducerApiPackage"; // right now is outboundEventsPackage
    public String asyncApiConsumerApiPackage = "asyncApiConsumerApiPackage"; // right now is adaptersCommandsPackage

    // common packages (for base classes in monolithic projects)
    public String entitiesCommonPackage = "entitiesCommonPackage";
    public String domainEventsCommonPackage = "domainEventsCommonPackage";
    public String coreImplementationCommonPackage = "coreImplementationCommonPackage";
    public String coreImplementationMappersCommonPackage = "coreImplementationMappersCommonPackage";
    public String infrastructureRepositoryCommonPackage = "infrastructureRepositoryCommonPackage";
    public String infrastructureEventsCommonPackage = "infrastructureEventsCommonPackage";
    public String adaptersWebCommonPackage = "adaptersWebCommonPackage";
    public String adaptersWebMappersCommonPackage = "adaptersWebMappersCommonPackage";
    public String adaptersCommandsCommonPackage = "adaptersCommandsCommonPackage";
    public String adaptersCommandsMappersCommonPackage = "adaptersCommandsMappersCommonPackage";
    public String adaptersEventsCommonPackage = "adaptersEventsCommonPackage";
    public String adaptersEventsMappersCommonPackage = "adaptersEventsMappersCommonPackage";

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

    public <T extends ProjectLayout> T processedLayout(Map<String, Object> options) {
        try {
            ProjectLayout layout = this.getClass().getConstructor().newInstance();
            layout.processLayoutPlaceHolders(options);
            return (T) layout;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void processLayoutPlaceHolders(Map<String, Object> options) {
        HandlebarsEngine engine = new HandlebarsEngine();
        try {
            var layoutAsMap = asMap();
            layoutAsMap.putAll(filterLayoutOptions(options));
            var model = new HashMap<String, Object>();
            model.putAll(layoutAsMap);
            model.putAll(options);
            for (int i = 0; i < 5; i++) { // 5 iterations should be enough to resolve all placeholders
                for (Map.Entry<String, Object> entry : layoutAsMap.entrySet()) {
                    if (entry.getValue() instanceof String value) {
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

    /**
     * Processes layout options and returns them as a map.
     * Removes the 'layout.' prefix from expressions in config option values.
     * This helps users by allowing them to use either format in their templates.
     *
     * This config:
     * layout.outboundEventsPackage: "{{layout.outboundPackage}}.events"
     * becomes this:
     * layout.outboundEventsPackage: "{{outboundPackage}}.events"
     */
    private Map<String, Object> filterLayoutOptions(Map<String, Object> options) {
        if (options == null) {
            return new HashMap<>();
        }
        Object layout = options.get("layout");
        Map<String, Object> layoutOptions = layout instanceof Map ? (Map) layout : new HashMap<>();
        for (Map.Entry<String, Object> entry : layoutOptions.entrySet()) {
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                if(value.contains("{{layout.")) {
                    value = value.replaceAll("\\{\\{layout\\.", "{{");
                    entry.setValue(value);
                }
            }
        }
        return layoutOptions;
    }
}
