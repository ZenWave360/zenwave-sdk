package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

public class BackendApplicationKotlinGeneratorTest {

    @Test
    public void generatesCallsTodoInServiceImplementation() throws Exception {
        String targetFolder = "target/projects/kotlin-calls-todo";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-calls.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var serviceImpl = java.nio.file.Path.of(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/application/OrdersServiceImpl.kt");
        String source = java.nio.file.Files.readString(serviceImpl);
        Assertions.assertTrue(source.contains(
                "// TODO: call CatalogInventoryZdl InventoryService.reserveStock(ReserveStockInput) -> StockReservedResult"));
        Assertions.assertTrue(source.contains("//       then emit one of: OrderCreated | StockUnavailable"));
    }

    @Test
    public void test_generator_kotlin_project_jpa() throws Exception {
        String targetFolder = "target/projects/kustomer-address-jpa";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/Customer.kt").exists());
    }

    @Test
    public void generatesTestDataOnlyForAggregates(@TempDir java.nio.file.Path tempDir) throws Exception {
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder(tempDir.toString())
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("includeEmitEventsImplementation", false)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(java.nio.file.Files.exists(
                tempDir.resolve("src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/Address.kt")),
                "Embedded domain types should still be generated");
        Assertions.assertTrue(java.nio.file.Files.exists(
                tempDir.resolve("src/test/resources/data/jpa/Customer/1.json")));
        Assertions.assertFalse(java.nio.file.Files.exists(
                tempDir.resolve("src/test/resources/data/jpa/Address/1.json")),
                "Embedded types should not get standalone test-data fixtures");
    }

    @Test
    public void test_generator_kotlin_project_with_events_uses_kotlin_nullability() throws Exception {
        String targetFolder = "target/projects/kustomer-events-jpa";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var serviceFile = java.nio.file.Path.of(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/application/CustomerServiceImpl.kt");
        var serviceContent = java.nio.file.Files.readString(serviceFile);
        Assertions.assertTrue(serviceContent.contains("eventsMapper.asCustomerEvent(it)"));
        Assertions.assertFalse(serviceContent.contains(".isPresent()"));
        Assertions.assertFalse(serviceContent.contains(".get()"));

        var eventsMapperFile = serviceFile.resolveSibling("mappers").resolve("EventsMapper.kt");
        var eventsMapperContent = java.nio.file.Files.readString(eventsMapperFile);
        Assertions.assertTrue(eventsMapperContent.contains("fun asCustomerDeleted(id: Long?):"));
    }

    @Test
    @Disabled("Mongo compilation is not covered by e2e for Kotlin; enable locally to verify persistence-specific templates")
    public void test_generator_kotlin_project_mongodb() throws Exception {
        String targetFolder = "target/projects/kustomer-address-mongodb";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_kotlin_project_modulith() throws Exception {
        String targetFolder = "target/projects/kustomer-address-mongodb-modulith";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("layout.commonPackage", "io.zenwave360.example.kotlin.common")
                .withOption("layout.moduleBasePackage", "io.zenwave360.example.kotlin.customer")
                .withOption("useSpringModulith", true)
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var moduleInfo = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/example/kotlin/customer/ModuleMetadata.kt");
        Assertions.assertTrue(moduleInfo.exists());
        Assertions.assertTrue(new String(java.nio.file.Files.readAllBytes(moduleInfo.toPath()))
                .contains("@ApplicationModule"));
    }

    @Test
    public void test_generator_kotlin_project_mongodb_lifecycle_transitions() throws Exception {
        String targetFolder = "target/projects/kotlin-orders-mongodb-lifecycle-transitions";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "orders")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var aggregateFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/CustomerOrderAggregate.kt");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/CustomerOrderAggregateTransitions.kt");
        Assertions.assertTrue(aggregateFile.exists(), "Aggregate file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Aggregate transitions file should exist");

        var aggregateContent = new String(java.nio.file.Files.readAllBytes(aggregateFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));
        Assertions.assertFalse(aggregateContent.contains("private fun requireState("),
                "Aggregate should not contain inline requireState() helper");
        Assertions.assertTrue(aggregateContent.contains("CustomerOrderAggregateTransitions.ensureCanCancelOrder(rootEntity)"),
                "Aggregate should call explicit transitions");
        Assertions.assertTrue(transitionsContent.contains("object CustomerOrderAggregateTransitions"),
                "Should generate the aggregate transitions object");
        Assertions.assertTrue(transitionsContent.contains("fun ensureCanCancelOrder(entity: CustomerOrder)"),
                "Should generate typed aggregate transition methods");
        Assertions.assertTrue(transitionsContent.contains("val current = entity.status"),
                "Transitions should derive current state from the lifecycle field");
    }

    @Test
    public void test_generator_kotlin_project_jpa_aggregate_and_entity_lifecycle() throws Exception {
        String targetFolder = "target/projects/kotlin-customer-address-jpa-lifecycle";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-aggregate-and-entity-lifecycle.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "customer-address")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var aggregateFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/CustomerAggregate.kt");
        var aggregateTransitionsFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/CustomerAggregateTransitions.kt");
        Assertions.assertTrue(aggregateFile.exists(), "Aggregate file should exist");
        Assertions.assertTrue(aggregateTransitionsFile.exists(), "Aggregate transitions file should exist");

        var aggregateContent = new String(java.nio.file.Files.readAllBytes(aggregateFile.toPath()));
        var aggregateTransitionsContent = new String(java.nio.file.Files.readAllBytes(aggregateTransitionsFile.toPath()));
        Assertions.assertFalse(aggregateContent.contains("private fun requireState("),
                "Aggregate should not contain inline requireState() helper");
        Assertions.assertTrue(aggregateContent.contains("CustomerAggregateTransitions.ensureCanActivateCustomer(rootEntity)"),
                "Aggregate should call explicit transitions");
        Assertions.assertTrue(aggregateTransitionsContent.contains("object CustomerAggregateTransitions"),
                "Should generate the aggregate transitions object");
        Assertions.assertTrue(aggregateTransitionsContent.contains("fun ensureCanActivateCustomer(entity: Customer)"),
                "Should generate typed aggregate transition methods");
        Assertions.assertTrue(aggregateTransitionsContent.contains("val current = entity.status"),
                "Aggregate transitions should derive current state from the lifecycle field");

        var serviceFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/application/AddressServiceImpl.kt");
        var entityTransitionsFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/AddressTransitions.kt");
        Assertions.assertTrue(serviceFile.exists(), "Address service impl file should exist");
        Assertions.assertTrue(entityTransitionsFile.exists(), "Address transitions file should exist");

        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        var entityTransitionsContent = new String(java.nio.file.Files.readAllBytes(entityTransitionsFile.toPath()));
        Assertions.assertFalse(serviceContent.contains("private fun <T> requireState("),
                "Service should not contain inline requireState() helper");
        Assertions.assertTrue(serviceContent.contains("AddressTransitions.ensureCanVerifyAddress(existingAddress)"),
                "Service should call explicit transitions");
        Assertions.assertTrue(entityTransitionsContent.contains("object AddressTransitions"),
                "Should generate the service transitions object");
        Assertions.assertTrue(entityTransitionsContent.contains("fun ensureCanVerifyAddress(entity: Address)"),
                "Should generate typed service transition methods");
        Assertions.assertTrue(entityTransitionsContent.contains("val current = entity.status"),
                "Entity transitions should derive current state from the lifecycle field");

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }
}
