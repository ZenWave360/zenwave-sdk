package io.zenwave360.sdk.zdl.layout;

/**
 * Hexagonal project layout.
 *
 * <pre>
 * 📦 {{basePackage}}
 *    📦 domain                         # Domain model (Entities, Aggregates, Value Objects)
 *        └─ *Entities
 *        |
 *    📦 ports                          # Port interfaces
 *        ├─ inbound                   # Primary ports (driving adapters)
 *        |   └─ UserServicePort       # Interface for business logic (input)
 *        └─ outbound                  # Secondary ports (driven adapters)
 *            └─ UserRepositoryPort    # Interface for persistence (output)
 *        |
 *    📦 application                   # Application core (business logic services)
 *        ├─ services                 # Service implementations
 *        |   └─ UserServiceImpl      # Implements UserServicePort, uses UserRepositoryPort
 *        └─ mappers                 # Optional: Mapping between entities and DTOs
 *        |
 *    📦 adapters                      # Interface adapters (controllers, repositories, listeners)
 *        ├─ web                     # Web adapter (e.g., REST)
 *        |   └─ UserController      # Calls UserServicePort
 *        ├─ persistence             # Persistence adapters
 *        |   ├─ mongodb/
 *        |   |   └─ MongoUserRepository (implements UserRepositoryPort)
 *        |   └─ jpa/
 *        |       └─ JpaUserRepository (implements UserRepositoryPort)
 *        └─ events                  # Event-driven adapters
 *            └─ UserEventListener   # Listens to events, calls UserServicePort
 *        |
 *    📦 config                  # Spring Boot configurations
 * </pre>
 */
public class HexagonalProjectLayout extends ProjectLayout {

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
        inboundPackage = "{{moduleBasePackage}}.ports.inbound";
        inboundDtosPackage = "{{moduleBasePackage}}.ports.inbound.dtos";

        // outbound / secondary ports
        outboundPackage = "{{moduleBasePackage}}.ports.outbound";
        outboundRepositoryPackage = "{{moduleBasePackage}}.ports.outbound.{{persistence}}";
        // outbound / secondary ports for events
        outboundEventsPackage = "{{moduleBasePackage}}.ports.outbound.events";
        outboundEventsModelPackage = "{{moduleBasePackage}}.ports.outbound.events.dtos";

        // core implementation / inner ring
        coreImplementationPackage = "{{moduleBasePackage}}.application";
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
