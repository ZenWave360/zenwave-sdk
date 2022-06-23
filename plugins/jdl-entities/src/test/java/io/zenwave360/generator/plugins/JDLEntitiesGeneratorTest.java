package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.Generator;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JDLEntitiesGeneratorTest {

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

    private Map<String, ?> loadJDLModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, ?> model = new JDLParser().withSpecFile(file.getAbsolutePath()).parse();
        return new JDLProcessor().process(model);
    }

    @Test
    public void test_entities() throws Exception {
        Map<String, ?> model = loadJDLModelFromResource("io/zenwave360/generator/resources/jdl/orders-model.jdl");
        JDLEntitiesGenerator generator = new JDLEntitiesGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);


        for (TemplateOutput outputTemplate : outputTemplates) {
            System.out.println(" ----------- " + outputTemplate.getTargetFile());
            System.out.println(outputTemplate.getContent());
        }
        outputTemplates = new JavaFormatter().format(outputTemplates);
    }

    @Test
    public void test_generator_hexagonal_mongodb_imperative() throws Exception {
        Configuration configuration = new JDLEntitiesConfigurationPreset()
                .withSpecFile("classpath:io/zenwave360/generator/resources/jdl/orders-model.jdl")
                .withTargetFolder("target/out")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", JDLEntitiesGenerator.PersistenceType.mongodb)
                .withOption("style", JDLEntitiesGenerator.ProgrammingStyle.imperative)
                ;

        new Generator(configuration).generate();

        List<String> logs = logCaptor.getLogs();
//        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
//        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    @Disabled
    public void test_generator_hexagonal_mongodb_imperative_registry() throws Exception {
        Configuration configuration = new JDLEntitiesConfigurationPreset()
                .withSpecFile("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\api-registry.jdl")
                .withTargetFolder("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy")
                .withOption("basePackage", "io.zenwave360.registry")
                .withOption("persistence", JDLEntitiesGenerator.PersistenceType.mongodb)
                .withOption("style", JDLEntitiesGenerator.ProgrammingStyle.imperative)
                ;

        new Generator(configuration).generate();

        List<String> logs = logCaptor.getLogs();
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

}
