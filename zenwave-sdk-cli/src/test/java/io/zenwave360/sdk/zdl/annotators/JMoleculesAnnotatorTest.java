package io.zenwave360.sdk.zdl.annotators;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.zdl.annotators.JMoleculesAnnotator.Architecture;
import io.zenwave360.sdk.zdl.layouts.CleanArchitectureProjectLayout;
import io.zenwave360.sdk.zdl.layouts.CleanHexagonalProjectLayout;
import io.zenwave360.sdk.zdl.layouts.DefaultProjectLayout;
import io.zenwave360.sdk.zdl.layouts.HexagonalProjectLayout;
import io.zenwave360.sdk.zdl.layouts.LayeredProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import io.zenwave360.sdk.zdl.layouts.SimpleDomainProjectLayout;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import io.zenwave360.sdk.zdl.model.JavaZdlModel.Annotated;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.ADAPTER_WEB_CONTROLLER;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.APPLICATION_SERVICE_IMPL;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.DOMAIN_ENTITY;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.DOMAIN_EVENT;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.INBOUND_DTO;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.INBOUND_SERVICE_PORT;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.INFRASTRUCTURE_EVENT_PUBLISHER;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.OUTBOUND_EVENT_PUBLISHER_PORT;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.OUTBOUND_REPOSITORY_PORT;
import static io.zenwave360.sdk.zdl.annotators.CoreArtifactType.PACKAGE_INFO_MODULE;

/**
 * The jMolecules annotation pack, driven through {@link ZDLProcessor} so the {@code useJMolecules}
 * wiring is part of what is tested.
 */
class JMoleculesAnnotatorTest {

    private static final String DDD = "org.jmolecules.ddd.annotation.";
    private static final String EVENT = "org.jmolecules.event.annotation.";
    private static final String HEX = "org.jmolecules.architecture.hexagonal.";
    private static final String LAYER = "org.jmolecules.architecture.layered.";

    /**
     * Covers all three ways an entity becomes an aggregate root, a plain entity that is none of
     * them, and both flavours of value object.
     */
    private static final String ZDL = """
            config {
                basePackage "io.example.orders"
            }

            @aggregate
            entity Customer {
              username String required
            }

            @lifecycle(field: status, initial: NEW)
            entity Order {
              status OrderStatus
            }

            aggregate ShipmentAggregate(Shipment) {
              ship(ShipmentInput) withEvents ShipmentDispatched
            }

            entity Shipment {
              trackingId String
            }

            entity Address {
              street String
            }

            @vo
            entity Money {
              amount BigDecimal
            }

            @embedded
            entity Audit {
              createdBy String
            }

            enum OrderStatus { NEW, DONE }

            input ShipmentInput { trackingId String }

            input CustomerInput { username String }

            event CustomerCreated { customerId String }

            event ShipmentDispatched { shipmentId String }

            @asyncapi({channel: "extChannel", topic: "ext-topic"})
            event ExternalCustomerEvent { customerId String }

            service CustomerService for (Customer) {
              createCustomer(CustomerInput) Customer
            }
            """;

    @TempDir
    Path tempDir;

    private JavaZdlModel process(ProjectLayout layout) throws IOException {
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ZDL, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        var processor = new ZDLProcessor();
        processor.useJMolecules = true;
        processor.layout = layout;
        var zdl = (Map<String, Object>) processor.process(contextModel).get("zdl");
        return (JavaZdlModel) zdl.get("javaModel");
    }

    private static List<String> annotationsOf(Annotated element, CoreArtifactType artifactType) {
        return element.annotations().stream()
                .filter(annotation -> annotation.appliesTo(artifactType.id()))
                .map(JavaZdlModel.Annotation::name)
                .toList();
    }

    private static JavaZdlModel.Entity entity(JavaZdlModel model, String name) {
        return model.entities.stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow();
    }

    private static JavaZdlModel.Event event(JavaZdlModel model, String name) {
        return model.events.stream().filter(e -> e.name().equals(name)).findFirst()
                .orElseGet(() -> model.externalEvents.stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow());
    }

    // ── architectureOf ────────────────────────────────────────────────────────

    @Test
    void layeredLayoutImpliesTheLayeredVocabulary() {
        Assertions.assertEquals(Architecture.LAYERED, JMoleculesAnnotator.architectureOf(new LayeredProjectLayout()));
    }

    @Test
    void hexagonalAndCleanHexagonalLayoutsImplyTheHexagonalVocabulary() {
        Assertions.assertEquals(Architecture.HEXAGONAL, JMoleculesAnnotator.architectureOf(new HexagonalProjectLayout()));
        Assertions.assertEquals(Architecture.HEXAGONAL, JMoleculesAnnotator.architectureOf(new CleanHexagonalProjectLayout()));
        // DefaultProjectLayout extends CleanHexagonalProjectLayout
        Assertions.assertEquals(Architecture.HEXAGONAL, JMoleculesAnnotator.architectureOf(new DefaultProjectLayout()));
    }

    @Test
    void layoutsWithoutAPortBoundaryImplyNoArchitectureVocabulary() {
        // SimpleDomainProjectLayout puts repositories in the base package: there is no boundary to name.
        // CleanArchitecture has one, but its vocabulary is onion, which is not implemented.
        Assertions.assertEquals(Architecture.NONE, JMoleculesAnnotator.architectureOf(new SimpleDomainProjectLayout()));
        Assertions.assertEquals(Architecture.NONE, JMoleculesAnnotator.architectureOf(new CleanArchitectureProjectLayout()));
        Assertions.assertEquals(Architecture.NONE, JMoleculesAnnotator.architectureOf(new ProjectLayout()));
        Assertions.assertEquals(Architecture.NONE, JMoleculesAnnotator.architectureOf(null));
    }

    // ── entities ──────────────────────────────────────────────────────────────

    @Test
    void aggregateRootsAreAnnotatedByAllThreeRules() throws IOException {
        var model = process(new SimpleDomainProjectLayout());

        // @aggregate option, @lifecycle option, and the root of a declared aggregate
        for (String name : List.of("Customer", "Order", "Shipment")) {
            Assertions.assertEquals(List.of(DDD + "AggregateRoot"), annotationsOf(entity(model, name), DOMAIN_ENTITY),
                    name + " should be an aggregate root");
            Assertions.assertEquals(List.of(DDD + "Repository"), annotationsOf(entity(model, name), OUTBOUND_REPOSITORY_PORT),
                    name + " should get a repository");
        }
    }

    @Test
    void plainEntitiesGetEntityAndNoRepository() throws IOException {
        var model = process(new SimpleDomainProjectLayout());

        // @AggregateRoot is meta-annotated with @Entity: never both
        Assertions.assertEquals(List.of(DDD + "Entity"), annotationsOf(entity(model, "Address"), DOMAIN_ENTITY));
        Assertions.assertEquals(List.of(), annotationsOf(entity(model, "Address"), OUTBOUND_REPOSITORY_PORT));
    }

    @Test
    void valueObjectsGetValueObjectAndNoIdentityOrRepository() throws IOException {
        var model = process(new HexagonalProjectLayout());

        for (String name : List.of("Money", "Audit")) {
            Assertions.assertEquals(List.of(DDD + "ValueObject"), annotationsOf(entity(model, name), DOMAIN_ENTITY), name);
            Assertions.assertEquals(List.of(), annotationsOf(entity(model, name), OUTBOUND_REPOSITORY_PORT), name);
            Assertions.assertNull(entity(model, name).idField(), name + " has no identity");
        }
    }

    @Test
    void theIdFieldIsTheOnlyFieldAnnotatedWithIdentity() throws IOException {
        var model = process(new SimpleDomainProjectLayout());
        var customer = entity(model, "Customer");

        Assertions.assertEquals(List.of(DDD + "Identity"), annotationsOf(customer.idField(), DOMAIN_ENTITY));
        Assertions.assertEquals(List.of(), annotationsOf(customer.fields().get(0), DOMAIN_ENTITY));
        // owner is an Input, not an Entity
        Assertions.assertEquals(List.of(), annotationsOf(model.inputs.get(0).fields().get(0), DOMAIN_ENTITY));
    }

    // ── events ────────────────────────────────────────────────────────────────

    @Test
    void localEventsAreDomainEventsAndAsyncapiPayloadsAreNot() throws IOException {
        var model = process(new HexagonalProjectLayout());

        Assertions.assertEquals(List.of(EVENT + "DomainEvent"), annotationsOf(event(model, "CustomerCreated"), DOMAIN_EVENT));
        // generated from the AsyncAPI contract, not a domain event
        Assertions.assertEquals(List.of(), annotationsOf(event(model, "ExternalCustomerEvent"), DOMAIN_EVENT));
    }

    // ── architecture vocabularies ─────────────────────────────────────────────

    @Test
    void noneAddsDddVocabularyOnly() throws IOException {
        var model = process(new SimpleDomainProjectLayout());

        Assertions.assertEquals(List.of(), annotationsOf(model.services.get(0), INBOUND_SERVICE_PORT));
        Assertions.assertEquals(List.of(), annotationsOf(model.services.get(0), APPLICATION_SERVICE_IMPL));
        Assertions.assertEquals(List.of(), model.artifactAnnotations(OUTBOUND_EVENT_PUBLISHER_PORT.id()));
        Assertions.assertEquals(List.of(), model.artifactAnnotations(ADAPTER_WEB_CONTROLLER.id()));
        Assertions.assertEquals("NONE", model.jmoleculesArchitecture);
    }

    @Test
    void hexagonalAddsPortsAndAdapters() throws IOException {
        var model = process(new HexagonalProjectLayout());

        // one service, annotated differently in the port and in the implementation
        Assertions.assertEquals(List.of(HEX + "PrimaryPort"), annotationsOf(model.services.get(0), INBOUND_SERVICE_PORT));
        Assertions.assertEquals(List.of(HEX + "Application"), annotationsOf(model.services.get(0), APPLICATION_SERVICE_IMPL));
        // the repository is the aggregate's secondary port
        Assertions.assertEquals(List.of(DDD + "Repository", HEX + "SecondaryPort"),
                annotationsOf(entity(model, "Customer"), OUTBOUND_REPOSITORY_PORT));
        // no layered vocabulary leaks in
        Assertions.assertEquals(List.of(DDD + "AggregateRoot"), annotationsOf(entity(model, "Customer"), DOMAIN_ENTITY));

        Assertions.assertEquals(List.of(HEX + "SecondaryPort"), names(model.artifactAnnotations(OUTBOUND_EVENT_PUBLISHER_PORT.id())));
        Assertions.assertEquals(List.of(HEX + "SecondaryAdapter"), names(model.artifactAnnotations(INFRASTRUCTURE_EVENT_PUBLISHER.id())));
        Assertions.assertEquals(List.of(HEX + "PrimaryAdapter"), names(model.artifactAnnotations(ADAPTER_WEB_CONTROLLER.id())));
        Assertions.assertEquals("HEXAGONAL", model.jmoleculesArchitecture);
    }

    @Test
    void layeredAddsLayerVocabulary() throws IOException {
        var model = process(new LayeredProjectLayout());

        Assertions.assertEquals(List.of(DDD + "AggregateRoot", LAYER + "DomainLayer"),
                annotationsOf(entity(model, "Customer"), DOMAIN_ENTITY));
        Assertions.assertEquals(List.of(DDD + "Repository", LAYER + "InfrastructureLayer"),
                annotationsOf(entity(model, "Customer"), OUTBOUND_REPOSITORY_PORT));
        // a plain entity is still in the domain layer, it just gets no repository
        Assertions.assertEquals(List.of(DDD + "Entity", LAYER + "DomainLayer"),
                annotationsOf(entity(model, "Address"), DOMAIN_ENTITY));
        Assertions.assertEquals(List.of(EVENT + "DomainEvent", LAYER + "DomainLayer"),
                annotationsOf(event(model, "CustomerCreated"), DOMAIN_EVENT));
        // one annotation scoped to both the port and the implementation
        Assertions.assertEquals(List.of(LAYER + "ApplicationLayer"), annotationsOf(model.services.get(0), INBOUND_SERVICE_PORT));
        Assertions.assertEquals(List.of(LAYER + "ApplicationLayer"), annotationsOf(model.services.get(0), APPLICATION_SERVICE_IMPL));

        Assertions.assertEquals(List.of(LAYER + "InfrastructureLayer"), names(model.artifactAnnotations(INFRASTRUCTURE_EVENT_PUBLISHER.id())));
        Assertions.assertEquals(List.of(LAYER + "InterfaceLayer"), names(model.artifactAnnotations(ADAPTER_WEB_CONTROLLER.id())));
        Assertions.assertEquals("LAYERED", model.jmoleculesArchitecture);
    }

    @Test
    void artifactLevelAnnotationsAreAddedForEveryArchitecture() throws IOException {
        for (ProjectLayout layout : List.of(new SimpleDomainProjectLayout(), new HexagonalProjectLayout(), new LayeredProjectLayout())) {
            var model = process(layout);
            // inputs and outputs are identity free structures, rendered by a single template
            Assertions.assertEquals(List.of(DDD + "ValueObject"), names(model.artifactAnnotations(INBOUND_DTO.id())));
            Assertions.assertEquals(List.of(DDD + "Module"), names(model.artifactAnnotations(PACKAGE_INFO_MODULE.id())));
        }
    }

    @Test
    void annotatorsDoNotRunUnlessEnabled() throws IOException {
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ZDL, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        var zdl = (Map<String, Object>) new ZDLProcessor().process(contextModel).get("zdl");
        var model = (JavaZdlModel) zdl.get("javaModel");

        Assertions.assertEquals(List.of(), entity(model, "Customer").annotations());
        Assertions.assertNull(model.jmoleculesArchitecture);
    }

    private static List<String> names(List<JavaZdlModel.Annotation> annotations) {
        return annotations.stream().map(JavaZdlModel.Annotation::name).toList();
    }
}
