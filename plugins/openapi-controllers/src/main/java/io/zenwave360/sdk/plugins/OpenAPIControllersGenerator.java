package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.zdl.ZDLFindUtils;
import io.zenwave360.sdk.zdl.ZDLJavaSignatureUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractOpenAPIGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;

public class OpenAPIControllersGenerator extends AbstractOpenAPIGenerator {

    public String apiProperty = "api";
    public String zdlProperty = "zdl";

    @DocumentedOption(description = "The package to generate REST Controllers")
    public String controllersPackage = "{{basePackage}}.adapters.web";

    @DocumentedOption(description = "Package where your domain entities are")
    public String entitiesPackage = "{{basePackage}}.core.domain";

    @DocumentedOption(description = "Package where your inbound dtos are")
    public String inboundDtosPackage = "{{basePackage}}.core.inbound.dtos";

    @DocumentedOption(description = "Package where your domain services/usecases interfaces are")
    public String servicesPackage = "{{basePackage}}.core.inbound";

    @DocumentedOption(description = "Should use same value configured in BackendApplicationDefaultPlugin. Whether to use an input DTO for entities used as command parameter.")
    public String inputDTOSuffix = "";
    @DocumentedOption(description = "Suffix for search criteria DTOs (default: Criteria)")
    public String criteriaDTOSuffix = "Criteria";

    @DocumentedOption(description = "Suffix for elasticsearch document entities (default: Document)")
    public String searchDTOSuffix = "Document";

    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.properties.items", "$.properties.content.items");

    protected HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected String templatesFolder = "io/zenwave360/sdk/plugins/OpenAPIControllersGenerator/";

    List<Object[]> templates = List.of(
            new Object[] {"src/main/java", "web/mappers/BaseMapper.java", "mappers/BaseMapper.java", JAVA},
            new Object[] {"src/main/java", "web/mappers/ServiceDTOsMapper.java", "mappers/{{service.name}}DTOsMapper.java", JAVA},
            new Object[] {"src/main/java", "web/{{webFlavor}}/ServiceApiController.java", "{{service.name}}ApiController.java", JAVA},
            new Object[] {"src/test/java", "web/{{webFlavor}}/ServiceApiControllerTest.java", "{{service.name}}ApiControllerTest.java", JAVA});

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        Function<Map<String, Object>, Boolean> skip = templateNames.length > 4 ? (Function) templateNames[4] : null;
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{asPackageFolder controllersPackage}}/" + templateNames[2])
                .withMimeType((OutputFormatType) templateNames[3])
                .withSkip(skip);
    }

    protected Map<String, Object> getZDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(zdlProperty);
    }

    protected Map<String, Object> getOpenAPIModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(apiProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        var templateOutputList = new ArrayList<TemplateOutput>();
        var openApiModel = getOpenAPIModel(contextModel);
        var zdlModel = getZDLModel(contextModel);

        String operationIdsRegex = operationIds.isEmpty() ? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
        List<Map<String, Object>> operations = JSONPath.get(openApiModel, "$.paths[*][*][?(@.operationId" + operationIdsRegex + ")]");
        Map<String, List<Map<String, Object>>> operationsByService = groupOperationsByService(operations);

        for (Map.Entry<String, List<Map<String, Object>>> operationByServiceEntry : operationsByService.entrySet()) {
            Set<String> dtoNames = new HashSet();
            dtoNames.addAll(JSONPath.get(operationByServiceEntry.getValue(), "$..x--request-dto"));
            dtoNames.addAll(JSONPath.get(operationByServiceEntry.getValue(), "$..x--response.x--response-dto"));

            Collection<String> entitiesServices = operationByServiceEntry.getValue().stream()
                    .map(operation -> ZDLFindUtils.findServiceMethod((String) operation.get("operationId"), zdlModel)).filter(Objects::nonNull)
                    .map(method -> (String) method.get("serviceName"))
                    .collect(Collectors.toSet());

            Map<String, Map<String, String>> requestDtosWithEntities = new HashMap<>();
            Map<String, Map<String, String>> responseDtosWithEntities = new HashMap<>();
            Map<String, Map<String, String>> responsePaginatedDtosWithEntities = new HashMap<>();
            for (Map operation : operationByServiceEntry.getValue()) {
                String requestDto = JSONPath.get(operation, "$.x--request-dto");
                String responseDto = JSONPath.get(operation, "$.x--response.x--response-dto");
                Map responseSchema = JSONPath.get(operation, "$.x--response.x--response-schema");
                var method = ZDLFindUtils.findServiceMethod((String) operation.get("operationId"), zdlModel);
                if (method == null) {
                    continue;
                }
                String inputType = JSONPath.get(method, "$.parameter");
                String outputType = JSONPath.get(method, "$.returnType");
                Map inputEntity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + inputType);
                Map outputEntity = JSONPath.get(zdlModel, "$.allEntitiesAndEnums." + outputType);
                if(inputEntity != null) {
                    Maps.getOrCreateDefault(requestDtosWithEntities, requestDto, new HashMap<>()).put(inputType, (String) inputEntity.get("className"));
                }
                if (outputEntity != null) {
                    if (JSONPath.get(method, "$.returnTypeIsArray", false)) {
                        String paginatedDto = findPaginatedDto(responseSchema);
                        Maps.getOrCreateDefault(responseDtosWithEntities, responseDto, new HashMap<>()).put(outputType, String.format("Page<%s>", outputEntity.get("className")));
                        Maps.getOrCreateDefault(responsePaginatedDtosWithEntities, responseDto, new HashMap<>()).put(outputType, (String) outputEntity.get("className"));
                    } else {
                        Maps.getOrCreateDefault(responseDtosWithEntities, responseDto, new HashMap<>()).put(outputType, (String) outputEntity.get("className"));
                    }
                }
            }

            Map serviceModel = Map.of(
                    "service", Map.of(
                            "name", operationByServiceEntry.getKey(),
                            "operations", operationByServiceEntry.getValue()),
                    "requestDtosWithEntities", requestDtosWithEntities,
                    "responseDtosWithEntities", responseDtosWithEntities,
                    "responsePaginatedDtosWithEntities", responsePaginatedDtosWithEntities,
                    "entitiesServices", entitiesServices);

            for (Object[] template : templates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(template), serviceModel));
            }
        }

        return templateOutputList;
    }

    String findPaginatedDto(Map<String, Object> responseSchema) {
        for (String jsonPath : paginatedDtoItemsJsonPath) {
            String paginatedDto = JSONPath.get(responseSchema, jsonPath + ".x--schema-name");
            if (paginatedDto != null) {
                return paginatedDto;
            }
        }
        return null;
    }

    protected Map<String, List<Map<String, Object>>> groupOperationsByService(List<Map<String, Object>> operations) {
        Map<String, List<Map<String, Object>>> operationsByService = new HashMap<>();
        for (Map<String, Object> operation : operations) {
            String tagName = (String) ObjectUtils.firstNonNull(operation.get("x--normalizedTagName"), "Default");
            if (!operationsByService.containsKey(tagName)) {
                operationsByService.put(tagName, new ArrayList<>());
            }
            String responseDto = JSONPath.get(operation, "$.x--response.x--response-dto");
            // operation.put("jdl-entity", dtoToEntityMap.get(responseDto));
            operationsByService.get(tagName).add(operation);
        }
        return operationsByService;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getOpenAPIModel(contextModel));
        model.put("zdl", getZDLModel(contextModel));
        model.put("webFlavor", style == ProgrammingStyle.imperative ? "mvc" : "webflux");
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("methodReturnType", (method, options) -> {
            if (method == null) { // legacy
                var operation = (Map) options.get("operation");
                var responseEntity = JSONPath.get(operation, "x--response.x--response-entity.className");
                var responseEntityPaginated = JSONPath.get(operation, "x--response.x--response-entity-paginated.className");
                return responseEntity;
            }
            return ZDLJavaSignatureUtils.methodReturnType((Map) method);
        });
        handlebarsEngine.getHandlebars().registerHelper("serviceMethodCall", (serviceMethod, options) -> {
            if (serviceMethod == null) { // legacy
                var operation = (Map) options.get("operation");
                var operationId = JSONPath.get(operation, "operationId");
                return String.format("%s(%s)", operationId, "input");
            }
            var methodName = JSONPath.get(serviceMethod, "name");
            var params = new ArrayList<String>();
            if(JSONPath.get(serviceMethod, "paramId") != null) {
                params.add("id"); // TODO
            }
            if(JSONPath.get(serviceMethod, "parameter") != null) {
                params.add("input");
            }
            return String.format("%s(%s)", methodName, StringUtils.join(params, ", "));
        });
        handlebarsEngine.getHandlebars().registerHelper("findServiceMethod", (operation, options) -> {
            var zdl = options.get("zdl");
            return ZDLFindUtils.findServiceMethod(JSONPath.get(operation, "operationId"), (Map) zdl);
        });
        handlebarsEngine.getHandlebars().registerHelper("hasPaginatedResponse", (operation, options) -> {
            return JSONPath.get(operation, "x--response.x--response-entity-paginated") != null;
        });
        handlebarsEngine.getHandlebars().registerHelper("hasResponseEntity", (operation, options) -> {
            return JSONPath.get(operation, "x--response.x--response-dto") != null;
        });
        handlebarsEngine.getHandlebars().registerHelper("requestEntity", (operation, options) -> {
            return JSONPath.get(operation, "x--request-entity");
        });
        handlebarsEngine.getHandlebars().registerHelper("responseEntity", (operation, options) -> {
            return JSONPath.get(operation, "x--response.x--response-entity");
        });
        handlebarsEngine.getHandlebars().registerHelper("responseEntityPaginated", (operation, options) -> {
            return JSONPath.get(operation, "x--response.x--response-entity-paginated");
        });

        handlebarsEngine.getHandlebars().registerHelper("orVoid", (context, options) -> {
            return StringUtils.isNotBlank((String) context) ? context : "Void";
        });

        handlebarsEngine.getHandlebars().registerHelper("asDtoName", (context, options) -> {
            return StringUtils.isNotBlank((String) context) ? openApiModelNamePrefix + context + openApiModelNameSuffix : null;
        });

        handlebarsEngine.getHandlebars().registerHelper("entityService", (entityName, options) -> {
            return ZDLFindUtils.findServiceName((String) entityName, options.get("zdl"));
        });

        handlebarsEngine.getHandlebars().registerHelper("statusCode", (context, options) -> {
            return "default".equals(context) ? "200" : context;
        });

        handlebarsEngine.getHandlebars().registerHelper("criteriaClassName", (context, options) -> {
            Map entity = (Map) context;
            Object criteria = JSONPath.get(entity, "$.options.searchCriteria");
            if (criteria instanceof String) {
                return criteria;
            }
            if (criteria == Boolean.TRUE) {
                return String.format("%s%s", entity.get("className"), criteriaDTOSuffix);
            }
            return "Pageable";
        });

        handlebarsEngine.getHandlebars().registerHelper("asMethodParameters", (context, options) -> {
            if (context instanceof Map) {
                Map operation = (Map) context;
                List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
                List methodParams = params.stream()
                        .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                        .map(param -> {
                            String javaType = getJavaTypeOrOptional(param);
                            String name = JSONPath.get(param, "$.name");
                            return javaType + " " + name;
                        }).collect(Collectors.toList());
                if (operation.containsKey("x--request-dto")) {
                    methodParams.add(String.format("%s%s%s %s", openApiModelNamePrefix, operation.get("x--request-dto"), openApiModelNameSuffix, "reqBody"));
                }
                return StringUtils.join(methodParams, ", ");
            }
            return options.fn(context);
        });

        handlebarsEngine.getHandlebars().registerHelper("asMethodParametersInitializer", (context, options) -> {
            if (context instanceof Map) {
                Map operation = (Map) context;
                List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
                List methodParams = params.stream()
                        .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                        .map(param -> {
                            String javaType = getJavaTypeOrOptional(param);
                            String name = JSONPath.get(param, "$.name");
                            return javaType + " " + name + " = null;";
                        }).collect(Collectors.toList());
                if (operation.containsKey("x--request-dto")) {
                    methodParams.add(String.format("%s%s%s %s = null;", openApiModelNamePrefix, operation.get("x--request-dto"), openApiModelNameSuffix, "reqBody"));
                }
                return StringUtils.join(methodParams, "\n");
            }
            return options.fn(context);
        });

        handlebarsEngine.getHandlebars().registerHelper("asMethodParameterValues", (context, options) -> {
            if (context instanceof Map) {
                Map operation = (Map) context;
                List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
                List methodParams = params.stream()
                        .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                        .map(param -> {
                            String name = JSONPath.get(param, "$.name");
                            return name;
                        }).collect(Collectors.toList());
                if (operation.containsKey("x--request-dto")) {
                    methodParams.add("reqBody");
                }
                return StringUtils.join(methodParams, ", ");
            }
            return options.fn(context);
        });

    }

    protected int compareParamsByRequire(Map<String, Object> param1, Map<String, Object> param2) {
        boolean required1 = JSONPath.get(param1, "required", false);
        boolean required2 = JSONPath.get(param2, "required", false);
        return (required1 && required2) || (!required1 && !required2) ? 0 : required1 ? -1 : 1;
    }

    protected String getJavaTypeOrOptional(Map<String, Object> param) {
        boolean isOptional = isOptional(param);
        String javaType = getJavaType(param);
        return isOptional ? "Optional<" + javaType + ">" : javaType;
    }

    protected String getJavaType(Map<String, Object> param) {
        String type = JSONPath.get(param, "$.schema.type");
        String format = JSONPath.get(param, "$.schema.format");
        if ("date".equals(format)) {
            return "LocalDate";
        }
        if ("date-time".equals(format)) {
            return "Instant";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "Long";
        }
        if ("integer".equals(type)) {
            return "Integer";
        }
        if ("number".equals(type)) {
            return "BigDecimal";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if ("array".equals(type)) {
            return "List<String>";
        }

        return "String";
    }

    protected boolean isOptional(Map param) {
        return "query".equals(JSONPath.get(param, "in")) && !JSONPath.get(param, "required", false);
    }
}
