package io.zenwave360.sdk.zdl.utils;

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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The default traversal in {@link ZDLAnnotator}: every annotator pack depends on it visiting the
 * whole model, and on being handed the ZDL node matching each java element.
 */
class ZDLAnnotatorTest {

    /**
     * A ZDL exercising every element kind the traversal walks: an entity with an id, a value object
     * without one, relationships, an enum with values, an input, an output, a local event, an
     * external (asyncapi) event, and methods with and without a return type.
     */
    private static final String ZDL = """
            config {
                basePackage "io.example.orders"
            }

            @aggregate
            entity Customer {
              username String required
              email String
            }

            entity Address {
              street String
              type AddressType
            }

            @vo
            entity Money {
              amount BigDecimal
              currency String
            }

            relationship OneToMany {
              Customer{addresses} to Address{customer}
            }

            enum AddressType { HOME, WORK }

            input CustomerInput {
              username String
              email String
            }

            output CustomerOutput {
              id String
              username String
            }

            event CustomerCreated {
              customerId String
            }

            @asyncapi({channel: "externalChannel", topic: "external-topic"})
            event ExternalCustomerEvent {
              customerId String
            }

            service CustomerService for (Customer) {
              createCustomer(CustomerInput) CustomerOutput
              deleteCustomer(id)
            }
            """;

    /** Records every callback the traversal makes, and the zdl node it was handed with it. */
    static class RecordingAnnotator implements ZDLAnnotator {

        final Set<String> visited = new LinkedHashSet<>();
        final List<String> zdlNodeNames = new ArrayList<>();

        private void visit(String kind, String name, Map<String, Object> zdlNode) {
            visited.add(kind + ":" + name);
            zdlNodeNames.add(kind + ":" + name + "->" + (zdlNode != null ? zdlNode.get("name") : null));
        }

        @Override
        public void annotate(JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
            visit("entity", entity.name(), zdlEntity);
        }

        @Override
        public void annotate(JavaZdlModel.Field field, JavaZdlModel.Annotated owner, Map<String, Object> zdlOwner, Map<String, Object> zdl) {
            visited.add("field:" + field.name());
        }

        @Override
        public void annotate(JavaZdlModel.Relationship relationship, JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
            visited.add("relationship:" + entity.name() + "." + relationship.name());
        }

        @Override
        public void annotate(JavaZdlModel.Enum javaEnum, Map<String, Object> zdlEnum, Map<String, Object> zdl) {
            visit("enum", javaEnum.name(), zdlEnum);
        }

        @Override
        public void annotate(JavaZdlModel.EnumValue enumValue, JavaZdlModel.Enum javaEnum, Map<String, Object> zdlEnum, Map<String, Object> zdl) {
            visited.add("enumValue:" + javaEnum.name() + "." + enumValue.name());
        }

        @Override
        public void annotate(JavaZdlModel.Input input, Map<String, Object> zdlInput, Map<String, Object> zdl) {
            visit("input", input.name(), zdlInput);
        }

        @Override
        public void annotate(JavaZdlModel.Output output, Map<String, Object> zdlOutput, Map<String, Object> zdl) {
            visit("output", output.name(), zdlOutput);
        }

        @Override
        public void annotate(JavaZdlModel.Event event, Map<String, Object> zdlEvent, Map<String, Object> zdl) {
            visit("event", event.name(), zdlEvent);
        }

        @Override
        public void annotate(JavaZdlModel.Service service, Map<String, Object> zdlService, Map<String, Object> zdl) {
            visit("service", service.name(), zdlService);
        }

        @Override
        public void annotate(JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> method, Map<String, Object> zdl) {
            visit("method", serviceMethod.name(), method);
        }

        @Override
        public void annotate(JavaZdlModel.MethodParameter methodParameter, Map<String, Object> method, Map<String, Object> zdl) {
            visited.add("parameter:" + method.get("name") + "." + methodParameter.name());
        }

        @Override
        public void annotate(JavaZdlModel.ReturnType returnType, JavaZdlModel.ServiceMethod serviceMethod, Map<String, Object> method, Map<String, Object> zdl) {
            visited.add("returnType:" + serviceMethod.name() + "." + returnType.type());
        }
    }

    @TempDir
    Path tempDir;

    private Map<String, Object> zdl;
    private JavaZdlModel javaModel;
    private RecordingAnnotator annotator;

    @BeforeEach
    void parseModel() throws IOException {
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ZDL, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        zdl = (Map<String, Object>) new ZDLProcessor().process(contextModel).get("zdl");
        javaModel = new JavaZdlModel(zdl);
        annotator = new RecordingAnnotator();
        annotator.annotate(javaModel, zdl);
    }

    @Test
    void visitsEntitiesTheirFieldsAndRelationships() {
        Assertions.assertTrue(annotator.visited.contains("entity:Customer"));
        Assertions.assertTrue(annotator.visited.contains("entity:Address"));
        Assertions.assertTrue(annotator.visited.contains("field:username"));
        Assertions.assertTrue(annotator.visited.contains("field:street"));
        Assertions.assertTrue(annotator.visited.contains("relationship:Customer.addresses"));
        Assertions.assertTrue(annotator.visited.contains("relationship:Address.customer"));
    }

    @Test
    void visitsTheSyntheticIdFieldOfEntitiesThatHaveOne() {
        // the technical id is injected by the templates, not a ZDL field, but annotators must see it
        Assertions.assertTrue(annotator.visited.contains("field:id"));
    }

    @Test
    void skipsTheIdFieldOfValueObjects() {
        // Money is @vo: no identity, so createIdField returns null and the traversal must not visit it
        var money = javaModel.entities.stream().filter(e -> e.name().equals("Money")).findFirst().orElseThrow();
        Assertions.assertNull(money.idField());
        Assertions.assertTrue(annotator.visited.contains("entity:Money"));
        Assertions.assertTrue(annotator.visited.contains("field:currency"));
    }

    @Test
    void visitsEnumsAndTheirValues() {
        Assertions.assertTrue(annotator.visited.contains("enum:AddressType"));
        Assertions.assertTrue(annotator.visited.contains("enumValue:AddressType.HOME"));
        Assertions.assertTrue(annotator.visited.contains("enumValue:AddressType.WORK"));
    }

    @Test
    void visitsInputsAndOutputsWithTheirFields() {
        Assertions.assertTrue(annotator.visited.contains("input:CustomerInput"));
        Assertions.assertTrue(annotator.visited.contains("output:CustomerOutput"));
        Assertions.assertTrue(annotator.visited.contains("field:email"));
    }

    @Test
    void visitsLocalAndExternalEventsAlike() {
        // asyncapi events land in externalEvents, and the traversal walks that list too
        Assertions.assertFalse(javaModel.events.isEmpty());
        Assertions.assertFalse(javaModel.externalEvents.isEmpty());
        Assertions.assertTrue(annotator.visited.contains("event:CustomerCreated"));
        Assertions.assertTrue(annotator.visited.contains("event:ExternalCustomerEvent"));
        Assertions.assertTrue(annotator.visited.contains("field:customerId"));
    }

    @Test
    void visitsServicesMethodsParametersAndReturnTypes() {
        Assertions.assertTrue(annotator.visited.contains("service:CustomerService"));
        Assertions.assertTrue(annotator.visited.contains("method:createCustomer"));
        Assertions.assertTrue(annotator.visited.contains("parameter:createCustomer.input"));
        Assertions.assertTrue(annotator.visited.contains("returnType:createCustomer.CustomerOutput"));
    }

    @Test
    void skipsTheReturnTypeOfVoidMethods() {
        var deleteCustomer = javaModel.services.get(0).methods().stream()
                .filter(method -> method.name().equals("deleteCustomer")).findFirst().orElseThrow();
        Assertions.assertNull(deleteCustomer.returnType());
        Assertions.assertTrue(annotator.visited.contains("method:deleteCustomer"));
        Assertions.assertTrue(annotator.visited.contains("parameter:deleteCustomer.id"));
        Assertions.assertTrue(annotator.visited.stream().noneMatch(v -> v.startsWith("returnType:deleteCustomer")));
    }

    @Test
    void handsEachElementItsMatchingZdlNode() {
        // annotators branch on the zdl node (options, annotations), so a mismatched node is silent breakage
        Assertions.assertTrue(annotator.zdlNodeNames.contains("entity:Customer->Customer"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("enum:AddressType->AddressType"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("input:CustomerInput->CustomerInput"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("output:CustomerOutput->CustomerOutput"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("event:CustomerCreated->CustomerCreated"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("service:CustomerService->CustomerService"));
        Assertions.assertTrue(annotator.zdlNodeNames.contains("method:createCustomer->createCustomer"));
    }

    @Test
    void traversalOverAnEmptyModelVisitsNothing() {
        var empty = new RecordingAnnotator();
        empty.annotate(new JavaZdlModel(Map.of()), Map.of());
        Assertions.assertEquals(Set.of(), empty.visited);
    }

    @Test
    void runningTheSameAnnotatorTwiceDoesNotDuplicateAnnotations() {
        // the documented idempotence contract: annotators run in ZDLProcessor and may run again in
        // ZDLProjectGenerator
        ZDLAnnotator marker = new ZDLAnnotator() {
            @Override
            public void annotate(JavaZdlModel.Entity entity, Map<String, Object> zdlEntity, Map<String, Object> zdl) {
                entity.addAnnotation(JavaZdlModel.Annotation.of("com.acme.Marker"));
            }
        };
        marker.annotate(javaModel, zdl);
        marker.annotate(javaModel, zdl);

        var customer = javaModel.entities.stream().filter(e -> e.name().equals("Customer")).findFirst().orElseThrow();
        Assertions.assertEquals(1, customer.annotations().size());
    }
}
