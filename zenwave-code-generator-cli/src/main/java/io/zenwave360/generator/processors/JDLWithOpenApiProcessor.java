package io.zenwave360.generator.processors;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.utils.JSONPath;

/**
 * Depends on {@link io.zenwave360.generator.processors.OpenApiProcessor} to run before.
 */
public class JDLWithOpenApiProcessor extends AbstractBaseProcessor {

    public static final String JDL_DEFAULT_PROPERTY = "jdl";
    public static final String OPENAPI_DEFAULT_PROPERTY = "openapi";

    public String jdlProperty = "jdl";
    public String openapiProperty = "openapi";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas for paginated lists (default: x-business-entity-paginated)")
    public String jdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.properties.items", "$.properties.content.items");

    @DocumentedOption(description = "Maps openapi dtos to jdl entity names")
    public Map<String, String> dtoToEntityNameMap = new HashMap<>();

    private Map<String, Map<String, Object>> dtoToEntityMap = new HashMap<>();

    public <T extends AbstractBaseProcessor> T withJdlProperty(String jdlProperty) {
        this.jdlProperty = jdlProperty;
        return (T) this;
    }

    public <T extends AbstractBaseProcessor> T withOpenapiProperty(String openapiProperty) {
        this.openapiProperty = openapiProperty;
        return (T) this;
    }

    public <T extends AbstractBaseProcessor> T withJdlBusinessEntityProperty(String jdlBusinessEntityProperty) {
        this.jdlBusinessEntityProperty = jdlBusinessEntityProperty;
        return (T) this;
    }

    public <T extends AbstractBaseProcessor> T withDtoToEntityName(String dto, String entityName) {
        this.dtoToEntityNameMap.put(dto, entityName);
        return (T) this;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        var openApiModel = (Map) contextModel.get(openapiProperty);
        var jdlModel = (Map) contextModel.getOrDefault(jdlProperty, Collections.emptyMap());

        buildDtoToEntityMap(openApiModel, jdlModel);

        enrichSchemasWithEntity(openApiModel, jdlModel);
        enrichOpenapiRequestAndResponseWithEntity(openApiModel);

        enrichJdlEntitiesWithDtoNames(openApiModel, jdlModel);

        List<Map<String, Object>> operations = JSONPath.get(openApiModel, "$.paths[*][*][?(@.operationId)]");
        return contextModel;
    }

    protected void enrichSchemasWithEntity(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String dtoName = (String) schema.get("x--schema-name");
            String entityName = JSONPath.get(schema, jdlBusinessEntityProperty);
            schema.put("x--entity", dtoToEntityMap.get(dtoName));

            var paginatedDtoSchema = getPaginatedDtoSchema(schema);
            if (paginatedDtoSchema != null) {
                String paginatedDtoName = (String) paginatedDtoSchema.get("x--schema-name");
                String paginatedEntityName = JSONPath.get(schema, jdlBusinessEntityPaginatedProperty);
                schema.put("x--entity-paginated", dtoToEntityMap.get(paginatedDtoName));
            }
        }
    }

    protected void enrichOpenapiRequestAndResponseWithEntity(Map<String, Object> openApiModel) {
        // TODO consider using schemas x--entity and x--entity-paginated to inform this
        List<Map<String, Object>> requests = JSONPath.get(openApiModel, "$..[?(@.x--request-dto)]");
        for (Map<String, Object> request : requests) {
            String dtoName = JSONPath.get(request, "$.x--request-dto");
            request.put("x--request-entity", dtoToEntityMap.get(dtoName));
        }

        List<Map<String, Object>> responses = JSONPath.get(openApiModel, "$..[?(@.x--response-dto)]");
        for (Map<String, Object> response : responses) {
            String dtoName = JSONPath.get(response, "$.x--response-dto");
            response.put("x--response-entity", dtoToEntityMap.get(dtoName));

            var paginatedDtoSchema = getPaginatedDtoSchema(response);
            if (paginatedDtoSchema != null) {
                String paginatedDtoName = (String) paginatedDtoSchema.get("x--schema-name");
                response.put("x--response-entity-paginated", dtoToEntityMap.get(paginatedDtoName));
            }
        }
    }

    protected void enrichJdlEntitiesWithDtoNames(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String schemaName = (String) schema.get("x--schema-name");
            String entityName = dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.entities." + entityName);
            if (entity != null) {
                var dtos = JSONPath.get(entity, "$.options.dtos", new ArrayList<Map>());
                var copiedSchema = new HashMap<>(schema);
                copiedSchema.remove("x--entity");
                dtos.add(copiedSchema);
                JSONPath.set(entity, "options.dtos", dtos);
            }
        }
    }

    protected Map<String, Object> getPaginatedDtoSchema(Map<String, Object> schemaOrResponse) {
        var schema = JSONPath.get(schemaOrResponse, "x--response-schema", schemaOrResponse);
        return paginatedDtoItemsJsonPath.stream()
                .map(jsonPath -> (Map) JSONPath.get(schema, jsonPath))
                .filter(paginatedDtoSchema -> paginatedDtoSchema != null)
                .findFirst().orElse(null);
    }

    protected void buildDtoToEntityMap(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String schemaName = (String) schema.get("x--schema-name");
            String entityName = dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.entities." + entityName);
            if (entity != null) {
                dtoToEntityMap.put(schemaName, entity);
            }
        }
    }
}
