package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.CleanArchitectureProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;
import static io.zenwave360.sdk.zdl.utils.ZDLFindUtils.is;

public class BackendApplicationProjectTemplates extends ProjectTemplates {

    public boolean includeEmitEventsImplementation = true;

    protected Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> is(model, "persistence") // if polyglot persistence -> skip
            || !(is(model, "aggregate") || ZDLFindUtils.isAggregateRoot(JSONPath.get(model, "zdl"), JSONPath.get(model, "$.entity.name")));
    protected Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo", "input", "abstract");
    protected Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo", "input");
//    protected Function<Map<String, Object>, Boolean> skipEntityInput = (model) -> inputDTOSuffix == null || inputDTOSuffix.isEmpty();

    protected Function<Map<String, Object>, Boolean> skipEvents = (model) -> !includeEmitEventsImplementation;
    protected Function<Map<String, Object>, Boolean> skipEventsBus = (model) -> ((Collection) model.get("events")).isEmpty();
    protected Function<Map<String, Object>, Boolean> skipInput = (model) -> is(model, "inline");

    public BackendApplicationProjectTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        var layoutNames = new ProjectLayout(); // layoutNames

        this.addTemplate(this.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java",
                layoutNames.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        this.addTemplate(this.domainEventsTemplates, "src/main/java", "core/domain/common/DomainEvent.java",
                layoutNames.domainEventsPackage, "{{event.name}}.java", JAVA, null, true);

        this.addTemplate(this.entityTemplates, "src/main/java", "core/domain/{{persistence}}/Entity.java",
                layoutNames.entitiesPackage, "{{entity.name}}.java", JAVA, skipEntity, false);
        this.addTemplate(this.entityTemplates, "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java",
                layoutNames.outboundRepositoryPackage, "{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
//        this.addTemplate(this.entityTemplates, "src/main/java", "core/inbound/dtos/EntityInput.java",
//                layout.inboundDtosPackage, "{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java",
                layoutNames.infrastructureRepositoryCommonPackage, "BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java",
                layoutNames.infrastructureRepositoryPackage, "{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java",
                layoutNames.infrastructureRepositoryPackage, "inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        this.addTemplate(this.enumTemplates, "src/main/java", "core/domain/common/DomainEnum.java",
                layoutNames.entitiesPackage, "{{enum.name}}.java", JAVA, null, false);
        this.addTemplate(this.inputEnumTemplates, "src/main/java", "core/domain/common/InputEnum.java",
                layoutNames.inboundDtosPackage, "{{enum.name}}.java", JAVA, null, false);
        this.addTemplate(this.eventEnumTemplates, "src/main/java", "core/domain/common/EventEnum.java",
                layoutNames.domainEventsPackage, "{{enum.name}}.java", JAVA, skipInput, false);

        this.addTemplate(this.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                layoutNames.inboundDtosPackage, "{{entity.className}}.java", JAVA, skipInput, false);
        this.addTemplate(this.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java",
                layoutNames.inboundDtosPackage, "{{entity.className}}.java", JAVA, null, false);

        this.addTemplate(this.serviceTemplates, "src/main/java", "core/inbound/Service.java",
                layoutNames.inboundPackage, "{{service.name}}.java", JAVA, null, false);
        this.addTemplate(this.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java",
                layoutNames.coreImplementationPackage, "{{service.name}}Impl.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java",
                layoutNames.coreImplementationMappersCommonPackage, "BaseMapper.java", JAVA, null, true);
        this.addTemplate(this.serviceTemplates, "src/main/java", "core/implementation/mappers/ServiceMapper.java",
                layoutNames.coreImplementationMappersPackage, "{{service.name}}Mapper.java", JAVA, null, true);
        this.addTemplate(this.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java",
                layoutNames.coreImplementationPackage, "{{service.name}}Test.java", JAVA, null, true);

        this.addTemplate(this.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java",
                layoutNames.coreImplementationMappersPackage, "EventsMapper.java", JAVA, skipEvents, true);
        this.addTemplate(this.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java",
                layoutNames.moduleConfigPackage, "RepositoriesInMemoryConfig.java", JAVA, null, true);
        this.addTemplate(this.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java",
                layoutNames.moduleConfigPackage, "ServicesInMemoryConfig.java", JAVA, null, true);

        this.addTemplate(this.allDomainEventsTemplates, "src/main/java", "core/outbound/events/EventPublisher.java",
                layoutNames.outboundEventsPackage, "EventPublisher.java", JAVA, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/main/java", "infrastructure/events/DefaultEventPublisher.java",
                layoutNames.infrastructureEventsPackage, "DefaultEventPublisher.java", JAVA, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/test/java", "infrastructure/events/InMemoryEventPublisher.java",
                layoutNames.infrastructureEventsPackage, "InMemoryEventPublisher.java", JAVA, skipEventsBus, false);

        this.addTemplate(this.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java",
                layoutNames.moduleConfigPackage, "TestDataLoader.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java",
                layoutNames.configPackage, "DockerComposeInitializer.java", JAVA, null, true);

        this.addTemplate(this.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java",
                layoutNames.inboundDtosPackage, "package-info.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/main/java", "infrastructure/package-info.java",
                layoutNames.infrastructurePackage, "package-info.java", JAVA, null, true);

        if(this.layout instanceof CleanArchitectureProjectLayout) {
            this.addTemplate(this.singleTemplates, "src/test/java", "ArchitectureTest.java",
                    layoutNames.moduleBasePackage, "ArchitectureTest.java", JAVA, null, true);
        }
    }
}
