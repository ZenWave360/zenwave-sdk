package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLProjectGenerator;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Generates a backend application with the following structure:
 <pre>
📦 basePackage
   📦 adapters
       └─ web
       |  └─ RestControllers (spring mvc)
       └─ events
          └─ *EventListeners (spring-cloud-streams)
   📦 core
       └─ 📦 domain
       |     └─ (entities and aggregates)
       └─ 📦 inbound
       |     ├─ dtos/
       |     └─ ServiceInterface (inbound service interface)
       ├─ 📦 outbound
       |     ├─ mongodb
       |     |  └─ *RepositoryInterface (spring-data interface)
       |     └─ jpa
       |        └─ *RepositoryInterface (spring-data interface)
       ├─ 📦 implementation
       |     ├─ mappers/
       |     └─ ServiceImplementation (inbound service implementation)
  📦 infrastructure
     ├─ mongodb
     |  └─ CustomRepositoryImpl (spring-data custom implementation)
     └─ jpa
        └─ CustomRepositoryImpl (spring-data custom implementation)
 </pre>
 */
public class BackendApplicationDefaultGenerator extends AbstractZDLProjectGenerator {

    public ProjectLayout layout;

    @DocumentedOption(description = "Entities to generate code for")
    public List<String> entities = new ArrayList<>();

    @DocumentedOption(description = "Persistence")
    public PersistenceType persistence = PersistenceType.mongodb;

    @DocumentedOption(description = "SQL database flavor")
    public DatabaseType databaseType = DatabaseType.postgresql;

    @DocumentedOption(description = "Programming Style")
    public ProgrammingStyle style = ProgrammingStyle.imperative;

    @DocumentedOption(description = "Use @Getter and @Setter annotations from Lombok")
    public boolean useLombok = false;

    @DocumentedOption(description = "Whether to add AsyncAPI/ApplicationEventPublisher as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work.")
    public boolean includeEmitEventsImplementation = true;

    @DocumentedOption(description = "Controls whether to add a read/write relationship by id when mapping relationships between aggregate (not recommended) keeping the relationship by object readonly.")
    public boolean addRelationshipsById = false;

    @DocumentedOption(description = "Specifies the Java data type for the ID fields of entities. Defaults to Long for JPA and String for MongoDB if not explicitly set.")
    public String idJavaType;


    @DocumentedOption(description = "If not empty, it will generate (and use) an `input` DTO for each entity used as command parameter")
    public String inputDTOSuffix = "";

    {
        getTemplateEngine().getHandlebars().registerHelpers(new BackendApplicationDefaultHelpers(this));
        getTemplateEngine().getHandlebars().registerHelpers(new BackendApplicationDefaultJpaHelpers(this));
    }

    protected boolean is(Map<String, Object> model, String... annotations) {
        String annotationsFilter = Arrays.stream(annotations).map(a -> "@." + a).collect(Collectors.joining(" || "));
        return !(JSONPath.get(model, "$.entity.options[?(" + annotationsFilter + ")]", List.of())).isEmpty();
    }

    protected Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> is(model, "persistence") // if polyglot persistence -> skip
            || !(is(model, "aggregate") || ZDLFindUtils.isAggregateRoot(JSONPath.get(model, "zdl"), JSONPath.get(model, "$.entity.name")));
    protected Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo", "input", "abstract");
    protected Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo", "input");
    protected Function<Map<String, Object>, Boolean> skipEntityInput = (model) -> inputDTOSuffix == null || inputDTOSuffix.isEmpty();

    protected Function<Map<String, Object>, Boolean> skipEvents = (model) -> !includeEmitEventsImplementation;
    protected Function<Map<String, Object>, Boolean> skipEventsBus = (model) -> ((Collection) model.get("events")).isEmpty();
    protected Function<Map<String, Object>, Boolean> skipInput = (model) -> is(model, "inline");
    @Override
    protected ProjectTemplates configureProjectTemplates() {
        var ts = new ProjectTemplates("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        var layout = new ProjectLayout(); // layoutNames

        ts.addTemplate(ts.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java",
                layout.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        ts.addTemplate(ts.domainEventsTemplates, "src/main/java", "core/domain/common/DomainEvent.java",
                layout.domainEventsPackage, "{{event.name}}.java", JAVA, null, true);

        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/domain/{{persistence}}/Entity.java",
                layout.entitiesPackage, "{{entity.name}}.java", JAVA, skipEntity, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java",
                layout.outboundRepositoryPackage, "{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/inbound/dtos/EntityInput.java",
                layout.inboundDtosPackage, "{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java",
                layout.infrastructureRepositoryPackage, "BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java",
                layout.infrastructureRepositoryPackage, "{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                layout.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java",
                layout.infrastructureRepositoryPackage, "inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                layout.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        ts.addTemplate(ts.enumTemplates, "src/main/java", "core/domain/common/DomainEnum.java",
                layout.entitiesPackage, "{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.inputEnumTemplates, "src/main/java", "core/domain/common/InputEnum.java",
                layout.inboundDtosPackage, "{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.eventEnumTemplates, "src/main/java", "core/domain/common/EventEnum.java",
                layout.domainEventsPackage, "{{enum.name}}.java", JAVA, skipInput, false);

        ts.addTemplate(ts.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                layout.inboundDtosPackage, "{{entity.className}}.java", JAVA, skipInput, false);
        ts.addTemplate(ts.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                layout.inboundDtosPackage, "{{entity.className}}.java", JAVA, null, false);

        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/inbound/Service.java",
                layout.inboundPackage, "{{service.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java",
                layout.coreImplementationPackage, "{{service.name}}Impl.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java",
                layout.coreImplementationMappersPackage, "BaseMapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/mappers/ServiceMapper.java",
                layout.coreImplementationMappersPackage, "{{service.name}}Mapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java",
                layout.coreImplementationPackage, "{{service.name}}Test.java", JAVA, null, true);

        ts.addTemplate(ts.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java",
                layout.coreImplementationMappersPackage, "EventsMapper.java", JAVA, skipEvents, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java",
                layout.configPackage, "RepositoriesInMemoryConfig.java", JAVA, null, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java",
                layout.configPackage, "ServicesInMemoryConfig.java", JAVA, null, true);

        ts.addTemplate(ts.allEventsTemplates, "src/main/java", "core/outbound/events/EventPublisher.java",
                layout.outboundEventsPackage, "EventPublisher.java", JAVA, skipEventsBus, false);
        ts.addTemplate(ts.allEventsTemplates, "src/main/java", "infrastructure/events/DefaultEventPublisher.java",
                layout.infrastructureEventsPackage, "DefaultEventPublisher.java", JAVA, skipEventsBus, false);
        ts.addTemplate(ts.allEventsTemplates, "src/test/java", "infrastructure/events/InMemoryEventPublisher.java",
                layout.infrastructureEventsPackage, "InMemoryEventPublisher.java", JAVA, skipEventsBus, false);

        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java",
                layout.configPackage, "TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java",
                layout.configPackage, "DockerComposeInitializer.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java",
                layout.inboundDtosPackage, "package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "infrastructure/package-info.java",
                layout.infrastructurePackage, "package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "ArchitectureTest.java",
                layout.basePackage, "ArchitectureTest.java", JAVA, null, true);

        return ts;
    }
    protected boolean isGenerateEntity(Map entity) {
        boolean skip = JSONPath.get(entity, "options.skip", false);
        String entityName = (String) entity.get("name");
        return !skip && (entities.isEmpty() || entities.contains(entityName));
    }

    @Override
    public Map<String, Object> asConfigurationMap() {
        var config = super.asConfigurationMap();
        config.put("idJavaType", getIdJavaType());
//        config.put("webFlavor", style == ProgrammingStyle.imperative ? WebFlavorType.mvc : WebFlavorType.webflux);
        return config;
    }

    public String getIdJavaType() {
        return ObjectUtils.firstNonNull(idJavaType, this.persistence == PersistenceType.jpa ? "Long" : "String");
    }

}
