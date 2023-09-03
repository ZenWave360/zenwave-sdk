package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZDLProcessor extends JDLProcessor {

    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> zdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;
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
        var entitiesToPopulate = (List<Map>) JSONPath.get(zdlModel, "$.allEntitiesAndEnums[*][?(@.options.copy)]");
        for (var entity : entitiesToPopulate) {
            var toCopyName = JSONPath.get(entity, "$.options.copy");
            var entityToCopy = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + toCopyName);
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
