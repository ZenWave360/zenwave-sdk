package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.generator.templating.OutputFormatType.JAVA;

public class JDLEntitiesGenerator extends AbstractJDLGenerator {

    enum PersistenceType {
        mongodb;
    }

    enum ProgrammingStyle {
        imperative, reactive;
    }

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Persistence MONGODB|JPA default: MONGODB")
    public PersistenceType persistence = PersistenceType.mongodb;

    @DocumentedOption(description = "ProgrammingStyle imperative|reactive default: imperative")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    public JDLEntitiesGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String templatesFolder = "io/zenwave360/generator/plugins/JDLEntitiesGenerator/";

    Object[] enumTemplate = { "src/main/java", "core/domain/common/Enum.java", "core/domain/{{enum.name}}.java", JAVA};

    List<Object[]> templatesByEntity = List.of(
            new Object[] { "src/main/java", "core/domain/{{persistence}}/Entity.java", "core/domain/{{entity.name}}.java", JAVA},
            new Object[] { "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "core/outbound/{{persistence}}/{{entity.name}}Repository.java", JAVA },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityCriteria.java", "core/inbound/dtos/{{entity.name}}Criteria.java", JAVA },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityInput.java", "core/inbound/dtos/{{entity.name}}Input.java", JAVA },
            new Object[] { "src/main/java", "adapters/web/{{webFlavor}}/EntityResource.java", "adapters/web/{{entity.name}}Resource.java", JAVA },
            new Object[] { "src/main/java", "core/outbound/search/EntityDocument.java", "core/outbound/search/{{entity.name}}Document.java", JAVA },
            new Object[] { "src/main/java", "core/outbound/search/EntitySearchRepository.java", "core/outbound/search/{{entity.name}}SearchRepository.java", JAVA }
    );

    List<Object[]> templatesByService = List.of(
            new Object[] { "src/main/java", "core/inbound/Service.java", "core/inbound/{{service.name}}.java", JAVA },
            new Object[] { "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "core/implementation/{{service.name}}Impl.java", JAVA }
    );

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{basePackageFolder}}/" + templateNames[2])
                .withMimeType((OutputFormatType) templateNames[3]);
    }

    protected Map<String, ?> getJDLModel(Map<String, ?> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        var templateOutputList = new ArrayList<TemplateOutput>();
        var apiModel = getJDLModel(contextModel);

        Map<String, Map<String, ?>> entities = (Map) apiModel.get("entities");
        for (Map<String, ?> entity : entities.values()) {
            for (Object[] templateValues : templatesByEntity) {
                templateOutputList.add(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("entity",  entity)));
            }
        }

        Map<String, Map<String, ?>> enums = JSONPath.get(apiModel, "$.enums.enums");
        for (Map<String, ?> enumValue : enums.values()) {
            templateOutputList.add(generateTemplateOutput(contextModel, asTemplateInput(enumTemplate), Map.of("enum", enumValue)));
        }

        Map<String, Map<String, Object>> services = JSONPath.get(apiModel, "$.options.options.service", Collections.emptyMap());
        for (Map<String, Object> service : services.values()) {
            String serviceName = ((String) service.get("value"));
            service.put("name", serviceName);
            List<Map<String, ?>> entitiesByService = getEntitiesByService(service, apiModel);
            service.put("entities", entitiesByService);
            for (Object[] templateValues : templatesByService) {
                templateOutputList.add(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("service",  service, "entities", entitiesByService)));
            }
        }

        return templateOutputList;
    }

    protected List<Map<String, ?>> getEntitiesByService(Map<String, Object> service, Map<String, ?> apiModel) {
        List entityNames = ((List) service.get("entityNames"));
        if(entityNames.size() == 1 && "*".equals(entityNames.get(0))) {
            entityNames = JSONPath.get(apiModel, "$.entities[*].name");
        }
        List<Map<String, ?>> entitiesByService = (List<Map<String, ?>>) entityNames.stream().map(e -> JSONPath.get(apiModel, "$.entities." + e)).collect(Collectors.toList());
        List excludedNames = ((List) service.get("excludedNames"));
        if(excludedNames.size() > 0) {
            entitiesByService = entitiesByService.stream().filter(e -> !excludedNames.contains(e.get("name"))).collect(Collectors.toList());
        }
        service.put("entityNames", entityNames);
        return entitiesByService;
    }

    public TemplateOutput generateTemplateOutput(Map<String, ?> contextModel, TemplateInput template, Map<String, ?> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdl", getJDLModel(contextModel));
        model.put("basePackageFolder", getBasePackageFolder());
        model.put("webFlavor", style == ProgrammingStyle.imperative? "mvc" : "webflux");
        model.putAll(extModel);
        return getTemplateEngine().processTemplate(model, template);
    }

}
