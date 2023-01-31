package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.utils.JSONPath;

import java.util.List;
import java.util.Map;

/**
 * Depends on {@link OpenApiProcessor} to run before.
 */
public class EnrichOpenAPIWithJDLProcessor extends EnrichSchemaWithJDLProcessor {

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.properties.items", "$.properties.content.items");

    @Override
    protected void enrichSchemaWithJdl(Map<String, Object> schemaModel, Map<String, Object> jdlModel) {
        super.enrichSchemaWithJdl(schemaModel, jdlModel);

        enrichPaginatedSchemasWithEntity(schemaModel, jdlModel);
        enrichOpenapiRequestAndResponseWithEntity(schemaModel);
    }

    protected void enrichPaginatedSchemasWithEntity(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
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

    protected Map<String, Object> getPaginatedDtoSchema(Map<String, Object> schemaOrResponse) {
        var schema = JSONPath.get(schemaOrResponse, "x--response-schema", schemaOrResponse);
        return paginatedDtoItemsJsonPath.stream()
                .map(jsonPath -> (Map) JSONPath.get(schema, jsonPath))
                .filter(paginatedDtoSchema -> paginatedDtoSchema != null)
                .findFirst().orElse(null);
    }

}
