package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZdlToMarkdownSupportTest {

    @Test
    void diagramModelResolver_resolvesAggregateAndRootEntity() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");

        var aggregate = DiagramModelResolver.resolveAggregate(zdl, "CustomerOrderAggregate");
        var rootEntity = DiagramModelResolver.resolveAggregateRootEntity(zdl, aggregate);
        var diagramEntity = DiagramModelResolver.resolveDiagramEntity(zdl, "CustomerOrderAggregate");

        Assertions.assertEquals("CustomerOrderAggregate", JSONPath.get(aggregate, "name"));
        Assertions.assertEquals("CustomerOrder", JSONPath.get(rootEntity, "name"));
        Assertions.assertEquals("CustomerOrder", JSONPath.get(diagramEntity, "name"));
    }

    @Test
    void diagramModelResolver_returnsNullForInvalidAggregate_andResolvesDirectEntityName() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");

        var invalidAggregate = DiagramModelResolver.resolveAggregate(zdl, Map.of("name", "BrokenAggregate"));
        var missingAggregate = DiagramModelResolver.resolveAggregate(zdl, "MissingAggregate");
        var directEntity = DiagramModelResolver.resolveDiagramEntity(zdl, "CustomerOrder");

        Assertions.assertNull(invalidAggregate);
        Assertions.assertNull(missingAggregate);
        Assertions.assertEquals("CustomerOrder", JSONPath.get(directEntity, "name"));
    }

    @Test
    void diagramModelResolver_fallsBackToAllEntitiesAndEnumsForAggregateRoot() {
        Map<String, Object> aggregate = Map.of(
                "name", "CustomerSummaryAggregate",
                "aggregateRoot", "CustomerSummary"
        );
        Map<String, Object> zdl = Map.of(
                "entities", Map.of(),
                "allEntitiesAndEnums", Map.of(
                        "CustomerSummary", Map.of("name", "CustomerSummary", "type", "outputs")
                )
        );

        var rootEntity = DiagramModelResolver.resolveAggregateRootEntity(zdl, aggregate);

        Assertions.assertEquals("CustomerSummary", JSONPath.get(rootEntity, "name"));
        Assertions.assertTrue(DiagramModelResolver.isNamedAggregate(aggregate));
        Assertions.assertFalse(DiagramModelResolver.isNamedAggregate(Map.of("name", "BrokenAggregate")));
    }

    @Test
    void associationCollector_collectsEntityAssociationsWithoutDuplicates() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        var customer = map(JSONPath.get(zdl, "$.entities.Customer"));

        var associations = AssociationCollector.collectEntityAssociations(customer, zdl);
        var targets = AssociationCollector.uniqueAssociationTargets(associations, "Customer").stream()
                .map(entity -> JSONPath.get(entity, "name", ""))
                .toList();

        Assertions.assertFalse(associations.isEmpty());
        Assertions.assertTrue(targets.contains("Address"));
        Assertions.assertTrue(targets.contains("AddressType"));
        Assertions.assertEquals(targets.size(), targets.stream().distinct().count());
    }

    @Test
    void associationCollector_filtersDuplicateTargetsAndExcludedNames() throws Exception {
        var zdl = loadZdl("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        var customer = map(JSONPath.get(zdl, "$.entities.Customer"));
        var address = map(JSONPath.get(zdl, "$.entities.Address"));

        var associations = AssociationCollector.collectCollectionAssociations(List.of(customer, address), zdl);
        var uniqueTargets = AssociationCollector.uniqueAssociationTargets(associations, List.of("Customer", "AddressType"));

        Assertions.assertFalse(associations.isEmpty());
        Assertions.assertEquals(List.of("Address"), uniqueTargets.stream()
                .map(entity -> JSONPath.get(entity, "name", ""))
                .toList());
    }

    @Test
    void serviceHelperFormatter_formatsAnnotationsAndServiceModels() {
        var method = Map.<String, Object>of(
                "paramId", "id",
                "parameter", "CustomerInput",
                "returnType", "CustomerOutput",
                "returnTypeIsArray", true,
                "withEvents", List.of("CustomerEvent", List.of("CustomerCreated", "CustomerUpdated")),
                "options", Map.of(
                        "post", "/customers",
                        "asyncapi", Map.of("channel", "customerChannel", "topic", "customer-topic"),
                        "paginated", true
                )
        );

        Map<String, Object> zdlModel = Map.of(
                "allEntitiesAndEnums", Map.of(
                        "CustomerInput", Map.of("name", "CustomerInput", "type", "inputs"),
                        "CustomerOutput", Map.of("name", "CustomerOutput", "type", "outputs")
                ),
                "events", Map.of(
                        "CustomerEvent", Map.of("name", "CustomerEvent", "type", "events"),
                        "CustomerCreated", Map.of("name", "CustomerCreated", "type", "events"),
                        "CustomerUpdated", Map.of("name", "CustomerUpdated", "type", "events")
                )
        );
        Map<String, Object> service = Map.of("methods", Map.of("createCustomer", method));

        Assertions.assertEquals("id, CustomerInput", ServiceHelperFormatter.methodParamsSignature(method));
        Assertions.assertEquals("CustomerOutput[]", ServiceHelperFormatter.methodReturnType(method));
        Assertions.assertEquals("CustomerEvent [CustomerCreated | CustomerUpdated]", ServiceHelperFormatter.methodEvents(method));
        Assertions.assertTrue(ServiceHelperFormatter.formatMethodAnnotations(method).contains("@post(\"/customers\")"));
        Assertions.assertTrue(ServiceHelperFormatter.formatMethodAnnotations(method).contains("@paginated()"));
        Assertions.assertEquals(List.of("CustomerInput"), ServiceHelperFormatter.serviceInputs(service, zdlModel).stream().map(entity -> JSONPath.get(entity, "name", "")).toList());
        Assertions.assertEquals(List.of("CustomerOutput"), ServiceHelperFormatter.serviceOutputs(service, zdlModel).stream().map(entity -> JSONPath.get(entity, "name", "")).toList());
        Assertions.assertEquals(List.of("CustomerEvent", "CustomerCreated", "CustomerUpdated"), ServiceHelperFormatter.serviceEvents(service, zdlModel).stream().map(entity -> JSONPath.get(entity, "name", "")).toList());
    }

    @Test
    void serviceHelperFormatter_coversOptionalReturnTypesAndAnnotationValueShapes() {
        var options = new LinkedHashMap<String, Object>();
        options.put("secured", true);
        options.put("nullable", null);
        options.put("roles", List.of("admin", "support"));
        options.put("asyncapi", Map.of(
                "channel", "customerChannel",
                "headers", Map.of("tenant", "acme"),
                "tags", List.of("customer", "events")
        ));
        var method = new LinkedHashMap<String, Object>();
        method.put("name", "publish");
        method.put("returnType", "CustomerOutput");
        method.put("returnTypeIsOptional", true);
        method.put("options", options);

        var annotations = ServiceHelperFormatter.formatMethodAnnotations(method);

        Assertions.assertEquals("CustomerOutput?", ServiceHelperFormatter.methodReturnType(method));
        Assertions.assertTrue(annotations.contains("@secured()"));
        Assertions.assertTrue(annotations.contains("@nullable()"));
        Assertions.assertTrue(annotations.contains("@roles(admin, support)"));
        Assertions.assertTrue(annotations.contains("@asyncapi(channel: customerChannel, headers: {tenant: acme}, tags: [customer, events])"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadZdl(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        return (Map<String, Object>) model.get("zdl");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
