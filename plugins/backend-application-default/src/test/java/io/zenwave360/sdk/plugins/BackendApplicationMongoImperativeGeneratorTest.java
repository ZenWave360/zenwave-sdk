package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.testutils.MavenCompiler;
import nl.altindag.log.LogCaptor;

public class BackendApplicationMongoImperativeGeneratorTest {

    private static LogCaptor logCaptor = LogCaptor.forRoot();

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forRoot();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @Test
    public void test_generator_mongodb_customer_address_multimodule() throws Exception {
        String targetFolder = "target/zdl/test_generator_mongodb_customer_address_multimodule";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("templates", "new " + BackendApplicationMultiModuleProjectTemplates.class.getName())
                .withOption("mavenModulesPrefix", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: customer-address-domain/src/main/java/io/zenwave360/example/core/domain/Customer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: customer-address-core-impl/src/main/java/io/zenwave360/example/core/implementation/CustomerServiceImpl.java"));
    }

    @Test
    public void test_generator_simple_packaging_mongodb_customer_address() throws Exception {
        String targetFolder = "target/zdl/test_generator_simple_packaging_mongodb_customer_address";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("layout", "SimpleDomainProjectLayout")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_mongodb_order_faults_attachments() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_mongodb_order_faults_attachments";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_mongodb_orders_with_aggregate() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_mongodb_orders_with_aggregate";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_mongodb_orders_with_aggregate_events() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_mongodb_orders_with_aggregate_events";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        // Verify aggregate result records are generated
        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.stream().anyMatch(l -> l.contains("CustomerOrderAggregate.java")));

        // Verify generated aggregate has result records
        var aggregateFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerOrderAggregate.java");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerOrderAggregateTransitions.java");
        Assertions.assertTrue(aggregateFile.exists(), "Aggregate file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Aggregate transitions file should exist");
        var aggregateContent = new String(java.nio.file.Files.readAllBytes(aggregateFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));
        Assertions.assertTrue(aggregateContent.contains("public record CreateOrderResult("),
                "Should contain CreateOrderResult record");
        Assertions.assertTrue(aggregateContent.contains("Optional<OrderEvent>"),
                "Should contain Optional<OrderEvent>");
        Assertions.assertTrue(aggregateContent.contains("Optional<OrderStatusUpdated>"),
                "Should contain Optional<OrderStatusUpdated>");
        Assertions.assertTrue(aggregateContent.contains("public CreateOrderResult createOrder("),
                "Command should return typed result");
        Assertions.assertFalse(aggregateContent.contains("List<Object> events"),
                "Should not have generic events list");

        // Verify service uses typed results and explicit publishing
        var serviceFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/implementation/OrdersServiceImpl.java");
        Assertions.assertTrue(serviceFile.exists(), "Service file should exist");
        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        Assertions.assertTrue(serviceContent.contains("var result = customerOrderAggregate."),
                "Service should capture typed result");
        Assertions.assertTrue(serviceContent.contains("result.aggregateRoot()"),
                "Service should access aggregateRoot from result");
        Assertions.assertTrue(serviceContent.contains(".ifPresent(it -> eventsProducer."),
                "Service should publish events via ifPresent");
        Assertions.assertFalse(serviceContent.contains("persistAndEmitEvents"),
                "Service should NOT have persistAndEmitEvents");
        Assertions.assertFalse(serviceContent.contains("instanceof"),
                "Service should NOT use instanceof for events");

        // Note: Maven compilation with includeEmitEventsImplementation=true requires
        // the AsyncAPI-generated OrdersEventsProducer interface which is not available
        // in this standalone test. The includeEmitEventsImplementation=false test above
        // verifies compilation.
    }

    @Test
    public void test_generator_hexagonal_mongodb_orders_with_aggregate_state_machine() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_mongodb_orders_with_aggregate_state_machine";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var aggregateFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerOrderAggregate.java");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerOrderAggregateTransitions.java");
        Assertions.assertTrue(aggregateFile.exists(), "Aggregate file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Aggregate transitions file should exist");
        var aggregateContent = new String(java.nio.file.Files.readAllBytes(aggregateFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));

        // Verify direct field initialization is used (no initializeStateIfNeeded)
        Assertions.assertFalse(aggregateContent.contains("initializeStateIfNeeded"),
                "Should NOT contain initializeStateIfNeeded method");
        Assertions.assertTrue(aggregateContent.contains("OrderStatus.RECEIVED"),
                "Should set initial state RECEIVED");
        Assertions.assertTrue(aggregateContent.contains("rootEntity.setStatus(OrderStatus.RECEIVED)"),
                "Should directly set initial state in constructor");

        Assertions.assertFalse(aggregateContent.contains("private void requireState("),
                "Aggregate should not contain inline requireState() helper");
        Assertions.assertTrue(aggregateContent.contains("CustomerOrderAggregateTransitions.ensureCanCancelOrder(rootEntity)"),
                "Commands with 'from' should call explicit aggregate transitions");
        Assertions.assertTrue(transitionsContent.contains("public final class CustomerOrderAggregateTransitions"),
                "Should generate the aggregate transitions class");
        Assertions.assertTrue(transitionsContent.contains("public static void ensureCanCancelOrder(CustomerOrder entity)"),
                "Should generate a typed transition method");
        Assertions.assertTrue(transitionsContent.contains("class InvalidStateTransitionException extends IllegalStateException"),
                "Transitions class should declare a nested InvalidStateTransitionException");
        Assertions.assertTrue(transitionsContent.contains("entity.getStatus()"),
                "Transitions should derive current state from the lifecycle field");
        Assertions.assertTrue(transitionsContent.contains("entity.getId()"),
                "Transitions should include the entity id in failures");

        // Verify state transitions (to) are generated
        Assertions.assertTrue(aggregateContent.contains("rootEntity.setStatus(OrderStatus."),
                "Commands with 'to' should set status");
        Assertions.assertTrue(aggregateContent.contains("OrderStatus.CANCELLED"),
                "Should contain CANCELLED transition");

        // Verify multiple from states are generated in the transitions helper
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.RECEIVED"),
                "cancelOrder should have RECEIVED from state");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.KITCHEN_ACCEPTED"),
                "cancelOrder should have KITCHEN_ACCEPTED from state");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.KITCHEN_IN_PROGRESS"),
                "cancelOrder should have KITCHEN_IN_PROGRESS from state");

        // Verify createOrder (no from/to) doesn't have requireState
        int createOrderIdx = aggregateContent.indexOf("public CreateOrderResult createOrder(");
        int firstRequireState = aggregateContent.indexOf("ensureCan", createOrderIdx);
        int nextMethodIdx = aggregateContent.indexOf("public UpdateOrderResult updateOrder(", createOrderIdx);
        Assertions.assertTrue(firstRequireState > nextMethodIdx || firstRequireState == -1 || firstRequireState > nextMethodIdx,
                "createOrder should not call transition validation before updateOrder");

        // Verify compilation
        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_mongodb_orders_with_lifecycle_entity() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_mongodb_orders_with_lifecycle_entity";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-lifecycle-entity.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var serviceFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/implementation/OrderServiceImpl.java");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/OrderTransitions.java");
        Assertions.assertTrue(serviceFile.exists(), "Service impl file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Service transitions file should exist");
        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));

        Assertions.assertFalse(serviceContent.contains("private <T> void requireState("),
                "Service should not contain inline requireState() helper");
        Assertions.assertTrue(transitionsContent.contains("public final class OrderTransitions"),
                "Should generate the service transitions class");
        Assertions.assertTrue(transitionsContent.contains("public static void ensureCanPlaceOrder(Order entity)"),
                "Should generate typed service transition methods");
        Assertions.assertTrue(transitionsContent.contains("class InvalidStateTransitionException extends IllegalStateException"),
                "Transitions class should declare a nested InvalidStateTransitionException");
        Assertions.assertTrue(transitionsContent.contains("entity.getStatus()"),
                "Transitions should derive current state from the lifecycle field");
        Assertions.assertTrue(transitionsContent.contains("entity.getId()"),
                "Transitions should include the entity id in failures");

        // Verify placeOrder calls explicit transitions with DRAFT state
        int placeOrderIdx = serviceContent.indexOf("placeOrder(");
        int placeOrderEnd = serviceContent.indexOf("public ", placeOrderIdx + 1);
        if (placeOrderEnd == -1) placeOrderEnd = serviceContent.length();
        String placeOrderSection = serviceContent.substring(placeOrderIdx, placeOrderEnd);
        Assertions.assertTrue(placeOrderSection.contains("OrderTransitions.ensureCanPlaceOrder(existingOrder)"),
                "placeOrder should call explicit service transitions");
        Assertions.assertTrue(placeOrderSection.contains(".setStatus(OrderStatus.PLACED)"),
                "placeOrder should set status to PLACED");

        // Verify confirmOrder has multiple from states
        int confirmOrderIdx = serviceContent.indexOf("confirmOrder(");
        int confirmOrderEnd = serviceContent.indexOf("public ", confirmOrderIdx + 1);
        if (confirmOrderEnd == -1) confirmOrderEnd = serviceContent.length();
        String confirmOrderSection = serviceContent.substring(confirmOrderIdx, confirmOrderEnd);
        Assertions.assertTrue(transitionsContent.contains("ensureCanConfirmOrder(Order entity)"),
                "Transitions should include confirmOrder validation");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.PLACED"),
                "confirmOrder should have PLACED from state");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.CONFIRMED"),
                "confirmOrder should have CONFIRMED from state");
        Assertions.assertTrue(confirmOrderSection.contains(".setStatus(OrderStatus.KITCHEN_IN_PROGRESS)"),
                "confirmOrder should set status to KITCHEN_IN_PROGRESS");

        // Verify cancelOrder has 3 from states
        int cancelOrderIdx = serviceContent.indexOf("cancelOrder(");
        int cancelOrderEnd = serviceContent.indexOf("public ", cancelOrderIdx + 1);
        if (cancelOrderEnd == -1) cancelOrderEnd = serviceContent.length();
        String cancelOrderSection = serviceContent.substring(cancelOrderIdx, cancelOrderEnd);
        Assertions.assertTrue(transitionsContent.contains("ensureCanCancelOrder(Order entity)"),
                "Transitions should include cancelOrder validation");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.DRAFT"),
                "cancelOrder should have DRAFT from state");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.PLACED"),
                "cancelOrder should have PLACED from state");
        Assertions.assertTrue(transitionsContent.contains("OrderStatus.CONFIRMED"),
                "cancelOrder should have CONFIRMED from state");
        Assertions.assertTrue(cancelOrderSection.contains(".setStatus(OrderStatus.CANCELLED)"),
                "cancelOrder should set status to CANCELLED");

        // Verify createOrder (no from/to) does NOT call requireState
        int createOrderIdx = serviceContent.indexOf("createOrder(");
        int createOrderEnd = serviceContent.indexOf("public ", createOrderIdx + 1);
        String createOrderSection = serviceContent.substring(createOrderIdx, createOrderEnd);
        Assertions.assertFalse(createOrderSection.contains("ensureCan"),
                "createOrder should NOT call transition validation (no from states)");

        // Verify compilation
        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

}
