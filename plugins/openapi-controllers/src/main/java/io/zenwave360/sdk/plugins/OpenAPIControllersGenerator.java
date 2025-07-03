package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.Lists;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.utils.ZDLHttpUtils;
import io.zenwave360.sdk.zdl.utils.ZDLJavaSignatureUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractOpenAPIGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.lang3.tuple.Pair;

public class OpenAPIControllersGenerator extends AbstractOpenAPIGenerator {

    public String apiProperty = "api";
    public String zdlProperty = "zdl";

    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Include Controller Unit tests (using ServicesInMemoryConfig)")
    public boolean includeControllerTests = true;

    @DocumentedOption(description = "JSONPath list to search for response DTO schemas for list or paginated results. User '$.items' for lists or '$.properties.<content property>.items' for paginated results.")
    public List<String> paginatedDtoItemsJsonPath = List.of("$.items", "$.properties.content.items");

    protected HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    public ProjectTemplates templates = new OpenAPIControllersTemplates();

    @Override
    public void onPropertiesSet() {
        super.onPropertiesSet();
        templates.setLayout(layout);
        templates.getTemplateHelpers(this)
                .forEach(helper -> handlebarsEngine.getHandlebars().registerHelpers(helper));
    }



    protected Map<String, Object> getZDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(zdlProperty);
    }

    protected Map<String, Object> getOpenAPIModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(apiProperty);
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        var generatedProjectFiles = new GeneratedProjectFiles();
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
            Map<String, Map<String, Object>> mapperRequestDtoEntity = new HashMap<>();
            Map<String, Map<String, Object>> mapperResponseDtoEntity = new HashMap<>();

            for (Map operation : operationByServiceEntry.getValue()) {
                var method = ZDLFindUtils.findServiceMethod((String) operation.get("operationId"), zdlModel);

                String httpVerb = JSONPath.get(operation, "x--httpVerb");
                String requestDto = JSONPath.get(operation, "$.x--request-dto");
                String requestDtoName = requestDto != null ? openApiModelNamePrefix + requestDto + openApiModelNameSuffix : null;
                String inputType = method != null? ZDLHttpUtils.getRequestBodyType(method, zdlModel) : "Entity";
                String responseSchemaName = JSONPath.get(operation, "$.x--response.x--response-dto");
                String responseEntityName = responseSchemaName != null ? openApiModelNamePrefix + responseSchemaName + openApiModelNameSuffix : null;
                String responseDtoName = responseSchemaName != null ? openApiModelNamePrefix + responseSchemaName + openApiModelNameSuffix : null;
                String outputType = JSONPath.get(method, "$.returnType");
                boolean isResponseArray = method != null? JSONPath.get(method, "$.returnTypeIsArray", false) : false;
                boolean isResponsePaginated = method != null? JSONPath.get(method, "$.options.paginated", false) : false;
                boolean isBinaryDownload = JSONPath.get(method, "$.options.filedownload") != null;
                if (isResponseArray) {
                    var innerArrayDTO = innerArrayDTO(JSONPath.get(operation, "$.x--response.x--response-schema"));
                    responseDtoName = innerArrayDTO != null ? openApiModelNamePrefix + innerArrayDTO + openApiModelNameSuffix : responseDtoName;
                }

                var methodParameters = methodParameters(operation);
                var serviceMethodParameter = serviceMethodParameter(method, zdlModel);

                serviceOperations.add(Maps.of(
                        "operationId", operation.get("operationId"),
                        "operation", operation,
                        "httpMethod", httpVerb,
                        "serviceMethod", method,
                        "statusCode", statusCode(operation),
                        "requestBodySchema", requestDto,
                        "requestDtoName", requestDtoName,
                        "methodParameters", methodParameters,
                        "methodParameterInstances", methodParameterInstances(operation),
                        "methodParameterPlaceholders", methodParameterPlaceholders(operation),
                        "reqBodyVariableName", reqBodyVariableName(method, zdlModel),
                        "serviceMethodParameter", serviceMethodParameter,
                        "serviceMethodCall", serviceMethodCall(method, operation, zdlModel),
                        "mappedInputVariable", mappedInputVariable(method),
                        "inputType", inputType,
                        "responseSchemaName", responseSchemaName,
                        "responseDtoName", responseDtoName,
                        "responseEntityName", responseEntityName,
                        "responseEntityExpression", responseEntityExpression(responseEntityName, responseDtoName, isResponseArray, isResponsePaginated, isBinaryDownload),
                        "methodReturnType", outputType,
                        "methodReturnTypeInstance", NamingUtils.asInstanceName(outputType),
                        "returnTypeIsOptional", JSONPath.get(method, "$.returnTypeIsOptional", false),
                        "isResponseArray", isResponseArray,
                        "isResponsePaginated", isResponsePaginated,
                        "isBinaryDownload", isBinaryDownload,
                        "isMultiPart", JSONPath.get(operation, "requestBody.content['multipart/form-data']") != null
                ));

                if ("patch".equals(httpVerb)) {
                    // skip mapper for Map input
                } else if (requestDto != null && inputType != null) {
                    var requestKey = format("%s_%s", requestDtoName, inputType);
                    Maps.getOrCreateDefault(mapperRequestDtoEntity, requestKey, new HashMap<>())
                            .putAll(Map.of("requestDto", requestDtoName, "inputType", inputType, "operation", operation));
                } else if (inputType != null && StringUtils.isNotBlank(serviceMethodParameter)) {
                    var requestKey = format("%s_%s", methodParameters, inputType);
                    Maps.getOrCreateDefault(mapperRequestDtoEntity, requestKey, new HashMap<>())
                            .putAll(Map.of("methodParameters", methodParameters, "inputType", serviceMethodParameter));
                }
                if (responseSchemaName != null && outputType != null) {
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

            var templates = Lists.concat(this.templates.singleTemplates, this.templates.serviceTemplates);
            for (var template : templates) {
                generatedProjectFiles.services.addAll(operationByServiceEntry.getKey(), generateTemplateOutput(contextModel, template, serviceModel));
            }
        }

        return generatedProjectFiles;
    }

    private static String responseEntityExpression(String responseEntityName, String responseDtoName, boolean isResponseArray, boolean isResponsePaginated, boolean isBinaryDownload) {
        if(isBinaryDownload) {
            return "Resource";
        }
        if(isResponsePaginated) {
            return responseEntityName;
        }
        if(isResponseArray) {
            return format("List<%s>", responseDtoName);
        }
        return defaultString(responseEntityName, "Void");
    }

    private String reqBodyVariableName(Map<String, Object> serviceMethod, Map zdl) {
        if (serviceMethod == null) { return "reqBody"; }
        var parameterType = JSONPath.get(serviceMethod, "parameter");
        var isInline = JSONPath.get(zdl, "$.inputs." + parameterType + ".options.inline", false);
        if(isInline) {
            var paramSignature = ZDLJavaSignatureUtils.inputSignature((String) parameterType, (Map) serviceMethod, zdl);
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

    private String methodParameters(Map operation) {
        return ZDLHttpUtils.methodParameters(operation, openApiModelNamePrefix, openApiModelNameSuffix).stream().map(param -> {
            return param.getKey() + " " + param.getValue();
        }).collect(Collectors.joining(", "));
    }

    public String methodParameterInstances(Map operation) {
        var methodParameters = methodParameters(operation);
        if (methodParameters.isEmpty()) {
            return "";
        }
        return Arrays.stream(methodParameters(operation).split(", "))
                .map(param -> param.split(" ")[1])
                .collect(Collectors.joining(", "));
    }

    private Object methodParameterPlaceholders(Map operation) {
        var methodParameters = methodParameters(operation);
        if (methodParameters.isEmpty()) {
            return "";
        }
        return Arrays.stream(methodParameters(operation).split(", "))
                .map(param -> "{}")
                .collect(Collectors.joining(", "));
    }


    private String serviceMethodParameter(Map<String, Object> method, Map<String, Object> zdlModel) {
        if(method == null) { return "Entity"; }
        return ZDLJavaSignatureUtils.findServiceMethodMainParameter(method, zdlModel);
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
        var methodParametersCallSignature = ZDLJavaSignatureUtils.methodParametersCallSignature((Map) serviceMethod, zdl);
        var paramId = serviceMethod.get("paramId") != null? ObjectUtils.firstNonNull(ZDLHttpUtils.getFirstPathParamsFromMethod((Map) serviceMethod), "id") : null;

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

        if(paramId != null) {
            methodParametersCallSignature = methodParametersCallSignature.replaceFirst("id", paramId);
        }
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
        model.putAll(templates.getDocumentedOptions());
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

}
