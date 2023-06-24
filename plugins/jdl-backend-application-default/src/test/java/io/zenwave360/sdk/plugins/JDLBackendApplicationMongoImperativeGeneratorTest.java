package io.zenwave360.sdk.plugins;

import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.formatters.JavaFormatter;
import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import nl.altindag.log.LogCaptor;

public class JDLBackendApplicationMongoImperativeGeneratorTest {

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
    public void test_entities() throws Exception {
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        JDLBackendApplicationDefaultGenerator generator = new JDLBackendApplicationDefaultGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);

        for (TemplateOutput outputTemplate : outputTemplates) {
            System.out.println(" ----------- " + outputTemplate.getTargetFile());
            System.out.println(outputTemplate.getContent());
        }
        outputTemplates = new JavaFormatter().format(outputTemplates);
    }

    @Test
    public void test_generator_hexagonal_mongodb_imperative() throws Exception {
        String targetFolder = "target/jdl/test_generator_hexagonal_mongodb_imperative";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    public void test_generator_hexagonal_mongodb_imperative_no_dtos() throws Exception {
        String targetFolder = "target/jdl/test_generator_hexagonal_mongodb_imperative_no_dtos";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("inputDTOSuffix", "")
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }


    @Test
    // @Disabled
    public void test_generator_hexagonal_mongodb_imperative_registry() throws Exception {
        String targetFolder = "target/jdl/test_generator_hexagonal_mongodb_imperative_registry";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }

    @Test
    // @Disabled
    public void test_generator_hexagonal_mongodb_imperative_registry_only_some_entities() throws Exception {
        String targetFolder = "target/jdl/test_generator_hexagonal_mongodb_imperative_registry_only_some_entities";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("entities", List.of("BaseEntity", "Customer"))
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        int exitCode = MavenCompiler.compile("src/test/resources/mongodb-elasticsearch-scs3-pom.xml", targetFolder);
        Assertions.assertEquals(0, exitCode);
    }
}
