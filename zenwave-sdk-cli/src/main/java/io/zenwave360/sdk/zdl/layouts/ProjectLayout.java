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

    public String basePackage = "{{asPackageFolder layout.basePackage}}";

    // in case of modular project
    public String configPackage = "{{asPackageFolder layout.configPackage}}";
    public String commonPackage = "{{asPackageFolder layout.commonPackage}}";
    public String modulesPackage = "{{asPackageFolder layout.modulesPackage}}";

    // module specific
    public String moduleBasePackage = "{{asPackageFolder layout.moduleBasePackage}}";
    public String moduleConfigPackage = "{{asPackageFolder layout.moduleConfigPackage}}";

    // domain entities and events
    public String entitiesPackage = "{{asPackageFolder layout.entitiesPackage}}";
    public String domainEventsPackage = "{{asPackageFolder layout.domainEventsPackage}}";

    // inbound services / primary ports
    public String inboundPackage = "{{asPackageFolder layout.inboundPackage}}";
    public String inboundDtosPackage = "{{asPackageFolder layout.inboundDtosPackage}}";

    // outbound / secondary ports
    public String outboundPackage = "{{asPackageFolder layout.outboundPackage}}";
    public String outboundRepositoryPackage = "{{asPackageFolder layout.outboundRepositoryPackage}}";
    // outbound / secondary ports for events (internal and asyncapi)
    public String outboundEventsPackage = "{{asPackageFolder layout.outboundEventsPackage}}";
    // asyncapi events dtos
    public String outboundEventsModelPackage = "{{asPackageFolder layout.outboundEventsModelPackage}}";

    // core implementation / inner ring
    public String coreImplementationPackage = "{{asPackageFolder layout.coreImplementationPackage}}";
    public String coreImplementationMappersPackage = "{{asPackageFolder layout.coreImplementationMappersPackage}}";

    // infrastructure / secondary adapters
    public String infrastructurePackage = "{{asPackageFolder layout.infrastructurePackage}}";
    public String infrastructureRepositoryPackage = "{{asPackageFolder layout.infrastructureRepositoryPackage}}";
    // infrastructure / secondary adapters for events
    public String infrastructureEventsPackage = "{{asPackageFolder layout.infrastructureEventsPackage}}";

    // primary adapters (web, events, commands)
    public String adaptersPackage = "{{asPackageFolder layout.adaptersPackage}}";
    public String adaptersWebPackage = "{{asPackageFolder layout.adaptersWebPackage}}";
    public String adaptersWebMappersPackage = "{{asPackageFolder layout.adaptersWebMappersPackage}}";
    public String adaptersCommandsPackage = "{{asPackageFolder layout.adaptersCommandsPackage}}";
    public String adaptersCommandsMappersPackage = "{{asPackageFolder layout.adaptersCommandsMappersPackage}}";
    public String adaptersEventsPackage = "{{asPackageFolder layout.adaptersEventsPackage}}";
    public String adaptersEventsMappersPackage = "{{asPackageFolder layout.adaptersEventsMappersPackage}}";

    // openapi generated packages
    public String openApiApiPackage = "{{asPackageFolder layout.openApiApiPackage}}";
    public String openApiModelPackage = "{{asPackageFolder layout.openApiModelPackage}}";
    // asyncapi generated packages (not in use yet)
    public String asyncApiModelPackage = "{{asPackageFolder layout.asyncApiModelPackage}}"; // right now is outboundEventsModelPackage
    public String asyncApiProducerApiPackage = "{{asPackageFolder layout.asyncApiProducerApiPackage}}"; // right now is outboundEventsPackage
    public String asyncApiConsumerApiPackage = "{{asPackageFolder layout.asyncApiConsumerApiPackage}}"; // right now is adaptersCommandsPackage

    // common packages (for base classes in monolithic projects)
    public String entitiesCommonPackage = "{{asPackageFolder layout.entitiesCommonPackage}}";
    public String domainEventsCommonPackage = "{{asPackageFolder layout.domainEventsCommonPackage}}";
    public String coreImplementationCommonPackage = "{{asPackageFolder layout.coreImplementationCommonPackage}}";
    public String coreImplementationMappersCommonPackage = "{{asPackageFolder layout.coreImplementationMappersCommonPackage}}";
    public String infrastructureRepositoryCommonPackage = "{{asPackageFolder layout.infrastructureRepositoryCommonPackage}}";
    public String infrastructureEventsCommonPackage = "{{asPackageFolder layout.infrastructureEventsCommonPackage}}";
    public String adaptersWebCommonPackage = "{{asPackageFolder layout.adaptersWebCommonPackage}}";
    public String adaptersWebMappersCommonPackage = "{{asPackageFolder layout.adaptersWebMappersCommonPackage}}";
    public String adaptersCommandsCommonPackage = "{{asPackageFolder layout.adaptersCommandsCommonPackage}}";
    public String adaptersCommandsMappersCommonPackage = "{{asPackageFolder layout.adaptersCommandsMappersCommonPackage}}";
    public String adaptersEventsCommonPackage = "{{asPackageFolder layout.adaptersEventsCommonPackage}}";
    public String adaptersEventsMappersCommonPackage = "{{asPackageFolder layout.adaptersEventsMappersCommonPackage}}";

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

    public String toString() {
        return this.getClass().getSimpleName();
    }
}
