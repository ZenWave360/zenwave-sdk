package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultGenerator;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultHelpers;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultJpaHelpers;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.CleanArchitectureProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.utils.ZDLFindUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.zenwave360.sdk.templating.OutputFormatType.KOTLIN;
import static io.zenwave360.sdk.zdl.utils.ZDLFindUtils.is;

public class BackendApplicationKotlinTemplates extends ProjectTemplates {

    public boolean includeEmitEventsImplementation = true;

    protected Function<Map<String, Object>, Boolean> skipEntityRepository = (model) -> is(model, "persistence") // if polyglot persistence -> skip
            || !(is(model, "aggregate") || ZDLFindUtils.isAggregateRoot(JSONPath.get(model, "zdl"), JSONPath.get(model, "$.entity.name")));
    protected Function<Map<String, Object>, Boolean> skipEntityId = (model) -> is(model, "embedded", "vo", "input", "abstract");
    protected Function<Map<String, Object>, Boolean> skipEntity = (model) -> is(model, "vo", "input");
//    protected Function<Map<String, Object>, Boolean> skipEntityInput = (model) -> inputDTOSuffix == null || inputDTOSuffix.isEmpty();

    protected Function<Map<String, Object>, Boolean> skipEvents = (model) -> !includeEmitEventsImplementation;
    protected Function<Map<String, Object>, Boolean> skipEventsBus = (model) -> ((Collection) model.get("events")).isEmpty();
    protected Function<Map<String, Object>, Boolean> skipInput = (model) -> is(model, "inline");

    @Override
    public List<Object> getTemplateHelpers(Generator generator) {
        var helpers = new ArrayList<>(super.getTemplateHelpers(generator));
        helpers.add(new BackendApplicationDefaultHelpers((BackendApplicationDefaultGenerator) generator));
        helpers.add(new BackendApplicationDefaultJpaHelpers((BackendApplicationDefaultGenerator) generator));
        helpers.add(new BackendApplicationKotlinHelpers((BackendApplicationDefaultGenerator) generator));
        return helpers;
    }

    public BackendApplicationKotlinTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/kotlin/BackendApplicationDefaultGenerator");

        var layoutNames = new ProjectLayout(); // layoutNames

        this.addTemplate(this.aggregateTemplates, "src/main/kotlin", "core/domain/common/Aggregate.kt",
                layoutNames.entitiesPackage, "{{aggregate.name}}.kt", KOTLIN, null, true);
        this.addTemplate(this.domainEventsTemplates, "src/main/kotlin", "core/domain/common/DomainEvent.kt",
                layoutNames.domainEventsPackage, "{{event.name}}.kt", KOTLIN, null, true);

        this.addTemplate(this.entityTemplates, "src/main/kotlin", "core/domain/{{persistence}}/Entity.kt",
                layoutNames.entitiesPackage, "{{entity.name}}.kt", KOTLIN, skipEntity, false);
        this.addTemplate(this.entityTemplates, "src/main/kotlin", "core/outbound/{{persistence}}/{{style}}/EntityRepository.kt",
                layoutNames.outboundRepositoryPackage, "{{entity.className}}Repository.kt", KOTLIN, skipEntityRepository, true);
//        this.addTemplate(this.entityTemplates, "src/main/kotlin", "core/inbound/dtos/EntityInput.kt",
//                layout.inboundDtosPackage, "{{entity.className}}{{inputDTOSuffix entity}}.kt", KOTLIN, skipEntityInput, false);
        this.addTemplate(this.entityTemplates, "src/test/kotlin", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.kt",
                layoutNames.infrastructureRepositoryCommonPackage, "BaseRepositoryIntegrationTest.kt", KOTLIN, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/kotlin", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.kt",
                layoutNames.infrastructureRepositoryPackage, "{{entity.className}}RepositoryIntegrationTest.kt", KOTLIN, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/kotlin", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.kt",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.kt", KOTLIN, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/kotlin", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.kt",
                layoutNames.infrastructureRepositoryPackage, "inmemory/{{entity.className}}RepositoryInMemory.kt", KOTLIN, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/kotlin", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.kt",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.kt", KOTLIN, skipEntityRepository, true);

        this.addTemplate(this.enumTemplates, "src/main/kotlin", "core/domain/common/DomainEnum.kt",
                layoutNames.entitiesPackage, "{{enum.name}}.kt", KOTLIN, null, false);
        this.addTemplate(this.inputEnumTemplates, "src/main/kotlin", "core/domain/common/InputEnum.kt",
                layoutNames.inboundDtosPackage, "{{enum.name}}.kt", KOTLIN, null, false);
        this.addTemplate(this.eventEnumTemplates, "src/main/kotlin", "core/domain/common/EventEnum.kt",
                layoutNames.domainEventsPackage, "{{enum.name}}.kt", KOTLIN, skipInput, false);

        this.addTemplate(this.inputTemplates, "src/main/kotlin", "core/inbound/dtos/InputOrOutput.kt",
                layoutNames.inboundDtosPackage, "{{entity.className}}.kt", KOTLIN, skipInput, false);
        this.addTemplate(this.outputTemplates, "src/main/kotlin", "core/inbound/dtos/InputOrOutput.kt",
                layoutNames.inboundDtosPackage, "{{entity.className}}.kt", KOTLIN, null, false);

        this.addTemplate(this.serviceTemplates, "src/main/kotlin", "core/inbound/Service.kt",
                layoutNames.inboundPackage, "{{service.name}}.kt", KOTLIN, null, false);
        this.addTemplate(this.serviceTemplates, "src/main/kotlin", "core/implementation/{{style}}/ServiceImpl.kt",
                layoutNames.coreImplementationPackage, "{{service.name}}Impl.kt", KOTLIN, null, true);
        this.addTemplate(this.singleTemplates, "src/main/kotlin", "core/implementation/mappers/BaseMapper.kt",
                layoutNames.coreImplementationMappersCommonPackage, "BaseMapper.kt", KOTLIN, null, true);
        this.addTemplate(this.serviceTemplates, "src/main/kotlin", "core/implementation/mappers/ServiceMapper.kt",
                layoutNames.coreImplementationMappersPackage, "{{service.name}}Mapper.kt", KOTLIN, null, true);
        this.addTemplate(this.serviceTemplates, "src/test/kotlin", "core/implementation/{{persistence}}/{{style}}/ServiceTest.kt",
                layoutNames.coreImplementationPackage, "{{service.name}}Test.kt", KOTLIN, null, true);

        this.addTemplate(this.allServicesTemplates, "src/main/kotlin", "core/implementation/mappers/EventsMapper.kt",
                layoutNames.coreImplementationMappersPackage, "EventsMapper.kt", KOTLIN, skipEvents, true);
        this.addTemplate(this.allServicesTemplates, "src/test/kotlin", "config/RepositoriesInMemoryConfig.kt",
                layoutNames.moduleConfigPackage, "RepositoriesInMemoryConfig.kt", KOTLIN, null, true);
        this.addTemplate(this.allServicesTemplates, "src/test/kotlin", "config/ServicesInMemoryConfig.kt",
                layoutNames.moduleConfigPackage, "ServicesInMemoryConfig.kt", KOTLIN, null, true);

        this.addTemplate(this.allDomainEventsTemplates, "src/main/kotlin", "core/outbound/events/EventPublisher.kt",
                layoutNames.outboundEventsPackage, "EventPublisher.kt", KOTLIN, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/main/kotlin", "infrastructure/events/DefaultEventPublisher.kt",
                layoutNames.infrastructureEventsPackage, "DefaultEventPublisher.kt", KOTLIN, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/test/kotlin", "infrastructure/events/InMemoryEventPublisher.kt",
                layoutNames.infrastructureEventsPackage, "InMemoryEventPublisher.kt", KOTLIN, skipEventsBus, false);

        this.addTemplate(this.singleTemplates, "src/test/kotlin", "config/TestDataLoader-{{persistence}}.kt",
                layoutNames.moduleConfigPackage, "TestDataLoader.kt", KOTLIN, null, true);
        this.addTemplate(this.singleTemplates, "src/test/kotlin", "config/DockerComposeInitializer-{{persistence}}.kt",
                layoutNames.configPackage, "DockerComposeInitializer.kt", KOTLIN, null, true);

        this.addTemplate(this.singleTemplates, "src/main/kotlin", "core/inbound/dtos/package-info.kt",
                layoutNames.inboundDtosPackage, "package-info.kt", KOTLIN, null, true);
        this.addTemplate(this.singleTemplates, "src/main/kotlin", "infrastructure/package-info.kt",
                layoutNames.infrastructurePackage, "package-info.kt", KOTLIN, null, true);

        if(this.layout instanceof CleanArchitectureProjectLayout) {
            this.addTemplate(this.singleTemplates, "src/test/kotlin", "ArchitectureTest.kt",
                    layoutNames.moduleBasePackage, "ArchitectureTest.kt", KOTLIN, null, true);
        }
    }
}
