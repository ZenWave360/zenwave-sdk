package io.zenwave360.sdk.zdl.layouts;

/**
 * Clean Architecture, using the ring vocabulary of Robert C. Martin's "Clean Architecture" (2017):
 * Entities, Use Cases (Input/Output Boundaries + Interactors), and Interface Adapters
 * (Controllers, Listeners, Handlers, and Gateways).
 *
 * <pre>
 * 📦 {{basePackage}}
 *    📦 domain                             # Entities ring: Enterprise Business Rules
 *        └─ 📦 event                       # Domain events
 *    📦 usecase                            # Use Cases ring: Application Business Rules
 *        ├─ 📦 boundary
 *        |     ├─ 📦 input                 # Input Boundary (service interfaces)
 *        |     |     └─ 📦 dto             # Request models
 *        |     └─ 📦 output                # Output Boundary / Data Access Interfaces
 *        |           ├─ 📦 {{persistence}} # Repository interfaces (Spring Data)
 *        |           └─ 📦 event           # EventPublisher interface
 *        |                 └─ 📦 dto       # AsyncAPI payloads
 *        └─ 📦 interactor                  # Use case implementations
 *              └─ 📦 mapper
 *    📦 adapter                            # Interface Adapters ring
 *        ├─ 📦 controller                  # REST controllers
 *        |     ├─ 📦 dto
 *        |     └─ 📦 mapper
 *        ├─ 📦 listener                    # Event listeners
 *        ├─ 📦 handler                     # AsyncAPI command handlers
 *        └─ 📦 gateway                     # Database / messaging gateways
 *              ├─ 📦 {{persistence}}       # Custom repository implementations
 *              └─ 📦 event                 # EventPublisher implementation
 *    📦 config                             # Spring Boot wiring
 * </pre>
 */
public class CleanArchitectureProjectLayout extends ProjectLayout {

    {
        basePackage = "{{basePackage}}";
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}"; // set to "{{basePackage}}.common" in modular projects
        modulesPackage = "{{basePackage}}.modules";

        // module specific
        moduleBasePackage = "{{basePackage}}";
        moduleConfigPackage = "{{moduleBasePackage}}.config";

        // Entities ring
        entitiesPackage = "{{moduleBasePackage}}.domain";
        domainEventsPackage = "{{entitiesPackage}}.event";

        // Use Cases ring: Input Boundary
        inboundPackage = "{{moduleBasePackage}}.usecase.boundary.input";
        inboundDtosPackage = "{{inboundPackage}}.dto";

        // Use Cases ring: Output Boundary / Data Access Interfaces
        outboundPackage = "{{moduleBasePackage}}.usecase.boundary.output";
        outboundRepositoryPackage = "{{outboundPackage}}.{{persistence}}";
        outboundEventsPackage = "{{outboundPackage}}.event";
        outboundEventsModelPackage = "{{outboundEventsPackage}}.dto";

        // Use Cases ring: Interactors
        coreImplementationPackage = "{{moduleBasePackage}}.usecase.interactor";
        coreImplementationMappersPackage = "{{coreImplementationPackage}}.mapper";

        // Interface Adapters ring: Gateways
        infrastructurePackage = "{{moduleBasePackage}}.adapter.gateway";
        infrastructureRepositoryPackage = "{{infrastructurePackage}}.{{persistence}}";
        infrastructureEventsPackage = "{{infrastructurePackage}}.event";

        // Interface Adapters ring: Controllers, Listeners, Handlers
        adaptersPackage = "{{moduleBasePackage}}.adapter";
        adaptersWebPackage = "{{adaptersPackage}}.controller";
        adaptersWebMappersPackage = "{{adaptersWebPackage}}.mapper";
        adaptersCommandsPackage = "{{adaptersPackage}}.handler";
        adaptersCommandsMappersPackage = "{{adaptersCommandsPackage}}.mapper";
        adaptersEventsPackage = "{{adaptersPackage}}.listener{{#if apiId}}.{{apiId}}{{/if}}";
        adaptersEventsMappersPackage = "{{adaptersEventsPackage}}.mapper";

        // openapi generated packages
        openApiApiPackage = "{{adaptersWebPackage}}";
        openApiModelPackage = "{{adaptersWebPackage}}.dto";
        // asyncapi generated packages
        asyncApiModelPackage = "{{outboundEventsModelPackage}}";
        asyncApiProducerApiPackage = "{{outboundEventsPackage}}";
        asyncApiConsumerApiPackage = "{{adaptersCommandsPackage}}";

        // common packages (for base classes in modular projects)
        entitiesCommonPackage = "{{commonPackage}}.domain";
        domainEventsCommonPackage = "{{entitiesCommonPackage}}.event";
        coreImplementationCommonPackage = "{{commonPackage}}.usecase.interactor";
        coreImplementationMappersCommonPackage = "{{coreImplementationCommonPackage}}.mapper";
        infrastructureRepositoryCommonPackage = "{{commonPackage}}.adapter.gateway.{{persistence}}";
        infrastructureEventsCommonPackage = "{{commonPackage}}.adapter.gateway.event";
        adaptersWebCommonPackage = "{{commonPackage}}.adapter.controller";
        adaptersWebMappersCommonPackage = "{{adaptersWebCommonPackage}}.mapper";
        adaptersCommandsCommonPackage = "{{commonPackage}}.adapter.handler";
        adaptersCommandsMappersCommonPackage = "{{adaptersCommandsCommonPackage}}.mapper";
        adaptersEventsCommonPackage = "{{commonPackage}}.adapter.listener";
        adaptersEventsMappersCommonPackage = "{{adaptersEventsCommonPackage}}.mapper";
    }

}
