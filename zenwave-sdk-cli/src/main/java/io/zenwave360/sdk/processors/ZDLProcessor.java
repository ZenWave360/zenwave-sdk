package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ZDLProcessor extends JDLProcessor {

    private Logger log = LoggerFactory.getLogger(getClass());

    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> zdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
        if(zdlModel == null) {
            return contextModel;
        }

        processServiceName(zdlModel);

        contextModel = super.process(contextModel);

        processCopyAnnotation(zdlModel);

        return contextModel;
    }

    public void processServiceName(Map<String, Object> zdlModel) {
        var services = JSONPath.get(zdlModel, "$.services", Map.of());
        for (Map.Entry<Object, Object> service : services.entrySet()) {
            var aggregates = JSONPath.get(service.getValue(), "$.aggregates", List.of());
            for (Object aggregate : aggregates) {
                JSONPath.set(zdlModel, "$.entities." + aggregate + ".options.service", service.getKey());
            }
        }
    }

    public void processCopyAnnotation(Map<String, Object> zdlModel) {
        var allEntities = new HashMap<>();
        allEntities.putAll(JSONPath.get(zdlModel, "$.allEntitiesAndEnums"));
        allEntities.putAll(JSONPath.get(zdlModel, "$.events"));
        var entitiesToPopulate = new ArrayList<Map>();
        entitiesToPopulate.addAll(JSONPath.get(zdlModel, "$.allEntitiesAndEnums[*][?(@.options.copy)]"));
        entitiesToPopulate.addAll(JSONPath.get(zdlModel, "$.events[*][?(@.options.copy)]"));
        for (var entity : entitiesToPopulate) {
            var toCopyName = JSONPath.get(entity, "$.options.copy");
            var entityToCopy = JSONPath.get(allEntities, "$." + toCopyName);
            if(entityToCopy == null) {
                log.error("Error Processing @copy({}) in {} {}", toCopyName, JSONPath.get(entity, "$.type"), JSONPath.get(entity, "$.name"));
                continue;
            }
            var fieldsToCopy = JSONPath.get(entityToCopy, "$.fields", Map.of());
            var entityFields = JSONPath.get(entity, "$.fields", Map.of());

            var copiedFields = new LinkedHashMap<>();
            copiedFields.putAll(fieldsToCopy);
            copiedFields.putAll(entityFields);

            entityFields.clear();
            entityFields.putAll(copiedFields);
        }
    }
}
