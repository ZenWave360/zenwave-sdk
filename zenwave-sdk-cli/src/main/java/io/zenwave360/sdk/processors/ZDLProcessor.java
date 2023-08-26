package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZDLProcessor extends JDLProcessor {

    public Map<String, Object> process(Map<String, Object> contextModel) {
        contextModel = super.process(contextModel);
        Map<String, Object> jdlModel = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;

        var entitiesToPopulate = (List<Map>) JSONPath.get(jdlModel, "$.allEntitiesAndEnums[*][?(@.options.copy)]");
        for (var entity : entitiesToPopulate) {
            var toCopyName = JSONPath.get(entity, "$.options.copy");
            var entityToCopy = JSONPath.get(jdlModel, "$.allEntitiesAndEnums." + toCopyName);
            var fieldsToCopy = JSONPath.get(entityToCopy, "$.fields", Map.of());
            var entityFields = JSONPath.get(entity, "$.fields", Map.of());

            var copiedFields = new LinkedHashMap<>();
            copiedFields.putAll(fieldsToCopy);
            copiedFields.putAll(entityFields);

            entityFields.clear();
            entityFields.putAll(copiedFields);
        }

        return contextModel;
    }
}
