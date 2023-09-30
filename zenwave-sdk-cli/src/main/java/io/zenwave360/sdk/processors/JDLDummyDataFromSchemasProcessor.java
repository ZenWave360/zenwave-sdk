package io.zenwave360.sdk.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;

public class JDLDummyDataFromSchemasProcessor extends AbstractBaseProcessor {

    public String jdlProperty = "zdl";
    public String apiProperty = "api";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)")
    public String jdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        var apiModel = (Map) contextModel.get(apiProperty);
        var zdlModel = (Map) contextModel.getOrDefault(jdlProperty, Maps.of("isDummy", true));
        contextModel.put(jdlProperty, zdlModel);

        // assign entity-service using tags from openapi or asyncapi
        Map<String, String> schemaTagMap = new HashMap<>();
        List<Map<String, Object>> operations = JSONPath.get(apiModel, "$.[*][*][*][?(@.operationId)]");
        for (Map<String, Object> operation : operations) {
            var tagName = (String) JSONPath.get(operation, "tags[0].name", JSONPath.get(operation, "tags[0]"));
            var schemaName = (String) JSONPath.get(operation, "$.x--response.x--response-dto");
            schemaTagMap.put(schemaName, normalizeTagName(tagName));
        }

        Map<String, Map> services = new HashMap<>();

        Map<String, Map<String, Object>> entities = new HashMap();
        zdlModel.put("entities", entities);
        Map<String, Map> schemas = JSONPath.get(apiModel, "$.components.schemas");
        for (Map.Entry<String, Map> schemaEntry : schemas.entrySet()) {
            var entity = new HashMap<String, Object>();
            var schemaName = schemaEntry.getKey();
            var businessName = (String) schemaEntry.getValue().get(jdlBusinessEntityProperty);
            var paginatedName = (String) schemaEntry.getValue().get(jdlBusinessEntityPaginatedProperty);
            var entityName = ObjectUtils.firstNonNull(businessName, paginatedName, schemaName);
            entity.put("name", entityName);
            entity.put("type", "entities");
            entity.put("description", schemaEntry.getValue().get("description"));
            entity.put("className", className(entityName));
            entity.put("instanceName", instanceName(entityName));
            entity.put("classNamePlural", classNamePlural(entityName));
            entity.put("instanceNamePlural", instanceNamePlural(entityName));

            var tagName = schemaTagMap.get(entityName);
            var serviceName = className(tagName + "Service");
            addEntityToServices(services, serviceName, entityName);

            entities.put(entityName, entity);
        }

        zdlModel.put("services", services);
        zdlModel.put("allEntitiesAndEnums", entities);

//        zdlModel.put("options", Maps.of("options", Maps.of("service", services)));

        return contextModel;
    }

    protected void addEntityToServices(Map<String, Map> services, String serviceName, String entityName) {
        Map service = services.getOrDefault(serviceName, new HashMap());
        services.put(serviceName, service);
        service.put("name", serviceName);
        service.put("aggregates", service.getOrDefault("entityNames", new ArrayList<>()));
        ((List) service.get("aggregates")).add(entityName);
    }

    protected String className(String name) {
        return NamingUtils.asJavaTypeName(name);
    }

    protected String instanceName(String name) {
        return NamingUtils.asInstanceName(name);
    }

    protected String classNamePlural(String name) {
        return NamingUtils.plural(className(name));
    }

    protected String instanceNamePlural(String name) {
        return NamingUtils.plural(instanceName(name));
    }
}
