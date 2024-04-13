package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class BackendApplicationMultiModuleProjectTemplates extends BackendApplicationProjectTemplates {

    public String mavenModulesPrefix;

    public BackendApplicationMultiModuleProjectTemplates() {
        var layoutNames = new ProjectLayout(); // layoutNames

        this.addTemplate(this.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        this.addTemplate(this.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        this.addTemplate(this.domainEventsTemplates, "src/main/java", "core/domain/common/DomainEvent.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.domainEventsPackage, "{{event.name}}.java", JAVA, null, true);

        this.addTemplate(this.entityTemplates, "src/main/java", "core/domain/{{persistence}}/Entity.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.entitiesPackage, "{{entity.name}}.java", JAVA, skipEntity, false);
        this.addTemplate(this.entityTemplates, "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.outboundRepositoryPackage, "{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
//        this.addTemplate(this.entityTemplates, "src/main/java", "core/inbound/dtos/EntityInput.java", "{{mavenModulesPrefix}}-domain",
//                layout.inboundDtosPackage, "{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                layoutNames.infrastructureRepositoryCommonPackage, "BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                layoutNames.infrastructureRepositoryPackage, "{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.infrastructureRepositoryPackage, "inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        this.addTemplate(this.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        this.addTemplate(this.enumTemplates, "src/main/java", "core/domain/common/DomainEnum.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.entitiesPackage, "{{enum.name}}.java", JAVA, null, false);
        this.addTemplate(this.inputEnumTemplates, "src/main/java", "core/domain/common/InputEnum.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.inboundDtosPackage, "{{enum.name}}.java", JAVA, null, false);
        this.addTemplate(this.eventEnumTemplates, "src/main/java", "core/domain/common/EventEnum.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.domainEventsPackage, "{{enum.name}}.java", JAVA, skipInput, false);

        this.addTemplate(this.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.inboundDtosPackage, "{{entity.className}}.java", JAVA, skipInput, false);
        this.addTemplate(this.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.inboundDtosPackage, "{{entity.className}}.java", JAVA, null, false);

        this.addTemplate(this.serviceTemplates, "src/main/java", "core/inbound/Service.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.inboundPackage, "{{service.name}}.java", JAVA, null, false);
        this.addTemplate(this.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.coreImplementationPackage, "{{service.name}}Impl.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.coreImplementationMappersCommonPackage, "BaseMapper.java", JAVA, null, true);
        this.addTemplate(this.serviceTemplates, "src/main/java", "core/implementation/mappers/ServiceMapper.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.coreImplementationMappersPackage, "{{service.name}}Mapper.java", JAVA, null, true);
        this.addTemplate(this.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.coreImplementationPackage, "{{service.name}}Test.java", JAVA, null, true);

        this.addTemplate(this.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.coreImplementationMappersPackage, "EventsMapper.java", JAVA, skipEvents, true);
        this.addTemplate(this.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.moduleConfigPackage, "RepositoriesInMemoryConfig.java", JAVA, null, true);
        this.addTemplate(this.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.moduleConfigPackage, "ServicesInMemoryConfig.java", JAVA, null, true);

        this.addTemplate(this.allDomainEventsTemplates, "src/main/java", "core/outbound/events/EventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.outboundEventsPackage, "EventPublisher.java", JAVA, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/main/java", "infrastructure/events/DefaultEventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.infrastructureEventsPackage, "DefaultEventPublisher.java", JAVA, skipEventsBus, false);
        this.addTemplate(this.allDomainEventsTemplates, "src/test/java", "infrastructure/events/InMemoryEventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.infrastructureEventsPackage, "InMemoryEventPublisher.java", JAVA, skipEventsBus, false);

        this.addTemplate(this.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.moduleConfigPackage, "TestDataLoader.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                layoutNames.configPackage, "DockerComposeInitializer.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                layoutNames.moduleConfigPackage, "TestDataLoader.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                layoutNames.configPackage, "DockerComposeInitializer.java", JAVA, null, true);

        this.addTemplate(this.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java", "{{mavenModulesPrefix}}-domain",
                layoutNames.inboundDtosPackage, "package-info.java", JAVA, null, true);
        this.addTemplate(this.singleTemplates, "src/main/java", "infrastructure/package-info.java", "{{mavenModulesPrefix}}-infra",
                layoutNames.infrastructurePackage, "package-info.java", JAVA, null, true);
        //        this.addTemplate(this.singleTemplates, "src/test/java", "ArchitectureTest.java",
        //                "{{asPackageFolder layout.basePackage}}/ArchitectureTest.java", JAVA, null, true);
    }
}
