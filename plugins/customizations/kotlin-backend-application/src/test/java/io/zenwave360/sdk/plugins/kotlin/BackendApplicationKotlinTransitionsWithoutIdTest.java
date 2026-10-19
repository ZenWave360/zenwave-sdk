package io.zenwave360.sdk.plugins.kotlin;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;

/** Spec sdk-service-transitions (Kotlin): what is generated for a from-transition without an id parameter. */
class BackendApplicationKotlinTransitionsWithoutIdTest {

    private static final String INVENTORY_IMPL = "src/main/kotlin/io/zenwave360/example/inventory/core/application/InventoryServiceImpl.kt";
    private static final String ORDERS_IMPL = "src/main/kotlin/io/zenwave360/example/orders/core/application/OrdersServiceImpl.kt";

    @TempDir
    Path tempDir;

    private Path generate(String zdl, String basePackage, String name) throws Exception {
        Path targetFolder = tempDir.resolve(name);
        Plugin plugin = (Plugin) Class.forName("io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin")
                .getConstructor()
                .newInstance();
        new MainGenerator().generate(plugin
                .withZdlFile(zdl)
                .withTargetFolder(targetFolder.toString())
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("skipFormatting", true)
                .withOption("haltOnFailFormatting", false));
        return targetFolder;
    }

    /** Whitespace-normalised body of one function of a generated Kotlin implementation. */
    private static String function(Path target, String file, String name) throws Exception {
        String normalized = Files.readString(target.resolve(file)).replaceAll("\\s+", " ");
        int start = normalized.indexOf("fun " + name + "(");
        Assertions.assertTrue(start >= 0, "missing function " + name);
        int end = normalized.indexOf(" override fun ", start + 1);
        return normalized.substring(start, end < 0 ? normalized.length() : end);
    }

    @Test
    void kotlinEntityServiceArrayReturnStubsLookupAndNeverCallsFindAll() throws Exception {
        Path target = generate("classpath:zdl/transitions-without-id-entity.zdl", "io.zenwave360.example.inventory", "entity-array");
        String body = function(target, INVENTORY_IMPL, "releaseStock");

        Assertions.assertTrue(body.contains("// TODO CUSTOM_REQUIRED: load the StockReservation(s) this transition applies to, e.g. by a correlation key in the input"), body);
        Assertions.assertTrue(body.contains("var stockReservations: List<StockReservation> = emptyList()"), body);
        Assertions.assertTrue(body.contains("stockReservations.forEach { existingStockReservation ->"), body);
        Assertions.assertTrue(body.contains("StockReservationTransitions.ensureCanReleaseStock(existingStockReservation)"), body);
        Assertions.assertTrue(body.contains("inventoryServiceMapper.update(existingStockReservation, input)"), body);
        Assertions.assertTrue(body.contains("existingStockReservation.status = ReservationStatus.RELEASED"), body);
        Assertions.assertTrue(body.contains("stockReservations = stockReservationRepository.saveAll(stockReservations)"), body);
        Assertions.assertTrue(body.contains("stockReservations.forEach { eventSource ->"), body);
        Assertions.assertTrue(body.contains("eventsMapper.asStockReleased(eventSource)"), body);
        Assertions.assertTrue(body.contains("return stockReservations"), body);
        Assertions.assertFalse(body.contains("findAll"), body);
        Assertions.assertFalse(body.contains("StockReservation()"), body);
        Assertions.assertTrue(body.indexOf("ensureCanReleaseStock") < body.indexOf("saveAll"), body);
        Assertions.assertTrue(body.indexOf("saveAll") < body.indexOf("eventPublisher"), body);
    }

    @Test
    void kotlinEntityServiceSingleAndOptionalReturnUseNullLookupAndLifecycleChain() throws Exception {
        Path target = generate("classpath:zdl/transitions-without-id-entity.zdl", "io.zenwave360.example.inventory", "entity-single");

        String single = function(target, INVENTORY_IMPL, "confirmOne");
        Assertions.assertTrue(single.contains("// TODO CUSTOM_REQUIRED: load the StockReservation this transition applies to"), single);
        Assertions.assertTrue(single.contains("val foundStockReservation: StockReservation? = emptyList<StockReservation>().firstOrNull()"), single);
        Assertions.assertTrue(single.contains("return foundStockReservation ?.let { existingStockReservation ->"), single);
        Assertions.assertTrue(single.contains("StockReservationTransitions.ensureCanConfirmOne(existingStockReservation)"), single);
        Assertions.assertTrue(single.contains("existingStockReservation.status = ReservationStatus.CONFIRMED"), single);
        Assertions.assertTrue(single.contains("?.let { stockReservationRepository.save(it) }"), single);
        Assertions.assertTrue(single.contains("?: throw NoSuchElementException(\"StockReservation not found\")"), single);
        Assertions.assertFalse(single.contains("findAll") || single.contains("findById") || single.contains("StockReservation()"), single);

        String optional = function(target, INVENTORY_IMPL, "confirmMaybe");
        Assertions.assertTrue(optional.contains("val foundStockReservation: StockReservation? = emptyList<StockReservation>().firstOrNull()"), optional);
        Assertions.assertTrue(optional.contains("?.let { stockReservationRepository.save(it) }"), optional);
        Assertions.assertFalse(optional.contains("throw NoSuchElementException"), optional);
    }

    @Test
    void kotlinAggregateServiceLooksUpAggregateInsteadOfNewInstance() throws Exception {
        Path target = generate("classpath:zdl/transitions-without-id-aggregate.zdl", "io.zenwave360.example.orders", "aggregate");

        String body = function(target, ORDERS_IMPL, "cancelOrder");
        Assertions.assertTrue(body.contains("// TODO CUSTOM_REQUIRED: load the OrderAggregate this transition applies to, e.g. by a correlation key in the input"), body);
        Assertions.assertTrue(body.contains("val orderAggregate: OrderAggregate = emptyList<OrderAggregate>().firstOrNull() ?: throw NoSuchElementException(\"OrderAggregate not found\")"), body);
        Assertions.assertFalse(body.contains("OrderAggregate()"), body);
        Assertions.assertTrue(body.contains("val rootEntity = orderAggregate.getRootEntity()"), body);
        Assertions.assertTrue(body.contains("CustomerOrderAggregateTransitions.ensureCanCancelOrder(rootEntity)"), body);
        Assertions.assertTrue(body.contains("orderAggregate.cancelOrder(input)"), body);

        Assertions.assertTrue(function(target, ORDERS_IMPL, "createOrder").contains("val orderAggregate = OrderAggregate()"));
    }

    @Test
    void kotlinMethodsWithAnIdAreUnchanged() throws Exception {
        Path entityTarget = generate("classpath:zdl/transitions-without-id-entity.zdl", "io.zenwave360.example.inventory", "entity-with-id");
        String withInput = function(entityTarget, INVENTORY_IMPL, "releaseReservation");
        Assertions.assertTrue(withInput.contains("return stockReservationRepository.findByIdOrNull(id) ?.let { existingStockReservation ->"), withInput);
        Assertions.assertTrue(withInput.contains("?: throw NoSuchElementException(\"StockReservation not found with id: $id\")"), withInput);
        Assertions.assertFalse(withInput.contains("TODO"), withInput);

        String noInput = function(entityTarget, INVENTORY_IMPL, "confirmReservation");
        Assertions.assertTrue(noInput.contains("val existingStockReservation = stockReservationRepository.findByIdOrNull(id)"), noInput);
        Assertions.assertFalse(noInput.contains("TODO"), noInput);

        Path aggregateTarget = generate("classpath:zdl/transitions-without-id-aggregate.zdl", "io.zenwave360.example.orders", "aggregate-with-id");
        String withId = function(aggregateTarget, ORDERS_IMPL, "cancelOrderById");
        Assertions.assertTrue(withId.contains("orderAggregate = customerOrderRepository.findOrderAggregateById(id)"), withId);
        Assertions.assertFalse(withId.contains("TODO CUSTOM_REQUIRED"), withId);
    }
}
