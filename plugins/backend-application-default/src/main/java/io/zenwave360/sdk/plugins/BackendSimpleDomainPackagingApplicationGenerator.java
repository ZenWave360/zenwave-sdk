package io.zenwave360.sdk.plugins;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.AbstractZDLProjectGenerator;
import io.zenwave360.sdk.generators.ZDLProjectTemplates;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.ZDLFindUtils;

/**
 * Generates a backend application with the following structure:
 <pre>
📦 basePackage
   └─ 📦 config
   └─ 📦 model (entities and aggregates)
   └─ 📦 dtos
   └─ 📦 events
   ├─ 📦 mappers
   ├─ *EventListeners (spring-cloud-streams)
   ├─ *RestControllers (spring mvc)
   ├─ ServiceImplementation
   └─ *RepositoryInterface
</pre>
 */
public class BackendSimpleDomainPackagingApplicationGenerator extends BackendDefaultApplicationGenerator {


    {
        configPackage = "{{basePackage}}.config";
        entitiesPackage = "{{basePackage}}.model";
        domainEventsPackage = "{{basePackage}}";
        inboundPackage = "{{basePackage}}";
        inboundDtosPackage = "{{basePackage}}.dtos";
        outboundPackage = "{{basePackage}}";
        outboundRepositoryPackage = "{{basePackage}}";
        coreImplementationPackage = "{{basePackage}}";
        infrastructurePackage = "{{basePackage}}";
        infrastructureRepositoryPackage = "{{basePackage}}";
        adaptersPackage = "{{basePackage}}";

        outboundEventsModelPackage = "{{basePackage}}.events.dtos";
        outboundEventsPackage = "{{basePackage}}.events";
    }

    @Override
    protected ZDLProjectTemplates configureProjectTemplates() {
        var ts = new ZDLProjectTemplates("io/zenwave360/sdk/plugins/BackendApplicationDefaultGenerator");

        ts.addTemplate(ts.aggregateTemplates, "src/main/java","core/domain/common/Aggregate.java",
                "{{asPackageFolder entitiesPackage}}/{{aggregate.name}}.java", JAVA, null, true);
        ts.addTemplate(ts.domainEventsTemplates, "src/main/java","core/domain/common/DomainEvent.java",
                "{{asPackageFolder domainEventsPackage}}/{{event.name}}.java", JAVA, null, true);

        ts.addTemplate(ts.entityTemplates, "src/main/java","core/domain/{{persistence}}/Entity.java",
                "{{asPackageFolder entitiesPackage}}/{{entity.name}}.java", JAVA, skipEntity, false);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/outbound/{{persistence}}/{{style}}/EntityRepository.java",
                "{{asPackageFolder outboundRepositoryPackage}}/{{entity.className}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/main/java","core/inbound/dtos/EntityInput.java",
                "{{asPackageFolder inboundDtosPackage}}/{{entity.className}}{{inputDTOSuffix entity}}.java", JAVA, skipEntityInput, false);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/BaseRepositoryIntegrationTest.java",
                "{{asPackageFolder infrastructurePackage}}/BaseRepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/EntityRepositoryIntegrationTest.java",
                "{{asPackageFolder infrastructureRepositoryPackage}}/{{entity.className}}RepositoryIntegrationTest.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/EntityRepositoryInMemory.java",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/{{entity.className}}RepositoryInMemory.java", JAVA, skipEntityRepository, true);
        ts.addTemplate(ts.entityTemplates, "src/test/java","infrastructure/{{persistence}}/{{style}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java",
                "{{asPackageFolder infrastructureRepositoryPackage}}/inmemory/InMemory{{capitalizeFirst persistence}}Repository.java", JAVA, skipEntityRepository, true);

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
        ts.addTemplate(ts.serviceTemplates, "src/main/java", "core/implementation/{{style}}/ServiceImpl.java",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Impl.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/implementation/mappers/BaseMapper.java",
                "{{asPackageFolder coreImplementationPackage}}/mappers/BaseMapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/main/java","core/implementation/mappers/ServiceMapper.java",
                "{{asPackageFolder coreImplementationPackage}}/mappers/{{service.name}}Mapper.java", JAVA, null, true);
        ts.addTemplate(ts.serviceTemplates, "src/test/java", "core/implementation/{{persistence}}/{{style}}/ServiceTest.java",
                "{{asPackageFolder coreImplementationPackage}}/{{service.name}}Test.java", JAVA, null, true);

        ts.addTemplate(ts.allServicesTemplates, "src/main/java", "core/implementation/mappers/EventsMapper.java",
                "{{asPackageFolder coreImplementationPackage}}/mappers/EventsMapper.java", JAVA, skipEvents, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/RepositoriesInMemoryConfig.java",
                "{{asPackageFolder configPackage}}/RepositoriesInMemoryConfig.java", JAVA, null, true);
        ts.addTemplate(ts.allServicesTemplates, "src/test/java", "config/ServicesInMemoryConfig.java",
                "{{asPackageFolder configPackage}}/ServicesInMemoryConfig.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/TestDataLoader-{{persistence}}.java",
                "{{asPackageFolder configPackage}}/TestDataLoader.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "config/DockerComposeInitializer-{{persistence}}.java",
                "{{asPackageFolder configPackage}}/DockerComposeInitializer.java", JAVA, null, true);

        ts.addTemplate(ts.singleTemplates, "src/main/java", "core/inbound/dtos/package-info.java",
                "{{asPackageFolder inboundDtosPackage}}/package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/main/java", "infrastructure/package-info.java",
                "{{asPackageFolder infrastructurePackage}}/package-info.java", JAVA, null, true);
        ts.addTemplate(ts.singleTemplates, "src/test/java", "ArchitectureTest.java",
                "{{asPackageFolder basePackage}}/ArchitectureTest.java", JAVA, null, true);

        return ts;
    }

}
