package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLGenerator;
import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.zdl.ZDLFindUtils;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;

import static java.util.Collections.emptyList;

public class ZDLToOpenAPIGenerator implements Generator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "zdl";

    @DocumentedOption(description = "API Title")
    public String title;

    @DocumentedOption(description = "DTO Suffix used for schemas in PATCH operations")
    public String dtoPatchSuffix = "Patch";

    @DocumentedOption(description = "Target file")
    public String targetFile = "openapi.yml";
    @DocumentedOption(description = "Extension property referencing original zdl entity in components schemas (default: x-business-entity)")
    public String zdlBusinessEntityProperty = "x-business-entity";

    @DocumentedOption(description = "Extension property referencing original zdl entity in components schemas for paginated lists")
    public String zdlBusinessEntityPaginatedProperty = "x-business-entity-paginated";

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. Examples: '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.items", "$.properties.content.items");

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;

    protected Map<String, Integer> httpStatusCodes = Map.of(
        "get", 200,
        "post", 201,
        "put", 200,
        "patch", 200,
        "delete", 204
    );

    public ZDLToOpenAPIGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private final TemplateInput zdlToOpenAPITemplate = new TemplateInput("io/zenwave360/sdk/plugins/ZDLToOpenAPIGenerator/ZDLToOpenAPI.yml", "{{targetFile}}").withMimeType(OutputFormatType.YAML);

    protected Map<String, Object> getZDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("serviceAggregates", (context, options) -> {
            Map service = options.hash("service", new HashMap<>());
            Map zdl = options.hash("zdl", new HashMap<>());
            List<String> aggregateNames = JSONPath.get(service, "$.aggregates", List.of());
            String aggregatesRegex = aggregateNames.isEmpty() ? "" : " =~ /(" + StringUtils.join(aggregateNames, "|") + ")/";
            return JSONPath.<List<Map<String, Object>>>get(zdl, "$.channels[*][*][?(@.operationId" + aggregatesRegex + ")]");
        });
        handlebarsEngine.getHandlebars().registerHelper("httpResponseStatus", (context, options) -> {
            Map operation = (Map) context;
            var defaultStatus = httpStatusCodes.get(operation.get("httpMethod"));
            return JSONPath.get(operation, "$.httpOptions.status", defaultStatus);
        });
        handlebarsEngine.getHandlebars().registerHelper("responseBodyCollectionSuffix", (context, options) -> {
            Map operation = (Map) context;
            var isArray = JSONPath.get(operation, "$.isResponseBodyArray", false);
            var paginated = JSONPath.get(operation, "$.paginated", false);
            if(isArray) {
                return paginated ? "Paginated" : "List";
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
        Map<String, Object> zdlModel = getZDLModel(contextModel);
        List<String> serviceNames = JSONPath.get(zdlModel, "$.options.options.service[*].value");
        ((Map) zdlModel).put("serviceNames", serviceNames);

        var paginatedEntities = ZDLFindUtils.findAllPaginatedEntities(zdlModel);
        var listedEntities = ZDLFindUtils.findAllEntitiesReturnedAsList(zdlModel);
        zdlModel.put("paginatedEntities", paginatedEntities);
        zdlModel.put("listedEntities", listedEntities);

        Map<String, Object> oasSchemas = new HashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        JSONPath.set(oasSchemas, "components.schemas", schemas);

        EntitiesToSchemasConverter converter = new EntitiesToSchemasConverter().withIdType(idType, idTypeFormat).withZdlBusinessEntityProperty(zdlBusinessEntityProperty);

        var methodsWithRest = JSONPath.get(zdlModel, "$.services[*].methods[*][?(@.options.get || @.options.post || @.options.put || @.options.delete || @.options.patch)]", Collections.<Map>emptyList());
        List<Map<String, Object>> entities = filterSchemasToInclude(zdlModel, methodsWithRest);
        for (Map<String, Object> entity : entities) {
            String entityName = (String) entity.get("name");
            Map<String, Object> openAPISchema = converter.convertToSchema(entity, zdlModel);
            schemas.put(entityName, openAPISchema);

            if(listedEntities.contains(entityName)) {
                Map<String, Object> listSchema = Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName));
                schemas.put(entityName + "List", listSchema);
            }

            if(paginatedEntities.contains(entityName)) {
                Map<String, Object> paginatedSchema = new HashMap<>();
                paginatedSchema.put("allOf", List.of(
                        Map.of("$ref", "#/components/schemas/Page"),
                        Map.of(zdlBusinessEntityPaginatedProperty, entityName),
                        Map.of("properties",
                                Map.of("content",
                                        Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName))))));
                schemas.put(entityName + "Paginated", paginatedSchema);
            }
        }

        var methodsWithPatch = JSONPath.get(zdlModel, "$.services[*].methods[*][?(@.options.patch)]", Collections.<Map>emptyList());
        List<String> entitiesForPatch = methodsWithPatch.stream().map(method -> (String) method.get("parameter")).collect(Collectors.toList());
        for (String entityName : entitiesForPatch) {
            if (entityName != null) {
                schemas.put(entityName + dtoPatchSuffix, Map.of("allOf", List.of(Map.of("$ref", "#/components/schemas/" + entityName))));
            }
        }

        String openAPISchemasString = null;
        try {
            openAPISchemasString = mapper.writeValueAsString(oasSchemas);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // remove first line
        openAPISchemasString = openAPISchemasString.substring(openAPISchemasString.indexOf("\n") + 1);

        return List.of(generateTemplateOutput(contextModel, zdlToOpenAPITemplate, zdlModel, openAPISchemasString));
    }

    protected List<Map<String, Object>> filterSchemasToInclude(Map<String, Object> model, List<Map> methodsWithCommands) {
        Map<String, Object> allEntitiesAndEnums = (Map) model.get("allEntitiesAndEnums");
        Map<String, Object> relationships = (Map) model.get("relationships");

        List<Map<String, Object>> schemasToInclude = new ArrayList<>();
        JSONPath.get(methodsWithCommands, "$.[*].parameter", List.of()).forEach(parameter -> {
            var entity = JSONPath.get(allEntitiesAndEnums, "$.['" + parameter + "']", null);
            if (entity != null) {
                if (JSONPath.get(entity, "$.options.inline", false)) {
                    var fields = JSONPath.get(entity, "$.fields[*].type", List.<String>of());
                    for (String type : fields) {
                        var inlineEntity = JSONPath.get(allEntitiesAndEnums, "$.['" + type + "']", null);
                        if(inlineEntity != null) {
                            schemasToInclude.add((Map) inlineEntity);
                        }
                    }
                } else {
                    schemasToInclude.add((Map) entity);
                }
            }
        });
        JSONPath.get(methodsWithCommands, "$.[*].returnType", List.of()).forEach(parameter -> {
            var entity = JSONPath.get(allEntitiesAndEnums, "$.['" + parameter + "']", null);
            if(entity != null) {
                schemasToInclude.add((Map) entity);
            }
        });

        Set<String> includeNames = new HashSet<>();
        for (Map<String, Object> schema : schemasToInclude) {
            includeNames.add((String) schema.get("name"));
            addReferencedTypeToIncludeNames(schema, allEntitiesAndEnums, includeNames);
        }

        for (String includeName : new ArrayList<>(includeNames)) {
            Map<String, Object> entity = (Map) allEntitiesAndEnums.get(includeName);
            if (entity != null) {
                addRelationshipTypeToIncludeNames(entity, allEntitiesAndEnums, relationships, includeNames);
            }
        }

        List<Map<String, Object>> schemasToIncludeList = new ArrayList<>(schemasToInclude);
        for (String includeName : includeNames) {
            Map<String, Object> entity = (Map) allEntitiesAndEnums.get(includeName);
            if (entity != null) {
                schemasToIncludeList.add(entity);
            }
        }

        return schemasToIncludeList;
    }

    private void addRelationshipTypeToIncludeNames(Map<String, Object> entity, Map<String, Object> entitiesMap, Map<String, Object> relationships, Set<String> includeNames) {
        var entityName = entity.get("name");
        var relatedTypes = new HashSet<String>(JSONPath.get(relationships, "$..[?(@.from == '" + entityName + "')].to", List.of()));
        for (String fieldType : relatedTypes) {
            if (entitiesMap.containsKey(fieldType) && !includeNames.contains(fieldType)) {
                includeNames.add(fieldType);
                addReferencedTypeToIncludeNames((Map) entitiesMap.get(fieldType), entitiesMap, includeNames);
            }
        }
    }

    protected void addReferencedTypeToIncludeNames(Map<String, Object> entity, Map<String, Object> entitiesMap, Set<String> includeNames) {
        var fieldTypes = new HashSet<String>(JSONPath.get(entity, "$.fields[*].type", List.of()));
        for (String fieldType : fieldTypes) {
            if (entitiesMap.containsKey(fieldType) && !includeNames.contains(fieldType)) {
                includeNames.add(fieldType);
                addReferencedTypeToIncludeNames((Map) entitiesMap.get(fieldType), entitiesMap, includeNames);
            }
        }
    }


    public TemplateOutput generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> zdlModel, String schemasAsString) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("zdlModel", zdlModel);
        model.put("schemasAsString", schemasAsString);
        return handlebarsEngine.processTemplate(model, template).get(0);
    }
}
