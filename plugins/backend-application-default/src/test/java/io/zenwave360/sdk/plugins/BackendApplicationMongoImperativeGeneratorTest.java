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
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/")
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

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
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

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
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
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

}
