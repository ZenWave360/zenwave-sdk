package io.zenwave360.sdk.zdl.layouts;

/**
 * Hexagonal Architecture (Ports and Adapters). The hexagon interior is represented by the
 * {@code domain} and {@code application} packages; application ports and their implementations
 * are grouped under {@code application}, while driving and driven adapters are siblings under
 * {@code adapter.in} and {@code adapter.out}.
 *
 * <pre>
 * 📦 {{basePackage}}
 *    📦 domain                             # Domain model: entities, aggregates, value objects
 *        └─ 📦 event                       # Domain events
 *    📦 application                        # Application-side hexagon interior
 *        ├─ 📦 port
 *        |     ├─ 📦 in                    # Driving ports (service interfaces)
 *        |     |     └─ 📦 dto             # Command / query models
 *        |     └─ 📦 out                   # Driven ports
 *        |           ├─ 📦 {{persistence}} # Repository interfaces (Spring Data)
 *        |           └─ 📦 event           # EventPublisher interface
 *        |                 └─ 📦 dto       # AsyncAPI payloads
 *        └─ 📦 service                     # Driving port implementations
 *              └─ 📦 mapper
 *    📦 adapter
 *        ├─ 📦 in                          # Driving adapters
 *        |     ├─ 📦 web                   # REST controllers
 *        |     ├─ 📦 event                 # Event listeners
 *        |     └─ 📦 command               # AsyncAPI command handlers
 *        └─ 📦 out                         # Driven adapters
 *              ├─ 📦 {{persistence}}       # Custom repository implementations
 *              └─ 📦 event                 # EventPublisher implementation
 *    📦 config                             # Spring Boot wiring
 * </pre>
 */
public class HexagonalProjectLayout extends ProjectLayout {

    {
        basePackage = "{{basePackage}}";
        // in case of modular project
        configPackage = "{{basePackage}}.config";
        commonPackage = "{{basePackage}}"; // set to "{{basePackage}}.common" in modular projects
        modulesPackage = "{{basePackage}}.modules";

        // module specific
        moduleBasePackage = "{{basePackage}}";
        moduleConfigPackage = "{{moduleBasePackage}}.config";

        // domain model
        entitiesPackage = "{{moduleBasePackage}}.domain";
        domainEventsPackage = "{{entitiesPackage}}.event";

        // driving (primary) ports
        inboundPackage = "{{moduleBasePackage}}.application.port.in";
        inboundDtosPackage = "{{inboundPackage}}.dto";

        // driven (secondary) ports
        outboundPackage = "{{moduleBasePackage}}.application.port.out";
        outboundRepositoryPackage = "{{outboundPackage}}.{{persistence}}";
        outboundEventsPackage = "{{outboundPackage}}.event";
        outboundEventsModelPackage = "{{outboundEventsPackage}}.dto";

        // hexagon interior: driving port implementations
        coreImplementationPackage = "{{moduleBasePackage}}.application.service";
        coreImplementationMappersPackage = "{{coreImplementationPackage}}.mapper";

        // driven adapters
        infrastructurePackage = "{{moduleBasePackage}}.adapter.out";
        infrastructureRepositoryPackage = "{{infrastructurePackage}}.{{persistence}}";
        infrastructureEventsPackage = "{{infrastructurePackage}}.event";

        // driving adapters
        adaptersPackage = "{{moduleBasePackage}}.adapter.in";
        adaptersWebPackage = "{{adaptersPackage}}.web";
        adaptersWebMappersPackage = "{{adaptersWebPackage}}.mapper";
        adaptersCommandsPackage = "{{adaptersPackage}}.command";
        adaptersCommandsMappersPackage = "{{adaptersCommandsPackage}}.mapper";
        adaptersEventsPackage = "{{adaptersPackage}}.event{{#if apiId}}.{{apiId}}{{/if}}";
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
        coreImplementationCommonPackage = "{{commonPackage}}.application.service";
        coreImplementationMappersCommonPackage = "{{coreImplementationCommonPackage}}.mapper";
        infrastructureRepositoryCommonPackage = "{{commonPackage}}.adapter.out.{{persistence}}";
        infrastructureEventsCommonPackage = "{{commonPackage}}.adapter.out.event";
        adaptersWebCommonPackage = "{{commonPackage}}.adapter.in.web";
        adaptersWebMappersCommonPackage = "{{adaptersWebCommonPackage}}.mapper";
        adaptersCommandsCommonPackage = "{{commonPackage}}.adapter.in.command";
        adaptersCommandsMappersCommonPackage = "{{adaptersCommandsCommonPackage}}.mapper";
        adaptersEventsCommonPackage = "{{commonPackage}}.adapter.in.event";
        adaptersEventsMappersCommonPackage = "{{adaptersEventsCommonPackage}}.mapper";
    }

}
