package io.zenwave360.sdk.processors;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.utils.JSONPath;

/**
 *
 */
public class EnrichSchemaWithJDLProcessor extends AbstractBaseProcessor {

    public String jdlProperty = "jdl";
    public String apiProperty = "api";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)")
    public String jdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @DocumentedOption(description = "Maps openapi dtos to jdl entity names")
    public Map<String, String> dtoToEntityNameMap = new HashMap<>();

    protected Map<String, Map<String, Object>> dtoToEntityMap = new HashMap<>();


    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        var schemaModel = (Map) contextModel.get(apiProperty);
        var jdlModel = (Map) contextModel.getOrDefault(jdlProperty, Collections.emptyMap());

        // This loop it's also in OpenAPIProcessor
        Map<String, Map> schemas = JSONPath.get(schemaModel, "$.components.schemas", Collections.emptyMap());
        for (Map.Entry<String, Map> entry : schemas.entrySet()) {
            entry.getValue().put("x--schema-name", entry.getKey());
        }

        buildDtoToEntityMap(schemaModel, jdlModel);
        enrichSchemaWithJdl(schemaModel, jdlModel);

        return contextModel;
    }

    protected void enrichSchemaWithJdl(Map<String, Object> schemaModel, Map<String, Object> jdlModel) {
        enrichSchemasWithEntity(schemaModel, jdlModel);
        enrichJdlEntitiesWithDtoNames(schemaModel, jdlModel);
    }

    protected void enrichSchemasWithEntity(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String dtoName = (String) schema.get("x--schema-name");
            String entityName = JSONPath.get(schema, jdlBusinessEntityProperty);
            schema.put("x--entity", dtoToEntityMap.get(dtoName));
        }
    }

    protected void enrichJdlEntitiesWithDtoNames(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String schemaName = (String) schema.get("x--schema-name");
            String entityName = dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.allEntitiesAndEnums." + entityName);
            if (entity != null) {
                var dtos = JSONPath.get(entity, "$.options.dtos", new ArrayList<Map>());
                var copiedSchema = new HashMap<>(schema);
                copiedSchema.remove("x--entity");
                dtos.add(copiedSchema);
                JSONPath.set(entity, "options.dtos", dtos);
            }
        }
    }

    protected void buildDtoToEntityMap(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String schemaName = (String) schema.get("x--schema-name");
            String entityName = dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.allEntitiesAndEnums." + entityName);
            if (entity != null) {
                dtoToEntityMap.put(schemaName, entity);
            }
        }
    }
}
