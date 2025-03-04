package io.zenwave360.sdk.zdl.layout;

public class DefaultProjectLayout extends ProjectLayout {

    {
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}.common";
        modulesPackage = "{{basePackage}}.modules";

        // module specific
        moduleBasePackage = "{{basePackage}}";
        moduleConfigPackage = "{{moduleBasePackage}}.config";

        // domain entities and events
        entitiesPackage = "{{moduleBasePackage}}.core.domain";
        domainEventsPackage = "{{moduleBasePackage}}.core.domain.events";

        // inbound services / primary ports
        inboundPackage = "{{moduleBasePackage}}.core.inbound";
        inboundDtosPackage = "{{moduleBasePackage}}.core.inbound.dtos";

        // outbound / secondary ports
        outboundPackage = "{{moduleBasePackage}}.core.outbound";
        outboundRepositoryPackage = "{{moduleBasePackage}}.core.outbound.{{persistence}}";
        // outbound / secondary ports for events
        outboundEventsPackage = "{{moduleBasePackage}}.core.outbound.events";
        outboundEventsModelPackage = "{{moduleBasePackage}}.core.outbound.events.dtos";

        // core implementation / inner ring
        coreImplementationPackage = "{{moduleBasePackage}}.core.implementation";
        coreImplementationMappersPackage = "{{moduleBasePackage}}.core.implementation.mappers";

        // infrastructure / secondary adapters
        infrastructurePackage = "{{moduleBasePackage}}.infrastructure";
        infrastructureRepositoryPackage = "{{moduleBasePackage}}.infrastructure.{{persistence}}";
        // infrastructure / secondary adapters for events
        infrastructureEventsPackage = "{{moduleBasePackage}}.infrastructure.events";

        // primary adapters (web, events, commands)
        adaptersPackage = "{{moduleBasePackage}}.adapters";
        adaptersWebPackage = "{{moduleBasePackage}}.adapters.web";
        adaptersWebMappersPackage = "{{moduleBasePackage}}.adapters.web.mappers";
        adaptersCommandsPackage = "{{moduleBasePackage}}.adapters.commands";
        adaptersCommandsMappersPackage = "{{moduleBasePackage}}.adapters.commands.mappers";
        adaptersEventsPackage = "{{moduleBasePackage}}.adapters.events";
        adaptersEventsMappersPackage = "{{moduleBasePackage}}.adapters.events.mappers";
    }

}
