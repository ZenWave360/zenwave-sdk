package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.testutils.MavenCompiler;

public class BackendApplicationJpaImperativeGeneratorTest {

    @Test
    public void test_generator_hexagonal_jpa() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_jpa_customer_address";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/Customer.java").exists());
    }

    @Test
    public void test_generator_maps_id() throws Exception {
        String targetFolder = "target/zdl/test_generator_maps_id";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-one-to-one-maps-id.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var addressFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/Address.java");
        Assertions.assertTrue(addressFile.exists());
        Assertions.assertTrue(new String(java.nio.file.Files.readAllBytes(addressFile.toPath())).contains("@MapsId"));
    }

    @Test
    public void test_generator_hexagonal_jpa_orders_with_aggregate_state_machine() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_jpa_orders_with_aggregate_state_machine";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
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
        Assertions.assertFalse(aggregateContent.contains("private void requireState("),
                "Aggregate should not contain inline requireState() helper");
        Assertions.assertTrue(aggregateContent.contains("CustomerOrderAggregateTransitions.ensureCanCancelOrder(rootEntity)"),
                "Aggregate should call explicit transitions");
        Assertions.assertTrue(transitionsContent.contains("public static void ensureCanCancelOrder(CustomerOrder entity)"),
                "Should generate typed aggregate transition methods");
        Assertions.assertTrue(transitionsContent.contains("entity.getStatus()"),
                "Transitions should derive current state from the lifecycle field");
    }

    @Test
    public void test_generator_hexagonal_jpa_aggregate_and_entity_lifecycle() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_jpa_aggregate_and_entity_lifecycle";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-aggregate-and-entity-lifecycle.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var aggregateFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerAggregate.java");
        var aggregateTransitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/CustomerAggregateTransitions.java");
        Assertions.assertTrue(aggregateFile.exists(), "Aggregate file should exist");
        Assertions.assertTrue(aggregateTransitionsFile.exists(), "Aggregate transitions file should exist");

        var aggregateContent = new String(java.nio.file.Files.readAllBytes(aggregateFile.toPath()));
        var aggregateTransitionsContent = new String(java.nio.file.Files.readAllBytes(aggregateTransitionsFile.toPath()));
        Assertions.assertFalse(aggregateContent.contains("private void requireState("),
                "Aggregate should not contain inline requireState() helper");
        Assertions.assertTrue(aggregateContent.contains("CustomerAggregateTransitions.ensureCanActivateCustomer(rootEntity)"),
                "Aggregate should call explicit transitions");
        Assertions.assertTrue(aggregateTransitionsContent.contains("public static void ensureCanActivateCustomer(Customer entity)"),
                "Should generate typed aggregate transition methods");
        Assertions.assertTrue(aggregateTransitionsContent.contains("entity.getStatus()"),
                "Aggregate transitions should derive current state from the lifecycle field");

        var serviceFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/application/AddressServiceImpl.java");
        var entityTransitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/AddressTransitions.java");
        Assertions.assertTrue(serviceFile.exists(), "Address service impl file should exist");
        Assertions.assertTrue(entityTransitionsFile.exists(), "Address transitions file should exist");

        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        var entityTransitionsContent = new String(java.nio.file.Files.readAllBytes(entityTransitionsFile.toPath()));
        Assertions.assertFalse(serviceContent.contains("private <T> void requireState("),
                "Service should not contain inline requireState() helper");
        Assertions.assertTrue(serviceContent.contains("AddressTransitions.ensureCanVerifyAddress(existingAddress)"),
                "Service should call explicit transitions");
        Assertions.assertTrue(entityTransitionsContent.contains("public static void ensureCanVerifyAddress(Address entity)"),
                "Should generate typed service transition methods");
        Assertions.assertTrue(entityTransitionsContent.contains("entity.getStatus()"),
                "Entity transitions should derive current state from the lifecycle field");

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_jpa_orders_with_lifecycle_entity() throws Exception {
        String targetFolder = "target/zdl/test_generator_hexagonal_jpa_orders_with_lifecycle_entity";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-lifecycle-entity.zdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var serviceFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/application/OrderServiceImpl.java");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/java/io/zenwave360/example/core/domain/OrderTransitions.java");
        Assertions.assertTrue(serviceFile.exists(), "Service impl file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Service transitions file should exist");

        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));
        Assertions.assertFalse(serviceContent.contains("private <T> void requireState("),
                "Service should not contain inline requireState() helper");
        Assertions.assertTrue(serviceContent.contains("OrderTransitions.ensureCanPlaceOrder(existingOrder)"),
                "Service should call explicit transitions");
        Assertions.assertTrue(transitionsContent.contains("public static void ensureCanPlaceOrder(Order entity)"),
                "Should generate typed service transition methods");
        Assertions.assertTrue(transitionsContent.contains("entity.getStatus()"),
                "Transitions should derive current state from the lifecycle field");
    }

}
