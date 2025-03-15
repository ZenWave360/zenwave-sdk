package io.zenwave360.sdk.zdl.layouts;

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
        outboundEventsPackage = "{{moduleBasePackage}}.events";
        // asyncapi events dtos
        outboundEventsModelPackage = "{{moduleBasePackage}}.events.dtos";

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
        adaptersWebPackage = "{{adaptersPackage}}";
        adaptersWebMappersPackage = "{{adaptersWebPackage}}.mappers";
        adaptersCommandsPackage = "{{adaptersPackage}}";
        adaptersCommandsMappersPackage = "{{adaptersCommandsPackage}}.mappers";
        adaptersEventsPackage = "{{adaptersPackage}}";
        adaptersEventsMappersPackage = "{{adaptersEventsPackage}}.mappers";

        // openapi generated packages
        openApiApiPackage = "{{adaptersWebPackage}}";
        openApiModelPackage = "{{adaptersWebPackage}}.dtos";
        // asyncapi generated packages (not in use yet)
        asyncApiModelPackage = "{{moduleBasePackage}}.events.dtos"; // right now is outboundEventsModelPackage
        asyncApiProducerApiPackage = "{{moduleBasePackage}}.events"; // right now is outboundEventsPackage
        asyncApiConsumerApiPackage = "{{moduleBasePackage}}.commands"; // right now is adaptersCommandsPackage

        // common packages (for base classes in monolithic projects)
        entitiesCommonPackage = "{{commonPackage}}.domain";
        domainEventsCommonPackage = "{{commonPackage}}.domain.events";
        coreImplementationCommonPackage = "{{commonPackage}}";
        coreImplementationMappersCommonPackage = "{{commonPackage}}.mappers";
        infrastructureRepositoryCommonPackage = "{{commonPackage}}";
        infrastructureEventsCommonPackage = "{{commonPackage}}";
        adaptersWebCommonPackage = "{{commonPackage}}";
        adaptersWebMappersCommonPackage = "{{commonPackage}}.mappers";
        adaptersCommandsCommonPackage = "{{commonPackage}}.commands";
        adaptersCommandsMappersCommonPackage = "{{commonPackage}}.commands.mappers";
        adaptersEventsCommonPackage = "{{commonPackage}}.events";
        adaptersEventsMappersCommonPackage = "{{commonPackage}}.events.mappers";
    }

}
