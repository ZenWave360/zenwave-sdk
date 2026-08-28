package io.zenwave360.sdk.zdl.layouts;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectLayoutTest {

    private static final Map<String, Object> OPTIONS = Map.of(
            "basePackage", "io.example.customer",
            "persistence", "jpa",
            "apiId", "customer");

    @Test
    void cleanArchitectureUsesBoundariesInteractorsAndGateways() {
        var layout = new CleanArchitectureProjectLayout().processedLayout(OPTIONS);

        assertAll(
                () -> assertEquals("io.example.customer.domain", layout.entitiesPackage),
                () -> assertEquals("io.example.customer.domain.event", layout.domainEventsPackage),
                () -> assertEquals("io.example.customer.usecase.boundary.input", layout.inboundPackage),
                () -> assertEquals("io.example.customer.usecase.boundary.input.dto", layout.inboundDtosPackage),
                () -> assertEquals("io.example.customer.usecase.boundary.output.jpa", layout.outboundRepositoryPackage),
                () -> assertEquals("io.example.customer.usecase.interactor", layout.coreImplementationPackage),
                () -> assertEquals("io.example.customer.adapter.gateway.jpa", layout.infrastructureRepositoryPackage),
                () -> assertEquals("io.example.customer.adapter.controller", layout.adaptersWebPackage),
                () -> assertEquals("io.example.customer.adapter.listener.customer", layout.adaptersEventsPackage),
                () -> assertEquals("io.example.customer.adapter.handler", layout.asyncApiConsumerApiPackage));
    }

    @Test
    void hexagonalArchitectureUsesApplicationPortsAndDirectionalAdapters() {
        var layout = new HexagonalProjectLayout().processedLayout(OPTIONS);

        assertAll(
                () -> assertEquals("io.example.customer.domain", layout.entitiesPackage),
                () -> assertEquals("io.example.customer.domain.event", layout.domainEventsPackage),
                () -> assertEquals("io.example.customer.application.port.in", layout.inboundPackage),
                () -> assertEquals("io.example.customer.application.port.in.dto", layout.inboundDtosPackage),
                () -> assertEquals("io.example.customer.application.port.out.jpa", layout.outboundRepositoryPackage),
                () -> assertEquals("io.example.customer.application.service", layout.coreImplementationPackage),
                () -> assertEquals("io.example.customer.adapter.out.jpa", layout.infrastructureRepositoryPackage),
                () -> assertEquals("io.example.customer.adapter.in.web", layout.adaptersWebPackage),
                () -> assertEquals("io.example.customer.adapter.in.event.customer", layout.adaptersEventsPackage),
                () -> assertEquals("io.example.customer.adapter.in.command", layout.asyncApiConsumerApiPackage));
    }

    @Test
    void cleanHexagonalUsesCoreApplicationPackage() {
        var layout = new CleanHexagonalProjectLayout().processedLayout(OPTIONS);
        var defaultLayout = new DefaultProjectLayout().processedLayout(OPTIONS);

        assertAll(
                () -> assertEquals("io.example.customer.core.application", layout.coreImplementationPackage),
                () -> assertEquals("io.example.customer.core.application.mappers", layout.coreImplementationMappersPackage),
                () -> assertEquals("io.example.customer.core.application", layout.coreImplementationCommonPackage),
                () -> assertEquals("io.example.customer.core.application", defaultLayout.coreImplementationPackage));
    }
}
