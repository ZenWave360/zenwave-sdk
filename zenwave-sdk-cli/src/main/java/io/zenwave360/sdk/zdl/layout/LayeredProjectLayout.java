package io.zenwave360.sdk.zdl.layout;

public class LayeredProjectLayout extends ProjectLayout {

    {
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}.common";
        modulesPackage = "{{basePackage}}.modules";

        // module specific
        moduleBasePackage = "{{basePackage}}";
        moduleConfigPackage = "{{moduleBasePackage}}.config";

        // domain entities and events
        entitiesPackage = "{{moduleBasePackage}}.domain";
        domainEventsPackage = "{{moduleBasePackage}}.domain.events";

        // inbound services / primary ports
        inboundPackage = "{{moduleBasePackage}}.service";
        inboundDtosPackage = "{{moduleBasePackage}}.service.dtos";

        // outbound / secondary ports
        outboundPackage = "{{moduleBasePackage}}.repository";
        outboundRepositoryPackage = "{{moduleBasePackage}}.repository.{{persistence}}";
        // outbound / secondary ports for events
        outboundEventsPackage = "{{moduleBasePackage}}.events";
        outboundEventsModelPackage = "{{moduleBasePackage}}.events.dtos";

        // core implementation / inner ring
        coreImplementationPackage = "{{moduleBasePackage}}.service.impl";
        coreImplementationMappersPackage = "{{moduleBasePackage}}.service.impl.mappers";

        // infrastructure / secondary adapters
        infrastructurePackage = "{{moduleBasePackage}}.repository";
        infrastructureRepositoryPackage = "{{moduleBasePackage}}.repository.{{persistence}}";
        // infrastructure / secondary adapters for events
        infrastructureEventsPackage = "{{moduleBasePackage}}.repository.events";

        // primary adapters (web, events, commands)
        adaptersPackage = "{{moduleBasePackage}}";
        adaptersWebPackage = "{{moduleBasePackage}}.web";
        adaptersWebMappersPackage = "{{moduleBasePackage}}.web.mappers";
        adaptersCommandsPackage = "{{moduleBasePackage}}.commands";
        adaptersCommandsMappersPackage = "{{moduleBasePackage}}.commands.mappers";
        adaptersEventsPackage = "{{moduleBasePackage}}.events";
        adaptersEventsMappersPackage = "{{moduleBasePackage}}.events.mappers";
    }

}
