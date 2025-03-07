package io.zenwave360.sdk.zdl.layout;

/**
 * Simple domain project layout.
 *
 * <pre>
 * 📦 {{basePackage}}
 *    └─ 📦 config
 *    └─ 📦 model (entities and aggregates)
 *    └─ 📦 dtos
 *    └─ 📦 events
 *    ├─ 📦 mappers
 *    ├─ *EventListeners (spring-cloud-streams)
 *    ├─ *RestControllers (spring mvc)
 *    ├─ ServiceImplementation
 *    └─ *RepositoryInterface
 * </pre>
 */
public class SimpleDomainProjectLayout extends ProjectLayout {

    {
        basePackage = "{{basePackage}}";
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
        inboundPackage = "{{moduleBasePackage}}";
        inboundDtosPackage = "{{moduleBasePackage}}.dtos";

        // outbound / secondary ports
        outboundPackage = "{{moduleBasePackage}}";
        outboundRepositoryPackage = "{{moduleBasePackage}}";
        // outbound / secondary ports for events (internal and asyncapi)
        outboundEventsPackage = "{{moduleBasePackage}}";
        // asyncapi events dtos
        outboundEventsModelPackage = "{{moduleBasePackage}}.domain.events";

        // core implementation / inner ring
        coreImplementationPackage = "{{moduleBasePackage}}";
        coreImplementationMappersPackage = "{{moduleBasePackage}}.mappers";

        // infrastructure / secondary adapters
        infrastructurePackage = "{{moduleBasePackage}}";
        infrastructureRepositoryPackage = "{{moduleBasePackage}}";
        // infrastructure / secondary adapters for events
        infrastructureEventsPackage = "{{moduleBasePackage}}";

        // primary adapters (web, events, commands)
        adaptersPackage = "{{moduleBasePackage}}";
        adaptersWebPackage = "{{moduleBasePackage}}";
        adaptersWebMappersPackage = "{{moduleBasePackage}}.mappers";
        adaptersCommandsPackage = "{{moduleBasePackage}}.commands";
        adaptersCommandsMappersPackage = "{{moduleBasePackage}}.commands.mappers";
        adaptersEventsPackage = "{{moduleBasePackage}}.events";
        adaptersEventsMappersPackage = "{{moduleBasePackage}}.events.mappers";

        // openapi generated packages
        openApiApiPackage = "{{adaptersWebPackage}}";
        openApiModelPackage = "{{adaptersWebPackage}}.dtos";
    }

}
