package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.zenwave360.generator.templating.OutputFormatType.JAVA;

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

    @DocumentedOption(description = "Maps openapi dtos to jdl entity names")
    public Map<String, String> dtoToEntityNameMap = new HashMap<>();

    private Map<String, Map<String, Object>> dtoToEntityMap = new HashMap<>();

    @DocumentedOption(description = "ProgrammingStyle imperative|reactive default: imperative")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String templatesFolder = "io/zenwave360/generator/plugins/JDLOpenAPIControllersGenerator/";

    List<Object[]> templates = List.of(
            new Object[] { "src/main/java", "web/mappers/ServiceDTOsMapper.java", "{{asPackageFolder controllersPackage}}/mappers/{{service.name}}DTOsMapper.java", JAVA },
            new Object[] { "src/main/java", "web/{{webFlavor}}/ServiceApiController.java", "{{asPackageFolder controllersPackage}}/{{service.name}}ApiController.java", JAVA }
    );

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        Function<Map<String,?>, Boolean> skip = templateNames.length > 4? (Function) templateNames[4] : null;
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{asPackageFolder basePackage}}/" + templateNames[2])
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

        buildDtoToEntityMap(openApiModel, jdlModel);

        String operationIdsRegex = operationIds.isEmpty()? "" : " =~ /(" + StringUtils.join(operationIds, "|") + ")/";
        List<Map<String, Object>> operations = JSONPath.get(openApiModel, "$.paths[*][*][?(@.operationId" + operationIdsRegex + ")]");
        Map<String, List<Map<String, Object>>> operationsByService = groupOperationsByService(operations);

        for (Map.Entry<String, List<Map<String, Object>>> operationByServiceEntry : operationsByService.entrySet()) {
            Map service = Map.of("service", Map.of("name", operationByServiceEntry.getKey(), "operations", operationByServiceEntry.getValue()));
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templates.get(1)), service));
        }


        for (Map.Entry<String, List<Map<String, Object>>> operationByServiceEntry : operationsByService.entrySet()) {
            Set dtoNames = new HashSet(JSONPath.get(operationByServiceEntry.getValue(), "$..x--response.x--response-dto"));
            Map dtosMap = (Map) dtoNames.stream().collect(Collectors.toMap(dto -> dto, dto -> Map.of("dtoName", dto, "jdl-entity", dtoToEntityMap.get(dto))));
            Map service = Map.of("service", Map.of("name", operationByServiceEntry.getKey(), "dtos", dtosMap));
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templates.get(0)), service));
        }

        return templateOutputList;
    }

    protected Map<String, List<Map<String, Object>>> groupOperationsByService(List<Map<String, Object>> operations) {
        Map<String, List<Map<String, Object>>> operationsByService = new HashMap<>();
        for (Map<String, Object> operation : operations) {
            String tagName = (String) ObjectUtils.firstNonNull(operation.get("x--normalizedTagName"), "Default");
            if(!operationsByService.containsKey(tagName)) {
                operationsByService.put(tagName, new ArrayList<>());
            }
            String responseDto = JSONPath.get(operation, "$.x--response.x--response-dto");
            operation.put("jdl-entity", dtoToEntityMap.get(responseDto));
            operationsByService.get(tagName).add(operation);
        }
        return operationsByService;
    }

    protected void buildDtoToEntityMap(Map<String, ?> openApiModel, Map<String, ?> jdlModel){
        List<Map<String, Object>> schemas = JSONPath.get(openApiModel, "$.components.schemas[*]");
        for (Map<String, Object> schema : schemas) {
            String schemaName = (String) schema.get("x--schema-name");
            String entityName =  dtoToEntityNameMap.getOrDefault(schemaName, (String) schema.get("x-jdl-entity"));
            entityName = StringUtils.defaultString(entityName, StringUtils.capitalize(schemaName));
            Map<String, Object> entity = JSONPath.get(jdlModel, "$.entities." + entityName);
            if(entity == null) {
                entity = Map.of("name", entityName, "className", entityName, "instanceName", entityName, "classNamePlural", entityName + "s", "instanceNamePlural", entityName + "s");
            }
            dtoToEntityMap.put(schemaName, entity);
        }
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

}
