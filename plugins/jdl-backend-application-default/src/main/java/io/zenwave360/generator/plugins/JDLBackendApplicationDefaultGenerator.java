package io.zenwave360.generator.plugins;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.Utils;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.zenwave360.generator.templating.OutputFormatType.JAVA;

public class JDLBackendApplicationDefaultGenerator extends AbstractJDLGenerator {

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

    @DocumentedOption(description = "Suffix for CRUD operations DTOs (default: Input)")
    public String inputDTOSuffix = "Input";

    @DocumentedOption(description = "Suffix for search criteria DTOs (default: Criteria)")
    public String criteriaDTOSuffix = "Criteria";

    @DocumentedOption(description = "Suffix for elasticsearch document entities (default: Document)")
    public String searchDTOSuffix = "Document";

    public JDLBackendApplicationDefaultGenerator withSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    private String templatesFolder = "io/zenwave360/generator/plugins/JDLEntitiesGenerator/";

    Object[] enumTemplate = { "src/main/java", "core/domain/common/Enum.java", "core/domain/{{enum.name}}.java", JAVA};

    Function<Map<String, Object>, Boolean> skipEntityResource = (model) -> JSONPath.get(model, "$.entity.options.service") == null;
    Function<Map<String, Object>, Boolean> skipSearchCriteria = (model) -> JSONPath.get(model, "$.entity.options.withSearchCriteria") == null;

    Function<Map<String, Object>, Boolean> skipElasticSearch = (model) -> JSONPath.get(model, "$.entity.options.search") == null;
    List<Object[]> templatesByEntity = List.of(
            new Object[] { "src/main/java", "core/domain/{{persistence}}/Entity.java", "core/domain/{{entity.name}}.java", JAVA},
            new Object[] { "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "core/outbound/{{persistence}}/{{entity.className}}Repository.java", JAVA },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityCriteria.java", "core/inbound/dtos/{{entity.className}}{{criteriaDTOSuffix}}.java", JAVA, skipSearchCriteria },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityInput.java", "core/inbound/dtos/{{entity.className}}{{inputDTOSuffix}}.java", JAVA },
            new Object[] { "src/main/java", "core/implementation/mappers/EntityMapper.java", "core/implementation/mappers/{{entity.className}}Mapper.java", JAVA },
            new Object[] { "src/main/java", "adapters/web/{{webFlavor}}/EntityResource.java", "adapters/web/{{entity.className}}Resource.java", JAVA, skipEntityResource },
            new Object[] { "src/main/java", "core/outbound/search/EntityDocument.java", "core/outbound/search/{{entity.className}}{{searchDTOSuffix}}.java", JAVA, skipElasticSearch },
            new Object[] { "src/main/java", "core/outbound/search/EntitySearchRepository.java", "core/outbound/search/{{entity.className}}SearchRepository.java", JAVA, skipElasticSearch }
    );

    List<Object[]> templatesByService = List.of(
            new Object[] { "src/main/java", "core/inbound/Service.java", "core/inbound/{{service.name}}.java", JAVA },
            new Object[] { "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "core/implementation/{{service.name}}Impl.java", JAVA }
    );

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        Function<Map<String, Object>, Boolean> skip = templateNames.length > 4? (Function) templateNames[4] : null;
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{asPackageFolder basePackage}}/" + templateNames[2])
                .withMimeType((OutputFormatType) templateNames[3])
                .withSkip(skip);
    }

    protected Map<String, Object> getJDLModel(Map<String, Object> contextModel) {
        return (Map) contextModel.get(sourceProperty);
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        var templateOutputList = new ArrayList<TemplateOutput>();
        var apiModel = getJDLModel(contextModel);

        Map<String, Map<String, Object>> entities = (Map) apiModel.get("entities");
        for (Map<String, Object> entity : entities.values()) {
            if(!isGenerateEntity((String) entity.get("name"))) {
                continue;
            }
            for (Object[] templateValues : templatesByEntity) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("entity",  entity)));
            }
        }

        Map<String, Map<String, Object>> enums = JSONPath.get(apiModel, "$.enums.enums");
        for (Map<String, Object> enumValue : enums.values()) {
            if(!isGenerateEntity((String) enumValue.get("name"))) {
                continue;
            }
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(enumTemplate), Map.of("enum", enumValue)));
        }

        Map<String, Map<String, Object>> services = JSONPath.get(apiModel, "$.options.options.service", Collections.emptyMap());
        for (Map<String, Object> service : services.values()) {
            String serviceName = ((String) service.get("value"));
            service.put("name", serviceName);
            List<Map<String, Object>> entitiesByService = getEntitiesByService(service, apiModel);
            service.put("entities", entitiesByService);
            boolean isGenerateService = entitiesByService.stream().anyMatch(entity -> isGenerateEntity((String) entity.get("name")));
            if(!isGenerateService) {
                continue;
            }
            for (Object[] templateValues : templatesByService) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("service",  service, "entities", entitiesByService)));
            }
        }

        return templateOutputList;
    }

    protected boolean isGenerateEntity(String entity) {
        return entities.isEmpty() || entities.contains(entity);
    }

    protected List<Map<String, Object>> getEntitiesByService(Map<String, Object> service, Map<String, Object> apiModel) {
        List entityNames = ((List) service.get("entityNames"));
        if(entityNames.size() == 1 && "*".equals(entityNames.get(0))) {
            entityNames = JSONPath.get(apiModel, "$.entities[*].name");
        }
        List<Map<String, Object>> entitiesByService = (List<Map<String, Object>>) entityNames.stream().map(e -> JSONPath.get(apiModel, "$.entities." + e)).collect(Collectors.toList());
        List excludedNames = ((List) service.get("excludedNames"));
        if(excludedNames.size() > 0) {
            entitiesByService = entitiesByService.stream().filter(e -> !excludedNames.contains(e.get("name"))).collect(Collectors.toList());
        }
        service.put("entityNames", entityNames);
        return entitiesByService;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(Utils.asConfigurationMap(this));
        model.put("context", contextModel);
        model.put("jdl", getJDLModel(contextModel));
        model.put("webFlavor", style == ProgrammingStyle.imperative? "mvc" : "webflux");
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

}
