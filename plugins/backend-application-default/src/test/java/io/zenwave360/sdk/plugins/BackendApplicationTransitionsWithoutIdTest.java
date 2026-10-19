package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.plugins.support.MethodBodyCase;
import io.zenwave360.sdk.plugins.support.ServiceMethodBodyPlanner;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.testutils.MavenCompiler;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Spec sdk-service-transitions: what is generated for a from-transition without an id parameter. */
public class BackendApplicationTransitionsWithoutIdTest {

    private static final String ENTITY_ZDL = "classpath:zdl/transitions-without-id-entity.zdl";
    private static final String AGGREGATE_ZDL = "classpath:zdl/transitions-without-id-aggregate.zdl";
    private static final String INVENTORY_IMPL = "src/main/java/io/zenwave360/example/inventory/core/application/InventoryServiceImpl.java";
    private static final String ORDERS_IMPL = "src/main/java/io/zenwave360/example/orders/core/application/OrdersServiceImpl.java";

    @TempDir
    Path tempDir;

    private String generate(String zdl, String basePackage, String name) throws Exception {
        String targetFolder = "target/zdl/transitions-without-id/" + name;
        new MainGenerator().generate(new BackendApplicationDefaultPlugin()
                .withZdlFile(zdl)
                .withTargetFolder(targetFolder)
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false));
        return targetFolder;
    }

    private static String source(String targetFolder, String file) throws Exception {
        return Files.readString(Path.of(targetFolder, file));
    }

    /** Whitespace-normalised body of one method of a generated implementation. */
    private static String method(String source, String name) {
        String normalized = source.replaceAll("\\s+", " ");
        int start = normalized.indexOf(" " + name + "(");
        Assertions.assertTrue(start >= 0, "missing method " + name);
        int end = normalized.indexOf("public ", start + 1);
        return normalized.substring(start, end < 0 ? normalized.length() : end).replace(" .", ".");
    }

    @Test
    public void entityServiceArrayReturnStubsLookupAndNeverCallsFindAll() throws Exception {
        String target = generate(ENTITY_ZDL, "io.zenwave360.example.inventory", "entity-array");
        String impl = source(target, INVENTORY_IMPL);
        String body = method(impl, "releaseStock");

        Assertions.assertTrue(body.contains("// TODO CUSTOM_REQUIRED: load the StockReservation(s) this transition applies to, e.g. by a correlation key in"), body);
        Assertions.assertTrue(body.contains("List<StockReservation> stockReservations = List.of();"), body);
        Assertions.assertTrue(body.contains("stockReservations.forEach(existingStockReservation -> {"), body);
        Assertions.assertTrue(body.contains("StockReservationTransitions.ensureCanReleaseStock(existingStockReservation);"), body);
        Assertions.assertTrue(body.contains("inventoryServiceMapper.update(existingStockReservation, input);"), body);
        Assertions.assertTrue(body.contains("existingStockReservation.setStatus(ReservationStatus.RELEASED);"), body);
        Assertions.assertTrue(body.contains("stockReservations = stockReservationRepository.saveAll(stockReservations);"), body);
        Assertions.assertTrue(body.contains("stockReservations.forEach(eventSource -> {"), body);
        Assertions.assertTrue(body.contains("eventsMapper.asStockReleased(eventSource)"), body);
        Assertions.assertTrue(body.contains("eventPublisher.onStockReleased("), body);
        Assertions.assertTrue(body.contains("return stockReservations;"), body);
        Assertions.assertFalse(body.contains("findAll"), body);
        Assertions.assertFalse(body.contains("new StockReservation"), body);
        // order: from-check, update, to-state, save, events
        Assertions.assertTrue(body.indexOf("ensureCanReleaseStock") < body.indexOf("update(existing"), body);
        Assertions.assertTrue(body.indexOf("update(existing") < body.indexOf("setStatus"), body);
        Assertions.assertTrue(body.indexOf("setStatus") < body.indexOf("saveAll"), body);
        Assertions.assertTrue(body.indexOf("saveAll") < body.indexOf("eventPublisher"), body);
    }

    @Test
    public void entityServiceSingleAndOptionalReturnUseEmptyOptionalAndLifecycleChain() throws Exception {
        String target = generate(ENTITY_ZDL, "io.zenwave360.example.inventory", "entity-single");
        String impl = source(target, INVENTORY_IMPL);

        String single = method(impl, "confirmOne");
        Assertions.assertTrue(single.contains("// TODO CUSTOM_REQUIRED: load the StockReservation this transition applies to"), single);
        Assertions.assertTrue(single.contains("Optional<StockReservation> foundStockReservation = Optional.empty();"), single);
        Assertions.assertTrue(single.contains("foundStockReservation.map(existingStockReservation -> {"), single);
        Assertions.assertTrue(single.contains("StockReservationTransitions.ensureCanConfirmOne(existingStockReservation);"), single);
        Assertions.assertTrue(single.contains("existingStockReservation.setStatus(ReservationStatus.CONFIRMED);"), single);
        Assertions.assertTrue(single.contains(".map(stockReservationRepository::save).orElseThrow();"), single);
        Assertions.assertTrue(single.contains("return stockReservation;"), single);
        Assertions.assertFalse(single.contains("findAll") || single.contains("findById") || single.contains("new StockReservation"), single);

        String optional = method(impl, "confirmMaybe");
        Assertions.assertTrue(optional.contains("Optional<StockReservation> foundStockReservation = Optional.empty();"), optional);
        Assertions.assertTrue(optional.contains(".map(stockReservationRepository::save);"), optional);
        Assertions.assertFalse(optional.contains("orElseThrow"), optional);
        Assertions.assertFalse(optional.contains("findAll") || optional.contains("new StockReservation"), optional);
    }

    @Test
    public void entityServiceWithoutIdGeneratedProjectCompiles() throws Exception {
        String target = generate(ENTITY_ZDL, "io.zenwave360.example.inventory", "entity-compiles");
        Assertions.assertEquals(0, MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", target));
    }

    @Test
    public void aggregateServiceLooksUpAggregateInsteadOfNewInstance() throws Exception {
        String target = generate(AGGREGATE_ZDL, "io.zenwave360.example.orders", "aggregate");
        String impl = source(target, ORDERS_IMPL);

        String body = method(impl, "cancelOrder");
        Assertions.assertTrue(body.contains("// TODO CUSTOM_REQUIRED: load the OrderAggregate this transition applies to, e.g. by a correlation key in"), body);
        Assertions.assertTrue(body.contains("var orderAggregate = Optional.<OrderAggregate>empty().orElseThrow();"), body);
        Assertions.assertFalse(body.contains("new OrderAggregate()"), body);
        // everything after the lookup follows unchanged
        Assertions.assertTrue(body.contains("var rootEntity = orderAggregate.getRootEntity();"), body);
        Assertions.assertTrue(body.contains("CustomerOrderAggregateTransitions.ensureCanCancelOrder(rootEntity);"), body);
        Assertions.assertTrue(body.contains("orderAggregate.cancelOrder(input);"), body);
        Assertions.assertTrue(body.contains("customerOrderRepository.save(result.aggregateRoot());"), body);

        // creation (no from-transition) still starts from a new aggregate
        Assertions.assertTrue(method(impl, "createOrder").contains("var orderAggregate = new OrderAggregate();"));

        Assertions.assertEquals(0, MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", target));
    }

    @Test
    public void methodsWithAnIdAreUnchanged() throws Exception {
        String entityTarget = generate(ENTITY_ZDL, "io.zenwave360.example.inventory", "entity-with-id");
        String entityImpl = source(entityTarget, INVENTORY_IMPL);

        String withInput = method(entityImpl, "releaseReservation");
        Assertions.assertTrue(withInput.contains("var stockReservation = stockReservationRepository.findById(id).map(existingStockReservation -> {"), withInput);
        Assertions.assertTrue(withInput.contains(".map(stockReservationRepository::save).orElseThrow();"), withInput);
        Assertions.assertFalse(withInput.contains("TODO"), withInput);

        String noInput = method(entityImpl, "confirmReservation");
        Assertions.assertTrue(noInput.contains("stockReservationRepository.findById(id).map(existingStockReservation -> {"), noInput);
        Assertions.assertFalse(noInput.contains("TODO"), noInput);
        Assertions.assertFalse(noInput.contains("Optional.empty()"), noInput);

        String aggregateTarget = generate(AGGREGATE_ZDL, "io.zenwave360.example.orders", "aggregate-with-id");
        String withId = method(source(aggregateTarget, ORDERS_IMPL), "cancelOrderById");
        Assertions.assertTrue(withId.contains("var orderAggregate = customerOrderRepository.findOrderAggregateById(id).orElseThrow();"), withId);
        Assertions.assertFalse(withId.contains("TODO CUSTOM_REQUIRED"), withId);
    }

    @Test
    public void plannerAndTemplateAgreeOnTheBranchForEveryMethodShape() throws Exception {
        Map<String, Object> parsed = new ZDLParser().withZdlFile(ENTITY_ZDL).parse();
        Map<String, Object> zdl = (Map<String, Object>) new ZDLProcessor().process(parsed).get("zdl");
        String impl = source(generate(ENTITY_ZDL, "io.zenwave360.example.inventory", "agreement"), INVENTORY_IMPL);

        // planner case -> marker the template branch renders
        var expectedMarkers = Map.of(
                MethodBodyCase.ENTITY_TRANSITION_LOOKUP_LIST, "stockReservations = List.of();",
                MethodBodyCase.ENTITY_TRANSITION_LOOKUP_REQUIRED, "Optional.empty() ",
                MethodBodyCase.ENTITY_TRANSITION_LOOKUP_OPTIONAL, "Optional.empty() ",
                MethodBodyCase.ENTITY_LIFECYCLE_UPDATE_REQUIRED, "stockReservationRepository.findById(id)",
                MethodBodyCase.ENTITY_TRANSITION_REQUIRED, "stockReservationRepository.findById(id)");
        int lookups = 0;
        for (String methodName : new String[] { "releaseStock", "confirmOne", "confirmMaybe", "releaseReservation", "confirmReservation" }) {
            Map<String, Object> method = JSONPath.get(zdl, "$.services.InventoryService.methods." + methodName);
            Map<String, Object> entity = JSONPath.get(zdl, "$.entities." + method.get("entity"));
            Map<String, Object> returnEntity = JSONPath.get(zdl, "$.allEntitiesAndEnums." + method.get("returnType"));
            var plan = ServiceMethodBodyPlanner.planEntityMethodBody(method, entity, returnEntity);
            String body = method(impl, methodName);
            String expected = expectedMarkers.get(plan.bodyCase());
            Assertions.assertNotNull(expected, methodName + " planned as unexpected case " + plan.bodyCase());
            Assertions.assertTrue(body.contains(expected.trim()),
                    methodName + " planned as " + plan.bodyCase() + " but template rendered: " + body);
            boolean planned = plan.bodyCase().name().startsWith("ENTITY_TRANSITION_LOOKUP");
            Assertions.assertEquals(planned, body.contains("TODO CUSTOM_REQUIRED: load the StockReservation"), methodName);
            if (planned) {
                lookups++;
            }
        }
        Assertions.assertEquals(3, lookups);
    }
}
