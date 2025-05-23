package io.zenwave360.sdk.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.EntitiesToSchemasConverter;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.processors.YamlOverlyMerger;
import io.zenwave360.sdk.utils.AntStyleMatcher;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.StringUtils;

public class ZDLToOpenAPIGenerator implements Generator {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String sourceProperty = "zdl";

    @DocumentedOption(description = "API Title")
    public String title;

    @DocumentedOption(description = "DTO Suffix used for schemas in PATCH operations")
    public String dtoPatchSuffix = "Patch";

    @DocumentedOption(description = "Target file")
    public String targetFile = "openapi.yml";

    @DocumentedOption(description = "Overlay Spec file to apply on top of generated OpenAPI file")
    public List<String> openapiOverlayFiles;

    @DocumentedOption(description = "OpenAPI file to be merged on top of generated OpenAPI file")
    public String openapiMergeFile;

    @DocumentedOption(description = "JsonSchema type for id fields and parameters.")
    public String idType = "string";

    @DocumentedOption(description = "JsonSchema type format for id fields and parameters.")
    public String idTypeFormat = null;


    @DocumentedOption(description = "Operation IDs to include. If empty, all operations will be included. (Supports Ant-style wildcards)")
    public List<String> operationIdsToInclude;

    @DocumentedOption(description = "Operation IDs to exclude. If not empty it will be applied to the processed operationIds to include. (Supports Ant-style wildcards)")
    public List<String> operationIdsToExclude;

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
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
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

        var paths = JSONPath.get(zdlModel, "$.services[*].paths", List.<Map>of());

        EntitiesToSchemasConverter converter = new EntitiesToSchemasConverter().withIdType(idType, idTypeFormat);

        var methodsWithRest = JSONPath.get(zdlModel, "$.services[*].methods[*][?(@.options.get || @.options.post || @.options.put || @.options.delete || @.options.patch)]", Collections.<Map>emptyList());
        methodsWithRest = filterOperationsToInclude(methodsWithRest);
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
                        Map.of("properties",
                                Map.of("content",
                                        Maps.of("type", "array", "items", Map.of("$ref", "#/components/schemas/" + entityName))))));
                schemas.put(entityName + "Paginated", paginatedSchema);
            }
        }

        var methodsWithPatch = JSONPath.get(zdlModel, "$.services[*].methods[*][?(@.options.patch)]", Collections.<Map>emptyList());
        methodsWithPatch = filterOperationsToInclude(methodsWithPatch);
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

        var template = generateTemplateOutput(contextModel, zdlToOpenAPITemplate, zdlModel, openAPISchemasString);
        var templateContent = YamlOverlyMerger.mergeAndOverlay(template.getContent(), openapiMergeFile, openapiOverlayFiles);
        template = new TemplateOutput(template.getTargetFile(), templateContent, template.getMimeType(), template.isSkipOverwrite());

        var generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(template);
        return generatedProjectFiles;
    }

    protected List<Map> filterOperationsToInclude(List<Map> methods) {
        List<Map> includedMethods = methods;
        if (operationIdsToInclude != null && !operationIdsToInclude.isEmpty()) {
            includedMethods = methods.stream()
                    .filter(method -> operationIdsToInclude.stream()
                            .anyMatch(include -> AntStyleMatcher.match(include, (String) method.get("name"))))
                    .toList();
        }
        if (operationIdsToExclude != null && !operationIdsToExclude.isEmpty()) {
            includedMethods = includedMethods.stream()
                    .filter(method -> operationIdsToExclude.stream()
                            .noneMatch(exclude -> AntStyleMatcher.match(exclude, (String) method.get("name"))))
                    .toList();
        }
        return includedMethods;
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
        return handlebarsEngine.processTemplate(model, template);
    }
}
