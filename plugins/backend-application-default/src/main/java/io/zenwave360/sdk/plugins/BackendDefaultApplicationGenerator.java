package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLProjectGenerator;
import io.zenwave360.sdk.generators.ZDLProjectTemplates;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.JSONPath;

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
public class BackendDefaultApplicationGenerator extends AbstractZDLProjectGenerator {

    public String configPackage = "{{basePackage}}.config";
    public String entitiesPackage = "{{basePackage}}.core.domain";
    public String inboundPackage = "{{basePackage}}.core.inbound";
    public String inboundDtosPackage = "{{basePackage}}.core.inbound.dtos";
    public String outboundPackage = "{{basePackage}}.core.outbound";
    public String coreImplementationPackage = "{{basePackage}}.core.implementation";
    public String infrastructurePackage = "{{basePackage}}.infrastructure";
    public String adaptersPackage = "{{basePackage}}.adapters";

    public String outboundEventsModelPackage = "{{basePackage}}.core.domain.events";
    public String outboundEventsPackage = "{{basePackage}}.core.outbound.events";


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

    @DocumentedOption(description = "Whether to add IEntityEventProducer interfaces as service dependencies. Depends on the naming convention of zenwave-asyncapi plugin to work.")
    public boolean includeEventsProducerDependencies = true;


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

    protected Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> !is(model, "aggregate");
    protected Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo", "input", "isSuperClass");
    protected Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo", "input");
    protected Function<Map<String, Object>, Boolean> skipEntityInput = (model) -> inputDTOSuffix == null || inputDTOSuffix.isEmpty();

    protected Function<Map<String, Object>, Boolean> skipInput = (model) -> is(model, "inline");
    @Override
    protected ZDLProjectTemplates configureProjectTemplates() {
        var ts = new ZDLProjectTemplates("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        ts.addTemplate(ts.entityTemplates, "src/main/java","core/domain/{{persistence}}/Entity.java",
                "{{asPackageFolder entitiesPackage}}/{{entity.name}}.java", JAVA, skipEntity, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/outbound/{{persistence}}/{{style}}/EntityRepository.java",
                "{{asPackageFolder outboundPackage}}/{{persistence}}/{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/inbound/dtos/EntityInput.java",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/implementation/mappers/EntityMapper.java",
                "{{asPackageFolder coreImplementationPackage}}/mappers/{{entity.className}}Mapper.java", JAVA, skipEntity, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java",
                "{{asPackageFolder infrastructurePackage}}/{{persistence}}/BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java",
                "{{asPackageFolder infrastructurePackage}}/{{persistence}}/{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                "{{asPackageFolder infrastructurePackage}}/{{persistence}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java",
                "{{asPackageFolder infrastructurePackage}}/{{persistence}}/inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                "{{asPackageFolder infrastructurePackage}}/{{persistence}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        ts.addTemplate(ts.enumTemplates, "src/main/java", "core/domain/common/Enum.java",
                "{{asPackageFolder entitiesPackage}}/{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.inputEnumTemplates, "src/main/java", "core/inbound/dtos/Enum.java",
                "{{asPackageFolder inboundDtosPackage}}/{{enum.name}}.java", JAVA, skipInput, false);

        ts.addTemplate(ts.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}.java", JAVA, skipInput, false);
        ts.addTemplate(ts.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}.java", JAVA, null, false);

        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/inbound/Service.java",
                "{{asPackageFolder inboundPackage}}/{{service.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/{{persistence}}/{{style}}/ServiceImpl.java",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Impl.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Test.java", JAVA, null, true);

        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java",
                "{{asPackageFolder configPackage}}/RepositoriesInMemoryConfig.java", JAVA, null, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java",
                "{{asPackageFolder configPackage}}/ServicesInMemoryConfig.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java",
                "{{asPackageFolder inboundDtosPackage}}/package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "infrastructure/package-info.java",
                "{{asPackageFolder infrastructurePackage}}/package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "ArchitectureTest.java",
                "{{asPackageFolder basePackage}}/ArchitectureTest.java", JAVA, null, true);

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
        return this.persistence == PersistenceType.jpa ? "Long" : "String";
    }

}
