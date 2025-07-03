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
}
