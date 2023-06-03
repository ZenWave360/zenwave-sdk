package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractJDLGenerator;
import io.zenwave360.sdk.generators.JDLEntitiesToSchemasConverter;
import io.zenwave360.sdk.processors.ZDLUtils;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;

import static java.util.Collections.emptyList;

public class JDLToOpenAPIGenerator extends AbstractJDLGenerator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> annotationsToGenerate = List.of("aggregate");
    @DocumentedOption(description = "Skip generating operations for entities annotated with these")
    public List<String> skipForAnnotations = List.of("vo", "embedded", "skip");

    @DocumentedOption(description = "Target file")
    public String targetFile = "openapi.yml";
    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas (default: x-business-entity)")
    public String jdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original jdl entity in components schemas for paginated lists")
    public String jdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. Examples: '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.items", "$.properties.content.items");

    @DocumentedOption(description = "Suffix for search criteria DTOs (default: Criteria)")
    public String criteriaDTOSuffix = "Criteria";

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    protected Map<String, Integer> httpStatusCodes = Map.of(
        "get", 200,
        "post", 201,
        "put", 200,
        "delete", 204
    );

    public JDLToOpenAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput jdlToOpenAPITemplate = new TemplateInput("io/zenwave360/sdk/plugins/OpenAPIToJDLGenerator/JDLToOpenAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getJDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    protected boolean isGenerateSchemaEntity(Map<String, Object> entity) {
        String entityName = (String) entity.get("name");
        return entities.isEmpty() || entities.contains(entityName);
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("skipOperations", (context, options) -> {
            Map entity = (Map) context;
            String annotationsFilter = annotationsToGenerate.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
            String skipAnnotationsFilter = skipForAnnotations.stream().map(a -> "@." + a).collect(Collectors.joining(" || "));
            boolean isGenerate = annotationsToGenerate.isEmpty() || !((List) JSONPath.get(entity, "$.options[?(" + annotationsFilter + ")]")).isEmpty();
            boolean isSkip = skipForAnnotations.isEmpty() || !((List) JSONPath.get(entity, "$.options[?(" + skipAnnotationsFilter + ")]")).isEmpty();
            return !isGenerate || isSkip;
        });
        handlebarsEngine.getHandlebars().registerHelper("serviceAggregates", (context, options) -> {
            Map service = options.hash("service", new HashMap<>());
            Map jdl = options.hash("jdl", new HashMap<>());
            List<String> aggregateNames = JSONPath.get(service, "$.aggregates", List.of());
            String aggregatesRegex = aggregateNames.isEmpty() ? "" : " =~ /(" + StringUtils.join(aggregateNames, "|") + ")/";
            return JSONPath.<List<Map<String, Object>>>get(jdl, "$.channels[*][*][?(@.operationId" + aggregatesRegex + ")]");
        });
        handlebarsEngine.getHandlebars().registerHelper("httpResponseStatus", (context, options) -> {
            Map operation = (Map) context;
            var defaultStatus = httpStatusCodes.get(operation.get("httpMethod"));
            return JSONPath.get(operation, "$.httpOptions.status", defaultStatus);
        });
        handlebarsEngine.getHandlebars().registerHelper("responseBodyCollectionSuffix", (context, options) -> {
            Map operation = (Map) context;
            var isArray = JSONPath.get(operation, "$.isResponseBodyArray", false);
            var pageable = JSONPath.get(operation, "$.pageable", false);
            if(isArray) {
                return pageable ? "Paginated" : "List";
            }
            return "";
        });
        handlebarsEngine.getHandlebars().registerHelper("asTagName", (context, options) -> {
            if (context instanceof String) {
                return ((String) context).replaceAll("(Service|UseCases)", "");
            }
            return "Default";
        });
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        Map<String, Object> jdlModel = getJDLModel(contextModel);
        List<String> serviceNames = JSONPath.get(jdlModel, "$.options.options.service[*].value");
        ((Map) jdlModel).put("serviceNames", serviceNames);

        if(this.entities == null) {
            this.entities = ZDLUtils.findAllServiceFacingEntities(jdlModel);
        }

        var paginatedEntities = ZDLUtils.findAllPaginatedEntities(jdlModel);
        var listedEntities = ZDLUtils.findAllEntitiesReturnedAsList(jdlModel);
        jdlModel.put("paginatedEntities", paginatedEntities);
        jdlModel.put("listedEntities", listedEntities);

        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        JDLEntitiesToSchemasConverter converter = new JDLEntitiesToSchemasConverter().withIdType(idType, idTypeFormat).withJdlBusinessEntityProperty(jdlBusinessEntityProperty);

        List<Map<String, Object>> entities = (List) JSONPath.get(jdlModel, "$.allEntitiesAndEnums[*]");
        for (Map<String, Object> entity : entities) {
            if (!isGenerateSchemaEntity(entity)) {
                continue;
            }
            String entityName = (String) entity.get("name");
            Map<String, Object> openAPISchema = converter.convertToSchema(entity, jdlModel);
            schemas.put(entityName, openAPISchema);

            if(listedEntities.contains(entityName)) {
                Map<String, Object> listSchema = Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName));
                schemas.put(entityName + "List", listSchema);
            }

            if(paginatedEntities.contains(entityName)) {
                Map<String, Object> paginatedSchema = new HashMap<>();
                paginatedSchema.put("allOf", List.of(
                        Map.of("$ref", "#/components/schemas/Page"),
                        Map.of(jdlBusinessEntityPaginatedProperty, entityName),
                        Map.of("properties",
                                Map.of("content",
                                        Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName))))));
                schemas.put(entityName + "Paginated", paginatedSchema);
            }
        }

        List<Map<String, Object>> enums = JSONPath.get(jdlModel, "$.enums.enums[*]", emptyList());
        for (Map<String, Object> enumValue : enums) {
            if (!isGenerateSchemaEntity(enumValue)) {
                continue;
            }
            Map<String, Object> enumSchema = converter.convertToSchema(enumValue, jdlModel);
            schemas.put((String) enumValue.get("name"), enumSchema);
        }

        String openAPISchemasString = null;
        try {
            openAPISchemasString = mapper.writeValueAsString(oasSchemas);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // remove first line
        openAPISchemasString = openAPISchemasString.substring(openAPISchemasString.indexOf("\n") + 1);

        return List.of(generateTemplateOutput(contextModel, jdlToOpenAPITemplate, jdlModel, openAPISchemasString));
    }

    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> jdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdlModel", jdlModel);
        model.put("schemasAsString", schemasAsString);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }
}
