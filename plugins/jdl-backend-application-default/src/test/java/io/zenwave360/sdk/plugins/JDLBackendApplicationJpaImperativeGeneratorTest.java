package io.zenwave360.sdk.plugins;

import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import nl.altindag.log.LogCaptor;

public class JDLBackendApplicationJpaImperativeGeneratorTest {

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

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        return new JDLProcessor().process(model);
    }

    @Test
    public void test_generator_hexagonal_jpa_imperative() throws Exception {
        String targetFolder = "target/test_generator_hexagonal_jpa_imperative";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("databaseType", DatabaseType.mariadb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/jpa-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_jpa_imperative_no_dtos() throws Exception {
        String targetFolder = "target/test_generator_hexagonal_jpa_imperative_no_dtos";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("databaseType", DatabaseType.mariadb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("inputDTOSuffix", "")
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/jpa-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }
}
