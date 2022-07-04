package io.zenwave360.generator.plugins;

import com.github.jknack.handlebars.Options;
import com.oracle.truffle.js.runtime.builtins.JSON;
import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.zenwave360.generator.templating.OutputFormatType.JAVA;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class JDLOpenAPIControllersGenerator extends AbstractJDLGenerator {

    enum ProgrammingStyle {
        imperative, reactive;
    }

    public String openapiProperty = "openapi";
    public String jdlProperty = "jdl";

    @DocumentedOption(description = "OpenAPI operationIds to generate code for")
    public List<String> operationIds = new ArrayList<>();

    @DocumentedOption(description = "The package to used by OpenAPI-Generator for generated api objects/classes")
    public String openApiApiPackage;

    @DocumentedOption(description = "The package to used by OpenAPI-Generator for generated model objects/classes")
    public String openApiModelPackage = "{{openApiApiPackage}}";

    @DocumentedOption(description = "Sets the prefix for model enums and classes used by OpenAPI-Generator")
    public String openApiModelNamePrefix = "";

    @DocumentedOption(description = "Sets the suffix for model enums and classes used by OpenAPI-Generator")
    public String openApiModelNameSuffix = "";

    @DocumentedOption(description = "The package to generate REST Controllers")
    public String controllersPackage = "{{basePackage}}.adapters.web";

    @DocumentedOption(description = "Package where your domain entities are")
    public String entitiesPackage = "{{basePackage}}.core.domain";

    @DocumentedOption(description = "Package where your domain services/usecases interfaces are")
    public String servicesPackage = "{{basePackage}}.core.inbound";

    @DocumentedOption(description = "ProgrammingStyle imperative|reactive default: imperative")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String templatesFolder = "io/zenwave360/generator/plugins/JDLOpenAPIControllersGenerator/";

    List<Object[]> templates = List.of(
            new Object[] { "src/main/java", "web/mappers/ServiceDTOsMapper.java", "mappers/{{service.name}}DTOsMapper.java", JAVA },
            new Object[] { "src/main/java", "web/{{webFlavor}}/ServiceApiController.java", "{{service.name}}ApiController.java", JAVA }
    );

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        Function<Map<String,?>, Boolean> skip = templateNames.length > 4? (Function) templateNames[4] : null;
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{asPackageFolder controllersPackage}}/" + templateNames[2])
                .withMimeType((OutputFormatType) templateNames[3])
                .withSkip(skip);
    }

    protected Map<String, ?> getJDLModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(jdlProperty);
    }
    protected Map<String, ?> getOpenAPIModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(openapiProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        var templateOutputList = new ArrayList<TemplateOutput>();
        var openApiModel = getOpenAPIModel(contextModel);
        var jdlModel = getJDLModel(contextModel);

        String operationIdsRegex = operationIds.isEmpty()? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
        List<Map<String, Object>> operations = JSONPath.get(openApiModel, "$.paths[*][*][?(@.operationId" + operationIdsRegex + ")]");
        Map<String, List<Map<String, Object>>> operationsByService = groupOperationsByService(operations);

        for (Map.Entry<String, List<Map<String, Object>>> operationByServiceEntry : operationsByService.entrySet()) {
            Set<String> dtoNames = new HashSet();
            dtoNames.addAll(JSONPath.get(operationByServiceEntry.getValue(), "$..x--request-dto"));
            dtoNames.addAll(JSONPath.get(operationByServiceEntry.getValue(), "$..x--response.x--response-dto"));
            Collection<Map<String, Object>> entities = JSONPath.get(operationByServiceEntry.getValue(), "$..x--entity[?(@.className)]"); // filters null
            entities = entities.stream().distinct().collect(Collectors.toList());
            Map dtoWithEntityMap = (Map) dtoNames.stream()
                    .filter(dto -> dto != null && !dto.endsWith("Paginated"))
                    .collect(Collectors.toMap(dto -> dto, dto -> Map.of("dtoName", dto, "x--entity", getOpenApiSchema(openApiModel, dto))));
            Map service = Map.of("service", Map.of("name", operationByServiceEntry.getKey(), "operations", operationByServiceEntry.getValue(), "dtos", dtoWithEntityMap, "entities", entities));
            for (Object[] template : templates) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(template), service));
            }
        }

        return templateOutputList;
    }

    protected Map getOpenApiSchema(Map openApiModel, String schemaName) {
        return JSONPath.get(openApiModel, "$.components.schemas." + schemaName + ".x--entity");
    }

    protected Map<String, List<Map<String, Object>>> groupOperationsByService(List<Map<String, Object>> operations) {
        Map<String, List<Map<String, Object>>> operationsByService = new HashMap<>();
        for (Map<String, Object> operation : operations) {
            String tagName = (String) ObjectUtils.firstNonNull(operation.get("x--normalizedTagName"), "Default");
            if(!operationsByService.containsKey(tagName)) {
                operationsByService.put(tagName, new ArrayList<>());
            }
            String responseDto = JSONPath.get(operation, "$.x--response.x--response-dto");
//            operation.put("jdl-entity", dtoToEntityMap.get(responseDto));
            operationsByService.get(tagName).add(operation);
        }
        return operationsByService;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, Map<String, ?> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(asConfigurationMap());
        model.put("context", contextModel);
        model.put("openapi", getOpenAPIModel(contextModel));
        model.put("jdl", getJDLModel(contextModel));
        model.put("webFlavor", style == ProgrammingStyle.imperative? "mvc" : "webflux");
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("orVoid", (context, options) -> {
            return StringUtils.isNotBlank((String) context)? context : "Void";
        });

        handlebarsEngine.getHandlebars().registerHelper("asMethodParameters", (context, options) -> {
            if(context instanceof Map) {
                Map operation = (Map) context;
                List<Map<String, Object>> params = (List) operation.getOrDefault("parameters", Collections.emptyList());
                List methodParams = params.stream()
                        .sorted((param1, param2) -> compareParamsByRequire(param1, param2))
                        .map(param -> {
                            String javaType = getJavaType(param);
                            String name = JSONPath.get(param, "$.name");
                            return javaType + " " + name;
                        }).collect(Collectors.toList());
                if(operation.containsKey("x--request-dto")) {
                    methodParams.add(String.format("%s%s%s %s", openApiModelNamePrefix, operation.get("x--request-dto"), openApiModelNameSuffix, "reqBody"));
                }
                return StringUtils.join(methodParams, ", ");
            }
            return options.fn(context);
        });
    }

    protected int compareParamsByRequire(Map<String, Object> param1, Map<String, Object> param2) {
        boolean required1 = JSONPath.get(param1, "required", false);
        boolean required2 = JSONPath.get(param2, "required", false);
        return (required1 && required2) || (!required1 && !required2)? 0 : required1? -1 : 1;
    }
    protected String getJavaType(Map<String, Object> param) {
        String type = JSONPath.get(param, "$.schema.type");
        String format = JSONPath.get(param, "$.schema.format");
        if("date".equals(format)) {
            return "LocalDate";
        }
        if("date-time".equals(format)) {
            return "Instant";
        }
        if("integer".equals(type) && "int32".equals(format)) {
            return "Integer";
        }
        if("integer".equals(type) && "int64".equals(format)) {
            return "Long";
        }
        if("number".equals(type)) {
            return "BigDecimal";
        }
        if("boolean".equals(type)) {
            return "Boolean";
        }

        return "String";
    }
}
