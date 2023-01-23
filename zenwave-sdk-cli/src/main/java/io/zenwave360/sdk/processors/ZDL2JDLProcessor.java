package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ZDL2JDLProcessor extends AbstractBaseProcessor {

    public ZDL2JDLProcessor() {
        this.targetProperty = "zdl";
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> model = targetProperty != null ? (Map) contextModel.get(targetProperty) : (Map) contextModel;

        var services = (Map<String, Map>) Maps.copy(JSONPath.get(model, "$.services"));
        for (Map service : services.values()) {
            service.put("entityNames", service.get("aggregates"));
            service.put("value", service.get("name"));
            service.put("name", "service");

            var entityNames = (List) service.get("aggregates");
            for (Object entityName : entityNames) {
                var entity = JSONPath.get(model, "$.entities['" + entityName + "']");
                var options = JSONPath.get(entity, "$.options", new HashMap<>());
                options.put("service", service.get("name"));
                JSONPath.set(entity, "$.options", options);
            }
        }
        model.put("options", Maps.of("options", Maps.of("service", services)));

        boolean withElasticsearch = !JSONPath.get(model, "$.entities[*].options.search", List.of()).isEmpty();
        if (withElasticsearch) {
            JSONPath.set(model, "$.options.options.search", true);
        }

        var enums = JSONPath.get(model, "$.enums.enums[*].name", List.of());
        var entities = JSONPath.get(model, "$.entities[*].name", List.of());
        var fields = JSONPath.get(model, "$.entities[*].fields.[*]", List.of());
        for (Object field : fields) {
            var fieldType = JSONPath.get(field, "$.type");
            if (enums.contains(fieldType)) {
                JSONPath.set(field, "$.isEnum", true);
            }
            if (entities.contains(fieldType)) {
                JSONPath.set(field, "$.isEntity", true);
            }
        }

        return contextModel;
    }

}
