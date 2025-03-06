package io.zenwave360.sdk.zdl.layout;

/**
 * CleanArchitectureProjectLayout project layout.
 *
 * <pre>
 * 📦 {{basePackage}}
 *    📦 domain                        # Core business entities and aggregates (Domain Layer)
 *        └─ *Entities
 *
 *    📦 application                   # Application layer (Use Cases)
 *        ├─ services/
 *        |   └─ *UseCase (service interfaces with input/output models)
 *        └─ dtos/
 *
 *    📦 adapters                      # Interface Adapters
 *        ├─ web                      # Web Adapter (Controllers)
 *        |   └─ RestControllers
 *        ├─ events                   # Event-driven Adapter
 *        |   └─ *EventListeners
 *        └─ persistence              # Persistence Adapter
 *            ├─ mongodb/
 *            |   ├─ MongoRepositoryInterface
 *            |   └─ MongoRepositoryImpl
 *            └─ jpa/
 *                ├─ JpaRepositoryInterface
 *                └─ JpaRepositoryImpl
 *
 *    📦 config                  # Spring Boot configuration, security, etc.
 * </pre>
 */
public class CleanArchitectureProjectLayout extends ProjectLayout {

    {
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}.common";
        modulesPackage = "{{basePackage}}.modules";

        // module specific
        moduleBasePackage = "{{basePackage}}";
        moduleConfigPackage = "{{moduleBasePackage}}.config";

        // domain entities and events
        entitiesPackage = "{{moduleBasePackage}}.domain.entities";
        domainEventsPackage = "{{moduleBasePackage}}.domain.events";

        // inbound services / primary ports (use cases)
        inboundPackage = "{{moduleBasePackage}}.application.usecases";
        inboundDtosPackage = "{{moduleBasePackage}}.application.usecases.dtos";

        // outbound / secondary ports (interfaces)
        outboundPackage = "{{moduleBasePackage}}.application.ports";
        outboundRepositoryPackage = "{{moduleBasePackage}}.application.ports.{{persistence}}";
        // outbound / secondary ports for events (internal and asyncapi)
        outboundEventsPackage = "{{moduleBasePackage}}.application.ports.events";
        // asyncapi events dtos
        outboundEventsModelPackage = "{{moduleBasePackage}}.application.ports.events.dtos";

        // core implementation (use case implementations)
        coreImplementationPackage = "{{moduleBasePackage}}.application.services";
        coreImplementationMappersPackage = "{{moduleBasePackage}}.application.mappers";

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

        // openapi generated packages
        openApiApiPackage = "{{adaptersWebPackage}}";
        openApiModelPackage = "{{adaptersWebPackage}}.dtos";

    }

}
