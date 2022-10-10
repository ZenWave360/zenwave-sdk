package io.zenwave360.generator.plugins;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.invoker.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.MainGenerator;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import nl.altindag.log.LogCaptor;

public class JDLBackendApplicationDefaultGeneratorTest {

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
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/generator/resources/jdl/orders-model.jdl");
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
        Configuration configuration = new JDLBackendApplicationDefaultConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/jdl/orders-model.jdl")
                .withTargetFolder("target/out")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", JDLBackendApplicationDefaultGenerator.PersistenceType.mongodb)
                .withOption("style", JDLBackendApplicationDefaultGenerator.ProgrammingStyle.imperative);

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    // @Disabled
    public void test_generator_hexagonal_mongodb_imperative_registry() throws Exception {
        Configuration configuration = new JDLBackendApplicationDefaultConfiguration()
                .withSpecFile("../../examples/spring-boot-mongo-elasticsearch/src/main/resources/model/orders-model.jdl")
                .withTargetFolder("../../examples/spring-boot-mongo-elasticsearch")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", JDLBackendApplicationDefaultGenerator.PersistenceType.mongodb)
                .withOption("style", JDLBackendApplicationDefaultGenerator.ProgrammingStyle.imperative);

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    // @Disabled
    public void test_generator_hexagonal_mongodb_imperative_registry_only_some_entities() throws Exception {
        Configuration configuration = new JDLBackendApplicationDefaultConfiguration()
                .withSpecFile("../../examples/spring-boot-mongo-elasticsearch/src/main/resources/model/orders-model.jdl")
                .withTargetFolder("../../examples/spring-boot-mongo-elasticsearch")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", JDLBackendApplicationDefaultGenerator.PersistenceType.mongodb)
                .withOption("style", JDLBackendApplicationDefaultGenerator.ProgrammingStyle.imperative)
                .withOption("entities", List.of("Organization"));

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    public void compile() throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("/path/to/pom.xml"));
        request.setBaseDirectory(new File("/path/to/base/dir"));
        request.setGoals(Collections.singletonList("test-compile"));

        Invoker invoker = new DefaultInvoker();
        invoker.execute(request);
    }
}
