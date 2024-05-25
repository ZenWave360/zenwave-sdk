package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.generators.ZDLProjectTemplates;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class BackendMultiModuleApplicationGenerator extends BackendDefaultApplicationGenerator {

    public String mavenModulesPrefix;
    @Override
    protected ZDLProjectTemplates configureProjectTemplates() {
        var ts = new ZDLProjectTemplates("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        ts.addTemplate(ts.aggregateTemplates, "src/main/java","core/domain/common/Aggregate.java","{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder entitiesPackage}}/{{aggregate.name}}.java", JAVA, null, true);
        ts.addTemplate(ts.domainEventsTemplates, "src/main/java","core/domain/common/DomainEvent.java","{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder domainEventsPackage}}/{{event.name}}.java", JAVA, null, true);

        ts.addTemplate(ts.entityTemplates, "src/main/java","core/domain/{{persistence}}/Entity.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder entitiesPackage}}/{{entity.name}}.java", JAVA, skipEntity, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/outbound/{{persistence}}/{{style}}/EntityRepository.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder outboundRepositoryPackage}}/{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/inbound/dtos/EntityInput.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                "{{asPackageFolder infrastructureRepositoryPackage}}/BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java", "{{mavenModulesPrefix}}-infra",
                "{{asPackageFolder infrastructureRepositoryPackage}}/{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

        ts.addTemplate(ts.enumTemplates, "src/main/java", "core/domain/common/Enum.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder entitiesPackage}}/{{enum.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.inputEnumTemplates, "src/main/java", "core/inbound/dtos/Enum.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundDtosPackage}}/{{enum.name}}.java", JAVA, null, false);

        ts.addTemplate(ts.inputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}.java", JAVA, skipInput, false);
        ts.addTemplate(ts.outputTemplates, "src/main/java", "core/inbound/dtos/InputOrOutput.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}.java", JAVA, null, false);

        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/inbound/Service.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundPackage}}/{{service.name}}.java", JAVA, null, false);
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Impl.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder coreImplementationPackage}}/mappers/BaseMapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/main/java","core/implementation/mappers/ServiceMapper.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder coreImplementationPackage}}/mappers/{{service.name}}Mapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Test.java", JAVA, null, true);

        ts.addTemplate(ts.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java","{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder coreImplementationPackage}}/mappers/EventsMapper.java", JAVA, skipEvents, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder configPackage}}/RepositoriesInMemoryConfig.java", JAVA, null, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder configPackage}}/ServicesInMemoryConfig.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder configPackage}}/TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-core-impl",
                "{{asPackageFolder configPackage}}/DockerComposeInitializer.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                "{{asPackageFolder configPackage}}/TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java", "{{mavenModulesPrefix}}-infra",
                "{{asPackageFolder configPackage}}/DockerComposeInitializer.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java", "{{mavenModulesPrefix}}-domain",
                "{{asPackageFolder inboundDtosPackage}}/package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "infrastructure/package-info.java", "{{mavenModulesPrefix}}-infra",
                "{{asPackageFolder infrastructurePackage}}/package-info.java", JAVA, null, true);
//        ts.addTemplate(ts.singleTemplates, "src/test/java", "ArchitectureTest.java",
//                "{{asPackageFolder basePackage}}/ArchitectureTest.java", JAVA, null, true);

        return ts;
    }

}
