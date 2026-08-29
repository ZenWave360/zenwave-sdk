package io.zenwave360.sdk.zdl.annotators;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.zdl.model.JavaZdlModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The JSpecify nullability pack. Both its annotations are artifact independent, so they render
 * wherever the owning element renders.
 */
class JSpecifyAnnotatorTest {

    private static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";
    private static final String NULLABLE = "org.jspecify.annotations.Nullable";

    /** The inline input gives one method a required and an optional parameter side by side. */
    private static final String ZDL = """
            config {
                basePackage "io.example.orders"
            }

            @aggregate
            entity Customer {
              username String required
            }

            @inline
            input CustomerAddressId {
              customerId String required
              nickname String
            }

            service CustomerService for (Customer) {
              addCustomerAddress(CustomerAddressId) Customer
            }
            """;

    @TempDir
    Path tempDir;

    private JavaZdlModel javaModel;

    @BeforeEach
    void process() throws IOException {
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ZDL, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        var processor = new ZDLProcessor();
        processor.useJSpecify = true;
        var zdl = (Map<String, Object>) processor.process(contextModel).get("zdl");
        javaModel = (JavaZdlModel) zdl.get("javaModel");
    }

    private static List<String> names(JavaZdlModel.Annotated element) {
        return element.annotations().stream().map(JavaZdlModel.Annotation::name).toList();
    }

    @Test
    void servicesAreNullMarkedInEveryArtifactThatRendersThem() {
        var service = javaModel.services.get(0);
        Assertions.assertEquals(List.of(NULL_MARKED), names(service));
        // artifact independent: renders on the port and on the implementation alike
        Assertions.assertTrue(service.annotations().get(0).appliesTo("inbound.service-port"));
        Assertions.assertTrue(service.annotations().get(0).appliesTo("application.service-impl"));
    }

    @Test
    void onlyOptionalParametersAreAnnotatedNullable() {
        var parameters = javaModel.services.get(0).methods().get(0).parameters();
        var required = parameters.stream().filter(p -> p.name().equals("customerId")).findFirst().orElseThrow();
        var optional = parameters.stream().filter(p -> p.name().equals("nickname")).findFirst().orElseThrow();

        Assertions.assertFalse(required.isOptional());
        Assertions.assertEquals(List.of(), names(required));

        Assertions.assertTrue(optional.isOptional());
        Assertions.assertEquals(List.of(NULLABLE), names(optional));
    }

    @Test
    void jspecifyAndJmoleculesCanBeEnabledTogether() throws IOException {
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ZDL, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        var processor = new ZDLProcessor();
        processor.useJSpecify = true;
        processor.useJMolecules = true;
        var zdl = (Map<String, Object>) processor.process(contextModel).get("zdl");
        var model = (JavaZdlModel) zdl.get("javaModel");

        // jspecify contributes to the service, jmolecules to the entity, neither overwrites the other
        Assertions.assertEquals(List.of(NULL_MARKED), names(model.services.get(0)));
        Assertions.assertEquals(
                List.of("org.jmolecules.ddd.annotation.AggregateRoot", "org.jmolecules.ddd.annotation.Repository"),
                names(model.entities.get(0)));
    }
}
