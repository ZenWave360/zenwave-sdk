package io.zenwave360.generator.plugins;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.AbstractJDLGenerator;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.OutputFormatType;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.utils.JSONPath;

import java.util.ArrayList;
import java.util.Arrays;
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

    @DocumentedOption(description = "Persistence")
    public PersistenceType persistence = PersistenceType.mongodb;

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

    private String templatesFolder = "io/zenwave360/generator/plugins/JDLEntitiesGenerator/";

    Object[] enumTemplate = { "src/main/java", "core/domain/common/Enum.java", "core/domain/{{enum.name}}.java", JAVA};

    boolean useSemanticAnnotations = false;

    boolean is(Map<String, Object> model, String ...annotations) {
        String annotationsFilter = Arrays.stream(annotations).map(a -> "@." + a).collect(Collectors.joining(" || "));
        return !((List) JSONPath.get(model, "$.entity.options[?(" + annotationsFilter + ")]")).isEmpty();
    }

    Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> useSemanticAnnotations && !is(model, "aggregate");
    Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo");
    Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo");
    Function<Map<String, Object>, Boolean> skipVO = (model) -> useSemanticAnnotations && !is(model, "vo");
    Function<Map<String, Object>, Boolean> skipEntityResource = (model) -> is(model, "vo") || !is(model, "service");
    Function<Map<String, Object>, Boolean> skipSearchCriteria = (model) -> is(model, "vo") || !is(model, "searchCriteria");
    Function<Map<String, Object>, Boolean> skipElasticSearch = (model) -> is(model, "vo") || !is(model, "search");
    protected List<Object[]> templatesByEntity = List.of(
            new Object[] { "src/main/java", "core/domain/vo/Entity.java", "core/domain/{{entity.name}}.java", JAVA, skipVO },
            new Object[] { "src/main/java", "core/domain/{{persistence}}/Entity.java", "core/domain/{{entity.name}}.java", JAVA, skipEntity},
            new Object[] { "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "core/outbound/{{persistence}}/{{entity.className}}Repository.java", JAVA, skipEntityRepository },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityCriteria.java", "core/inbound/dtos/{{criteriaClassName entity }}.java", JAVA, skipSearchCriteria },
            new Object[] { "src/main/java", "core/inbound/dtos/EntityInput.java", "core/inbound/dtos/{{entity.className}}{{inputDTOSuffix}}.java", JAVA, skipEntity },
            new Object[] { "src/main/java", "core/implementation/mappers/EntityMapper.java", "core/implementation/mappers/{{entity.className}}Mapper.java", JAVA, skipEntity },
            new Object[] { "src/main/java", "adapters/web/{{webFlavor}}/EntityResource.java", "adapters/web/{{entity.className}}Resource.java", JAVA, skipEntityResource },
            new Object[] { "src/main/java", "core/outbound/search/EntityDocument.java", "core/outbound/search/{{entity.className}}{{searchDTOSuffix}}.java", JAVA, skipElasticSearch },
            new Object[] { "src/main/java", "core/outbound/search/EntitySearchRepository.java", "core/outbound/search/{{entity.className}}SearchRepository.java", JAVA, skipElasticSearch },

            new Object[] { "src/test/java", "core/outbound/{{persistence}}/{{style}}/InMemoryMongoRepository.java", "core/outbound/{{persistence}}/inmemory/InMemoryMongoRepository.java", JAVA, skipEntityRepository },
            new Object[] { "src/test/java", "core/outbound/{{persistence}}/{{style}}/EntityRepositoryInMemory.java", "core/outbound/{{persistence}}/inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository }
    );

    protected List<Object[]> templatesByService = List.of(
            new Object[] { "src/main/java", "core/inbound/Service.java", "core/inbound/{{service.name}}.java", JAVA },
            new Object[] { "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "core/implementation/{{service.name}}Impl.java", JAVA },
            new Object[] { "src/test/java", "core/implementation/{{style}}/ServiceTest.java", "core/implementation/{{service.name}}Test.java", JAVA }
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

        useSemanticAnnotations = ((List) JSONPath.get(apiModel, "$.entities[*][?(@.options.aggregate)]")).size() > 0;

        Map<String, Map<String, Object>> entities = (Map) apiModel.get("entities");
        for (Map<String, Object> entity : entities.values()) {
            if(!isGenerateEntity(entity)) {
                continue;
            }
            for (Object[] templateValues : templatesByEntity) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("entity",  entity)));
            }
        }

        Map<String, Map<String, Object>> enums = JSONPath.get(apiModel, "$.enums.enums");
        for (Map<String, Object> enumValue : enums.values()) {
            if(!isGenerateEntity(enumValue)) {
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
            boolean isGenerateService = entitiesByService.stream().anyMatch(entity -> isGenerateEntity(entity));
            if(!isGenerateService) {
                continue;
            }
            for (Object[] templateValues : templatesByService) {
                templateOutputList.addAll(generateTemplateOutput(contextModel, asTemplateInput(templateValues), Map.of("service",  service, "entities", entitiesByService)));
            }
        }

        return templateOutputList;
    }

    {
        handlebarsEngine.getHandlebars().registerHelper("fieldType", (context, options) -> {
            Map field = (Map) context;
            String type = (String) field.get("type");
            String prefix = (String) options.hash.getOrDefault("prefix", "");
            String suffix = (String) options.hash.getOrDefault("suffix", "");
            if(field.get("isArray") == Boolean.TRUE) {
                return String.format("List<%s%s%s>", prefix, type, suffix);
            }
            return String.format("%s%s%s", prefix, type, suffix);
        });

        handlebarsEngine.getHandlebars().registerHelper("criteriaClassName", (context, options) -> {
            Map entity = (Map) context;
            Object criteria = JSONPath.get(entity, "$.options.searchCriteria");
            if(criteria instanceof String) {
                return criteria;
            }
            if(criteria == Boolean.TRUE) {
                return String.format("%s%s", entity.get("className"), criteriaDTOSuffix);
            }
            return "Pageable";
        });

        handlebarsEngine.getHandlebars().registerHelper("skipEntityRepository", (context, options) -> {
            Map entity = (Map) context;
            return skipEntityRepository.apply(Map.of("entity", entity));
        });

        handlebarsEngine.getHandlebars().registerHelper("skipEntityId", (context, options) -> {
            Map entity = (Map) context;
            return skipEntityId.apply(Map.of("entity", entity));
        });
    }
    protected boolean isGenerateEntity(Map entity) {
        boolean skip = JSONPath.get(entity, "options.skip", false);
        String entityName = (String) entity.get("name");
        return !skip && (entities.isEmpty() || entities.contains(entityName));
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
        model.putAll(this.asConfigurationMap());
        model.put("context", contextModel);
        model.put("jdl", getJDLModel(contextModel));
        model.put("webFlavor", style == ProgrammingStyle.imperative? "mvc" : "webflux");
        model.putAll(extModel);
        return getTemplateEngine().processTemplates(model, List.of(template));
    }

}
