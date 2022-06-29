package io.zenwave360.generator.processors;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import org.apache.avro.data.Json;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, ?> process(Map<String, ?> contextModel) {
        var openApiModel = (Map) contextModel.get(openapiProperty);
        var jdlModel = (Map) contextModel.get(jdlProperty);

        buildDtoToEntityMap(openApiModel, jdlModel);
        enrichOpenapiRequestAndResponseWithEntity(openApiModel);
        enrichSchemasWithEntity(openApiModel, jdlModel);

        enrichJdlEntitiesWithDtoNames(openApiModel, jdlModel);

        List<Map<String, Object>> operations = JSONPath.get(openApiModel, "$.paths[*][*][?(@.operationId)]");
        return contextModel;
    }

    protected void enrichOpenapiRequestAndResponseWithEntity(Map<String, Object> openApiModel) {
        List<Map<String, Object>> requests = JSONPath.get(openApiModel, "$..[?(@.x--request-dto)]");
        for (Map<String, Object> request : requests) {
            String dtoName = JSONPath.get(request, "$.x--request-dto");
            request.put("x--request-entity", dtoToEntityMap.get(dtoName));
        }

        List<Map<String, Object>> responses = JSONPath.get(openApiModel, "$..[?(@.x--response-dto)]");
        for (Map<String, Object> response : responses) {
            String dtoName = JSONPath.get(response, "$.x--response-dto");
            response.put("x--response-entity", dtoToEntityMap.get(dtoName));
        }
    }

    protected void enrichSchemasWithEntity(Map<String, Object> openApiModel, Map<String, Object> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$..[?(@." + jdlBusinessEntityProperty + ")]");
        for (Map<String, Object> schema : schemas) {
            String entityName = JSONPath.get(schema, jdlBusinessEntityProperty);
            schema.put("x--entity", JSONPath.get(jdlModel, "$.entities." + entityName));
        }
    }

    protected void enrichJdlEntitiesWithDtoNames(Map<String, ?> openApiModel, Map<String, ?> jdlModel) {
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            if(isArrayOrPaginationSchema(schema)) {
                continue;
            }
            String schemaName = (String) schema.get("x--schema-name");
            String entityName =  dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.entities." + entityName);
            if(entity != null) {
                var dtos = JSONPath.get(entity, "$.options.dtos", new ArrayList<Map>());
                var copiedSchema = new HashMap<>(schema);
                copiedSchema.remove("x--entity");
                dtos.add(copiedSchema);
                JSONPath.set(entity, "options.dtos", dtos);
            }
        }
    }

    protected void buildDtoToEntityMap(Map<String, ?> openApiModel, Map<String, ?> jdlModel){
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            if(isArrayOrPaginationSchema(schema)) {
                continue;
            }
            String schemaName = (String) schema.get("x--schema-name");
            String entityName =  dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get(jdlBusinessEntityProperty));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.entities." + entityName);
            if(entity == null) {
                entity = Map.of("name", entityName, "className", entityName, "instanceName", entityName, "classNamePlural", entityName + "s", "instanceNamePlural", entityName + "s");
            }
            dtoToEntityMap.put(schemaName, entity);
        }
    }

    private boolean isArrayOrPaginationSchema(Map<String, Object> schema) {
        String schemaName = (String) schema.get("x--schema-name");
        return schemaName.endsWith("Paginated");
    }
}
