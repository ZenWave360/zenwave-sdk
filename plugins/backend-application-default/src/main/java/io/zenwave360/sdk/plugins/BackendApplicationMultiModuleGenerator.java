package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class BackendApplicationMultiModuleGenerator extends BackendApplicationDefaultGenerator {

    public String mavenModulesPrefix;
    @Override
    protected ProjectTemplates configureProjectTemplates() {
        var ts = new ProjectTemplates("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        var layout = new ProjectLayout(); // layoutNames

        ts.addTemplate(ts.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java", "{{mavenModulesPrefix}}-domain",
                layout.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        ts.addTemplate(ts.aggregateTemplates, "src/main/java", "core/domain/common/Aggregate.java", "{{mavenModulesPrefix}}-domain",
                layout.entitiesPackage, "{{aggregate.name}}.java", JAVA, null, true);
        ts.addTemplate(ts.domainEventsTemplates, "src/main/java", "core/domain/common/DomainEvent.java", "{{mavenModulesPrefix}}-domain",
                layout.domainEventsPackage, "{{event.name}}.java", JAVA, null, true);

        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/domain/{{persistence}}/Entity.java", "{{mavenModulesPrefix}}-domain",
                layout.entitiesPackage, "{{entity.name}}.java", JAVA, skipEntity, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "{{mavenModulesPrefix}}-domain",
                layout.outboundRepositoryPackage, "{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/main/java", "core/inbound/dtos/EntityInput.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundDtosPackage, "{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                layout.infrastructureRepositoryCommonPackage, "BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                layout.infrastructureRepositoryPackage, "{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                layout.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java", "{{mavenModulesPrefix}}-core-impl",
                layout.infrastructureRepositoryPackage, "inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java", "infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                layout.infrastructureRepositoryPackage, "inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        ts.addTemplate(ts.enumTemplates, "src/main/java", "core/domain/common/DomainEnum.java", "{{mavenModulesPrefix}}-domain",
                layout.entitiesPackage, "{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.inputEnumTemplates, "src/main/java", "core/domain/common/InputEnum.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundDtosPackage, "{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.eventEnumTemplates, "src/main/java", "core/domain/common/EventEnum.java", "{{mavenModulesPrefix}}-domain",
                layout.domainEventsPackage, "{{enum.name}}.java", JAVA, skipInput, false);

        ts.addTemplate(ts.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundDtosPackage, "{{entity.className}}.java", JAVA, skipInput, false);
        ts.addTemplate(ts.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundDtosPackage, "{{entity.className}}.java", JAVA, null, false);

        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/inbound/Service.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundPackage, "{{service.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "{{mavenModulesPrefix}}-core-impl",
                layout.coreImplementationPackage, "{{service.name}}Impl.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java", "{{mavenModulesPrefix}}-domain",
                layout.coreImplementationMappersCommonPackage, "BaseMapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/mappers/ServiceMapper.java", "{{mavenModulesPrefix}}-core-impl",
                layout.coreImplementationMappersPackage, "{{service.name}}Mapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java", "{{mavenModulesPrefix}}-core-impl",
                layout.coreImplementationPackage, "{{service.name}}Test.java", JAVA, null, true);

        ts.addTemplate(ts.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java", "{{mavenModulesPrefix}}-core-impl",
                layout.coreImplementationMappersPackage, "EventsMapper.java", JAVA, skipEvents, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                layout.configPackage, "RepositoriesInMemoryConfig.java", JAVA, null, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                layout.configPackage, "ServicesInMemoryConfig.java", JAVA, null, true);

        ts.addTemplate(ts.allEventsTemplates, "src/main/java", "core/outbound/events/EventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layout.outboundEventsPackage, "EventPublisher.java", JAVA, skipEventsBus, false);
        ts.addTemplate(ts.allEventsTemplates, "src/main/java", "infrastructure/events/DefaultEventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layout.infrastructureEventsPackage, "DefaultEventPublisher.java", JAVA, skipEventsBus, false);
        ts.addTemplate(ts.allEventsTemplates, "src/test/java", "infrastructure/events/InMemoryEventPublisher.java", "{{mavenModulesPrefix}}-core-impl",
                layout.infrastructureEventsPackage, "InMemoryEventPublisher.java", JAVA, skipEventsBus, false);

        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                layout.configPackage, "TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                layout.configPackage, "DockerComposeInitializer.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                layout.configPackage, "TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                layout.configPackage, "DockerComposeInitializer.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java", "{{mavenModulesPrefix}}-domain",
                layout.inboundDtosPackage, "package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "infrastructure/package-info.java", "{{mavenModulesPrefix}}-infra",
                layout.infrastructurePackage, "package-info.java", JAVA, null, true);
//        ts.addTemplate(ts.singleTemplates, "src/test/java", "ArchitectureTest.java",
//                "{{asPackageFolder layout.basePackage}}/ArchitectureTest.java", JAVA, null, true);

        return ts;
    }

}
