package io.zenwave360.sdk.zdl.layouts;

/**
 * Simple domain project layout.
 *
 * <pre>
 * 📦 {{basePackage}}                            # Root package
 *    📦 config                                  # Spring Boot configuration, security, etc.
 *    📦 domain                                  # Domain Layer (Business Entities and Events)
 *        ├─ *Entities
 *        └─ events/
 *           └─ *DomainEvents
 *    📦 repository                              # Repository Layer (Persistence and Data Access)
 *        ├─ {{persistence}}/
 *        |   ├─ *RepositoryInterface            # Persistence interface (Spring Data, etc.)
 *        |   └─ *RepositoryImpl                 # Repository implementation
 *    📦 events                                  # Events Layer (Internal and Async API Events)
 *        ├─ *EventListeners                     # Event listeners
 *    📦 commands                                # Command Layer (Command Handlers)
 *        ├─ *CommandHandlers                   # Command handlers (e.g., CQRS commands)
 *    📦 service                                 # Service Layer (Business Logic and DTOs)
 *        ├─ dtos/
 *        |   └─ *DTOs                           # Data Transfer Objects
 *        ├─ impl/
 *        |   └─ *ServiceImplementation          # Service implementations
 *        └─ impl/mappers/
 *            └─ *Mappers                        # Object mappers for transformations
 *    📦 web                                     # Web Layer (Controllers and API)
 *        ├─ *RestControllers                   # REST controllers (Spring MVC, etc.)
 *        └─ mappers/
 *           └─ *WebMappers                     # Mappers for web layer transformations
 * </pre>
 */
public class LayeredProjectLayout extends ProjectLayout {

    {
        basePackage = "{{basePackage}}";
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}"; // set to "{{basePackage}}.common" in modular projects
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
        // outbound / secondary ports for events (internal and asyncapi)
        outboundEventsPackage = "{{moduleBasePackage}}.events";
        // asyncapi events dtos
        outboundEventsModelPackage = "{{moduleBasePackage}}.events.dtos";

        // core implementation / inner ring
        coreImplementationPackage = "{{moduleBasePackage}}.service.impl";
        coreImplementationMappersPackage = "{{moduleBasePackage}}.service.impl.mappers";

        // infrastructure / secondary adapters
        infrastructurePackage = "{{moduleBasePackage}}.repository";
        infrastructureRepositoryPackage = "{{moduleBasePackage}}.repository.{{persistence}}";
        // infrastructure / secondary adapters for events
        infrastructureEventsPackage = "{{moduleBasePackage}}.events";

        // primary adapters (web, events, commands)
        adaptersPackage = "{{moduleBasePackage}}";
        adaptersWebPackage = "{{adaptersPackage}}.web";
        adaptersWebMappersPackage = "{{adaptersWebPackage}}.mappers";
        adaptersCommandsPackage = "{{adaptersPackage}}.commands";
        adaptersCommandsMappersPackage = "{{adaptersCommandsPackage}}.mappers";
        adaptersEventsPackage = "{{adaptersPackage}}.events{{#if apiId}}.{{apiId}}{{/if}}";
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
        coreImplementationCommonPackage = "{{commonPackage}}.service.impl";
        coreImplementationMappersCommonPackage = "{{commonPackage}}.service.impl.mappers";
        infrastructureRepositoryCommonPackage = "{{commonPackage}}.repository.{{persistence}}";
        infrastructureEventsCommonPackage = "{{commonPackage}}.events";
        adaptersWebCommonPackage = "{{commonPackage}}.web";
        adaptersWebMappersCommonPackage = "{{commonPackage}}.web.mappers";
        adaptersCommandsCommonPackage = "{{commonPackage}}.commands";
        adaptersCommandsMappersCommonPackage = "{{commonPackage}}.commands.mappers";
        adaptersEventsCommonPackage = "{{commonPackage}}.events";
        adaptersEventsMappersCommonPackage = "{{commonPackage}}.events.mappers";
    }

}
