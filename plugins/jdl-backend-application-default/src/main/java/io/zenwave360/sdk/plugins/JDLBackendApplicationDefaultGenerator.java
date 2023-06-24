package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractJDLGenerator;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.WebFlavorType;
import io.zenwave360.sdk.templating.*;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLBackendApplicationDefaultGenerator extends AbstractJDLGenerator {

    public String sourceProperty = "jdl";

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Persistence")
    public PersistenceType persistence = PersistenceType.mongodb;

    @DocumentedOption(description = "SQL database flavor")
    public DatabaseType databaseType = DatabaseType.postgresql;

    @DocumentedOption(description = "Programming Style")
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
    {
        handlebarsEngine.getHandlebars().registerHelpers(new JDLBackendApplicationDefaultHelpers(this));
        handlebarsEngine.getHandlebars().registerHelpers(new JDLBackendApplicationDefaultJpaHelpers(this));
    }

    private String templatesFolder = "io/zenwave360/sdk/plugins/JDLEntitiesGenerator/";


    boolean is(Map<String, Object> model, String... annotations) {
        String annotationsFilter = Arrays.stream(annotations).map(a -> "@." + a).collect(Collectors.joining(" || "));
        return !((List) JSONPath.get(model, "$.entity.options[?(" + annotationsFilter + ")]")).isEmpty();
    }

    Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> !is(model, "aggregate");
    Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo", "input", "isSuperClass");
    Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo", "input");
    Function<Map<String, Object>, Boolean> skipVO = (model) -> !is(model, "vo");

    Function<Map<String, Object>, Boolean> skipEntityInput = (model) -> is(model, "vo", "input") || inputDTOSuffix == null || inputDTOSuffix.isEmpty();
    Function<Map<String, Object>, Boolean> skipInput = (model) -> !is(model, "input");
    Function<Map<String, Object>, Boolean> skipEntityResource = (model) -> is(model, "vo", "input") || !is(model, "service");
    Function<Map<String, Object>, Boolean> skipSearchCriteria = (model) -> is(model, "vo", "input") || !is(model, "searchCriteria");
    Function<Map<String, Object>, Boolean> skipElasticSearch = (model) -> is(model, "vo", "input") || !is(model, "search");

    Object[] enumTemplate = {"src/main/java", "core/domain/common/Enum.java", "core/domain/{{enum.name}}.java", JAVA};
    Object[] enumDtoTemplate = {"src/main/java", "core/inbound/dtos/Enum.java", "core/inbound/dtos/{{enum.name}}.java", JAVA};
    protected List<Object[]> templatesByEntity = List.of(
            new Object[] {"src/main/java", "core/domain/vo/Entity.java", "core/domain/{{entity.name}}.java", JAVA, skipVO},
            new Object[] {"src/main/java", "core/domain/{{persistence}}/Entity.java", "core/domain/{{entity.name}}.java", JAVA, skipEntity},
            new Object[] {"src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "core/outbound/{{persistence}}/{{entity.className}}Repository.java", JAVA, skipEntityRepository, true},
            new Object[] {"src/main/java", "core/inbound/dtos/EntityCriteria.java", "core/inbound/dtos/{{criteriaClassName entity }}.java", JAVA, skipSearchCriteria},
            new Object[] {"src/main/java", "core/inbound/dtos/EntityInput.java", "core/inbound/dtos/{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput},
            new Object[] {"src/main/java", "core/inbound/dtos/EntityInput.java", "core/inbound/dtos/{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipInput},
            new Object[] {"src/main/java", "core/implementation/mappers/EntityMapper.java", "core/implementation/mappers/{{entity.className}}Mapper.java", JAVA, skipEntity, true},
//            new Object[] {"src/main/java", "adapters/web/{{webFlavor}}/EntityResource.java", "adapters/web/{{entity.className}}Resource.java", JAVA, skipEntityResource},
            new Object[] {"src/main/java", "core/domain/search/EntityDocument.java", "core/domain/search/{{entity.className}}{{searchDTOSuffix}}.java", JAVA, skipElasticSearch},
            new Object[] {"src/main/java", "core/outbound/search/EntitySearchRepository.java", "core/outbound/search/{{entity.className}}SearchRepository.java", JAVA, skipElasticSearch, true},

            new Object[] {"src/test/java", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java", "infrastructure/{{persistence}}/BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true},
            new Object[] {"src/test/java", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java", "infrastructure/{{persistence}}/{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true},
            new Object[] {"src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "infrastructure/{{persistence}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true},
            new Object[] {"src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java", "infrastructure/{{persistence}}/inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true}
    );

    protected List<Object[]> templatesByInputOutput = (List) List.of(((Object)
            new Object[] {"src/main/java", "core/inbound/dtos/InputOrOutput.java", "core/inbound/dtos/{{entity.className}}.java", JAVA}));

    protected List<Object[]> templatesByService = List.of(
            new Object[] {"src/main/java", "core/inbound/Service.java", "core/inbound/{{service.name}}.java", JAVA},
            new Object[] {"src/main/java", "core/implementation/{{persistence}}/{{style}}/ServiceImpl.java", "core/implementation/{{service.name}}Impl.java", JAVA},
            new Object[] {"src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java", "core/implementation/{{service.name}}Test.java", JAVA});

    protected List<Object[]> templatesForAllServices = List.of(
            new Object[] {"src/test/java", "config/InMemoryTestsConfig.java", "config/InMemoryTestsConfig.java", JAVA},
            new Object[] {"src/test/java", "config/InMemoryTestsManualContext.java", "config/InMemoryTestsManualContext.java", JAVA});

    protected List<Object[]> singleTemplates = List.of(
            new Object[] {"src/main/java", "core/inbound/dtos/package-info.java", "core/inbound/dtos/package-info.java", JAVA},
            new Object[] {"src/main/java", "infrastructure/package-info.java", "infrastructure/package-info.java", JAVA},
            new Object[] {"src/test/java", "ArchitectureTest.java", "ArchitectureTest.java", JAVA});

    public TemplateEngine getTemplateEngine() {
        return handlebarsEngine;
    }

    protected TemplateInput asTemplateInput(Object[] templateNames) {
        boolean skipOverwrite = templateNames.length > 5 ? (boolean) templateNames[5] : false;
        Function<Map<String, Object>, Boolean> skip = templateNames.length > 4 ? (Function) templateNames[4] : null;
        return new TemplateInput()
                .withTemplateLocation(templatesFolder + templateNames[0] + "/" + templateNames[1])
                .withTargetFile(templateNames[0] + "/{{asPackageFolder basePackage}}/" + templateNames[2])
                .withMimeType((OutputFormatType) templateNames[3])
                .withSkipOverwrite(skipOverwrite)
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
            if (!isGenerateEntity(entity)) {
                continue;
            }
            for (Object[] templateValues : templatesByEntity) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("entity", entity)));
            }
        }

        Map<String, Map<String, Object>> enums = JSONPath.get(apiModel, "$.enums.enums");
        for (Map<String, Object> enumValue : enums.values()) {
            if (!isGenerateEntity(enumValue)) {
                continue;
            }
            var comment = enumValue.get("comment");
            var isDtoInput = comment != null && comment.toString().contains("@input");
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(isDtoInput? enumDtoTemplate : enumTemplate), Map.of("enum", enumValue)));
        }

        List<Map<String, Object>> inputsOutputs = JSONPath.get(apiModel, "$.['inputs','outputs'][*]", Collections.emptyList());
        for (Map<String, Object> inputOutput : inputsOutputs) {
            for (Object[] templateValues : templatesByInputOutput) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("entity", inputOutput)));
            }
        }

        Map<String, Map<String, Object>> services = JSONPath.get(apiModel, "$.options.options.service", Collections.emptyMap());
        List<Map<String, Object>> servicesList = new ArrayList<>();
        for (Map<String, Object> service : services.values()) {
            String serviceName = ((String) service.get("value"));
            service.put("name", serviceName);
            List<Map<String, Object>> entitiesByService = getEntitiesByService(service, apiModel);
            service.put("entities", entitiesByService);
            boolean isGenerateService = entitiesByService.stream().anyMatch(entity -> isGenerateEntity(entity));
            if (!isGenerateService) {
                continue;
            }
            servicesList.add(service);
            for (Object[] templateValues : templatesByService) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("service", service, "entities", entitiesByService)));
            }
        }

        for (Object[] templateValues : templatesForAllServices) {
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("services", servicesList)));
        }


        for (Object[] templateValues : singleTemplates) {
            templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Collections.emptyMap()));
        }

        return templateOutputList;
    }

    protected String getIdJavaType() {
        return this.persistence == PersistenceType.jpa ? "Long" : "String";
    }

    protected boolean isGenerateEntity(Map entity) {
        boolean skip = JSONPath.get(entity, "options.skip", false);
        String entityName = (String) entity.get("name");
        return !skip && (entities.isEmpty() || entities.contains(entityName));
    }

    protected List<Map<String, Object>> getEntitiesByService(Map<String, Object> service, Map<String, Object> apiModel) {
        List entityNames = ((List) service.get("entityNames"));
        if (entityNames.size() == 1 && "*".equals(entityNames.get(0))) {
            entityNames = JSONPath.get(apiModel, "$.entities[*].name");
        }
        List<Map<String, Object>> entitiesByService = (List<Map<String, Object>>) entityNames.stream().map(e -> JSONPath.get(apiModel, "$.entities." + e)).collect(Collectors.toList());
        List excludedNames = ((List) service.get("excludedNames"));
        if (excludedNames != null && excludedNames.size() > 0) {
            entitiesByService = entitiesByService.stream().filter(e -> !excludedNames.contains(e.get("name"))).collect(Collectors.toList());
        }
        service.put("entityNames", entityNames);
        return entitiesByService;
    }

    public List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, TemplateInput template, Map<String, Object> extModel) {
        Map<String, Object> model = new HashMap<>();
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdl", getJDLModel(contextModel));
        model.put("idJavaType", getIdJavaType());
        model.put("webFlavor", style == ProgrammingStyle.imperative ? WebFlavorType.mvc : WebFlavorType.webflux);
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

}
