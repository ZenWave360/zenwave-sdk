package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.*;

public class BackendApplicationKotlinGeneratorTest {

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

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
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

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
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

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_kotlin_project_jpa_lifecycle_transitions() throws Exception {
        String targetFolder = "target/projects/kotlin-orders-jpa-lifecycle-transitions";
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/orders-with-lifecycle-entity.zdl")
                .withTargetFolder(targetFolder)
                .withOption("templates", "new " + BackendApplicationKotlinTemplates.class.getName())
                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("projectName", "orders")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        var serviceFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/implementation/OrderServiceImpl.kt");
        var transitionsFile = new java.io.File(targetFolder,
                "src/main/kotlin/io/zenwave360/examples/kotlin/core/domain/OrderTransitions.kt");
        Assertions.assertTrue(serviceFile.exists(), "Service impl file should exist");
        Assertions.assertTrue(transitionsFile.exists(), "Service transitions file should exist");

        var serviceContent = new String(java.nio.file.Files.readAllBytes(serviceFile.toPath()));
        var transitionsContent = new String(java.nio.file.Files.readAllBytes(transitionsFile.toPath()));
        Assertions.assertFalse(serviceContent.contains("private fun <T> requireState("),
                "Service should not contain inline requireState() helper");
        Assertions.assertTrue(serviceContent.contains("OrderTransitions.ensureCanPlaceOrder(existingOrder)"),
                "Service should call explicit transitions");
        Assertions.assertTrue(transitionsContent.contains("object OrderTransitions"),
                "Should generate the service transitions object");
        Assertions.assertTrue(transitionsContent.contains("fun ensureCanPlaceOrder(entity: Order)"),
                "Should generate typed service transition methods");
        Assertions.assertTrue(transitionsContent.contains("val current = entity.status"),
                "Transitions should derive current state from the lifecycle field");

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/jpa-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }
}
