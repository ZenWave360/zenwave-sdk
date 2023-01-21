package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.rmi.Naming;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.ZDLFindUtils;
import io.zenwave360.sdk.zdl.ZDLHttpUtils;
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

    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.items", "$.properties.content.items");

    protected HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected String templatesFolder = "io/zenwave360/sdk/plugins/OpenAPIControllersGenerator/";

    List<Object[]> templates = List.of(
            new Object[] {"src/main/java", "web/mappers/BaseMapper.java", "mappers/BaseMapper.java", JAVA},
            new Object[] {"src/main/java", "web/mappers/ServiceDTOsMapper.java", "mappers/{{serviceName}}DTOsMapper.java", JAVA},
            new Object[] {"src/main/java", "web/{{webFlavor}}/ServiceApiController.java", "{{serviceName}}ApiController.java", JAVA},
            new Object[] {"src/test/java", "web/{{webFlavor}}/ServiceApiControllerTest.java", "{{serviceName}}ApiControllerTest.java", JAVA});

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
            Collection<String> entitiesServices = operationByServiceEntry.getValue().stream()
                    .map(operation -> ZDLFindUtils.findServiceMethod((String) operation.get("operationId"), zdlModel)).filter(Objects::nonNull)
                    .map(method -> (String) method.get("serviceName"))
                    .collect(Collectors.toSet());


            List<Map<String, Object>> serviceOperations = new ArrayList<>();
            Map<String, Map<String, String>> mapperRequestDtoEntity = new HashMap<>();
            Map<String, Map<String, Object>> mapperResponseDtoEntity = new HashMap<>();

            for (Map operation : operationByServiceEntry.getValue()) {
                var method = ZDLFindUtils.findServiceMethod((String) operation.get("operationId"), zdlModel);

                String requestDto = JSONPath.get(operation, "$.x--request-dto");
                String requestDtoName = requestDto != null ? openApiModelNamePrefix + requestDto + openApiModelNameSuffix : null;
                String inputType = method != null? ZDLHttpUtils.getRequestBodyType(method, zdlModel) : "Entity";
                String responseSchemaName = JSONPath.get(operation, "$.x--response.x--response-dto");
                String responseEntityName = responseSchemaName != null ? openApiModelNamePrefix + responseSchemaName + openApiModelNameSuffix : null;
                String responseDtoName = responseSchemaName != null ? openApiModelNamePrefix + responseSchemaName + openApiModelNameSuffix : null;
                String outputType = JSONPath.get(method, "$.returnType");
                boolean isResponseArray = method != null? JSONPath.get(method, "$.returnTypeIsArray", false) : false;
                boolean isResponsePaginated = method != null? JSONPath.get(method, "$.options.paginated", false) : false;
                if (isResponseArray) {
                    var innerArrayDTO = innerArrayDTO(JSONPath.get(operation, "$.x--response.x--response-schema"));
                    responseDtoName = innerArrayDTO != null ? openApiModelNamePrefix + innerArrayDTO + openApiModelNameSuffix : responseDtoName;
                }

                serviceOperations.add(Maps.of(
                        "operationId", operation.get("operationId"),
                        "operation", operation,
                        "serviceMethod", method,
                        "statusCode", statusCode(operation),
                        "requestBodySchema", requestDto,
                        "requestDtoName", requestDtoName,
                        "methodParameters", methodParameters(operation),
                        "reqBodyVariableName", reqBodyVariableName(method, zdlModel),
                        "serviceMethodParameter", serviceMethodParameter(method, zdlModel),
                        "serviceMethodCall", serviceMethodCall(method, operation, zdlModel),
                        "mappedInputVariable", mappedInputVariable(method),
                        "inputType", inputType,
                        "responseSchemaName", responseSchemaName,
                        "responseDtoName", responseDtoName,
                        "responseEntityName", responseEntityName,
                        "responseEntityExpression", responseEntityExpression(responseEntityName, responseDtoName, isResponseArray, isResponsePaginated),
                        "methodReturnType", outputType,
                        "methodReturnTypeInstance", NamingUtils.asInstanceName(outputType),
                        "returnTypeIsOptional", JSONPath.get(method, "$.returnTypeIsOptional", false),
                        "isResponseArray", isResponseArray,
                        "isResponsePaginated", isResponsePaginated
                ));

                if(requestDto != null && inputType != null) {
                    var requestKey = format("%s_%s", requestDtoName, inputType);
                    Maps.getOrCreateDefault(mapperRequestDtoEntity, requestKey, new HashMap<>())
                            .putAll(Map.of("requestDto", requestDtoName, "inputType", inputType));
                }
                if(responseSchemaName != null && outputType != null) {
                    var responseKey = format("%s_%s_%s_%s", responseDtoName, outputType, isResponseArray, isResponsePaginated);
                    Maps.getOrCreateDefault(mapperResponseDtoEntity, responseKey, new HashMap<>())
                            .putAll(Map.of("responseDto", responseDtoName, "responseEntityName", responseEntityName, "outputType", outputType,
                                    "isResponseArray", isResponseArray, "isResponsePaginated", isResponsePaginated));
                }
            }

            Map serviceModel = Map.of(
                    "serviceName", operationByServiceEntry.getKey(),
                    "serviceOperations", serviceOperations,
                    "mapperRequestDtoEntity", mapperRequestDtoEntity,
                    "mapperResponseDtoEntity", mapperResponseDtoEntity,
                    "entitiesServices", entitiesServices);

            for (Object[] template : templates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(template), serviceModel));
            }
        }

        return templateOutputList;
    }

    private static String responseEntityExpression(String responseEntityName, String responseDtoName, boolean isResponseArray, boolean isResponsePaginated) {
        if(isResponsePaginated) {
            return responseEntityName;
        }
        if(isResponseArray) {
            return format("List<%s>", responseDtoName);
        }
        return defaultString(responseEntityName, "Void");
    }

    private Object methodParameters(Map operation) {
        List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
        List methodParams = params.stream()
                .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                .map(param -> {
                    String javaType = getJavaTypeOrOptional(param);
                    String name = JSONPath.get(param, "$.name");
                    return javaType + " " + name;
                }).collect(Collectors.toList());
        if (operation.containsKey("x--request-dto")) {
            var dto = (String) operation.get("x--request-dto");
            methodParams.add(format("%s%s%s %s", openApiModelNamePrefix, dto, openApiModelNameSuffix, "reqBody"));
        }
        return StringUtils.join(methodParams, ", ");
    }

    private String reqBodyVariableName(Map<String, Object> serviceMethod, Map zdl) {
        if (serviceMethod == null) { return "reqBody"; }
        var parameterType = JSONPath.get(serviceMethod, "parameter");
        var isInline = JSONPath.get(zdl, "$.inputs." + parameterType + ".options.inline", false);
        if(isInline) {
            var paramSignature = ZDLJavaSignatureUtils.inputSignature((String) parameterType, (Map) serviceMethod, zdl, inputDTOSuffix);
            var  methodParametersCallSignature = paramSignature.get(paramSignature.size() - 1).split(" ")[1];
            var params = methodParametersCallSignature.split(", ");
            var reqBody = "";
            for (int i = 0; i < params.length; i++) {
                if(!"id".equals(params[i]) && !"pageable".equals(params[i])) {
                    reqBody = params[i];
                }
            }
            return methodParametersCallSignature.replaceFirst(reqBody, "reqBody");
        }
        return "reqBody";
    }

    private String serviceMethodParameter(Map<String, Object> method, Map<String, Object> zdlModel) {
        if(method == null) { return "Entity"; }
        return ZDLHttpUtils.getRequestBodyType(method, zdlModel);
    }

    private String mappedInputVariable(Map<String, Object> method) {
        return "input";
    }

    String statusCode(Map operation) {
        var status = (String) JSONPath.get(operation, "x--response.x--statusCode");
        return "default".equals(status) ? "200" : status;
    }

    String innerArrayDTO(Map<String, Object> responseSchema) {
        for (String jsonPath : paginatedDtoItemsJsonPath) {
            String paginatedDto = JSONPath.get(responseSchema, jsonPath + ".x--schema-name");
            if (paginatedDto != null) {
                return paginatedDto;
            }
        }
        return null;
    }

    private String serviceMethodCall(Map serviceMethod, Map operation, Map zdl) {
        if (serviceMethod == null) { // legacy
            var operationId = JSONPath.get(operation, "operationId");
            return format("%s(%s)", operationId, "input");
        }
        var methodName = JSONPath.get(serviceMethod, "name");
        var methodParametersCallSignature = ZDLJavaSignatureUtils.methodParametersCallSignature((Map) serviceMethod, zdl, inputDTOSuffix);
        var paramId = ObjectUtils.firstNonNull(ZDLHttpUtils.getFirstPathParamsFromMethod((Map) serviceMethod), "id");

        if(operation.get("x--request-schema") != null) {
            // find the last parameter name, that is not the pagination: that is the reqBody variable name
            var params = methodParametersCallSignature.split(", ");
            var reqBody = "";
            for (int i = 0; i < params.length; i++) {
                if(!"id".equals(params[i]) && !"pageable".equals(params[i])) {
                    reqBody = params[i];
                }
            }
            methodParametersCallSignature = methodParametersCallSignature.replaceFirst(reqBody, "input");
        }

        methodParametersCallSignature = methodParametersCallSignature.replaceFirst("^id$", paramId);
        methodParametersCallSignature = methodParametersCallSignature.replaceFirst("^id, ", paramId + ", ");
        methodParametersCallSignature = methodParametersCallSignature.replaceAll("pageable", "pageOf(page, limit, sort)");

        return format("%s(%s)", methodName, methodParametersCallSignature);
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
                    methodParams.add(format("%s%s%s %s = null;", openApiModelNamePrefix, operation.get("x--request-dto"), openApiModelNameSuffix, "reqBody"));
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
