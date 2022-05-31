package io.zenwave360.generator;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class GeneratorTest {

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
    public void testGenerator() throws Exception {
//        File file = new File(getClass().getClassLoader().getResource("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml").toURI());
        Configuration configuration = new Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpPluginGenerator.class, TemplateFileWriter.class);

        new Generator(configuration).generate();

        logCaptor.getLogs();
    }

    @Test
    public void testGeneratorWithMultipleFiles() throws Exception {
        //        File file = new File(getClass().getClassLoader().getResource("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml").toURI());
        Configuration configuration = new Configuration()
                .withTargetFolder("target/zenwave630/out")
                .withOption("0.specFile", "classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml")
                .withOption("1.specFile", "classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml")
                .withOption("2.specFile", "classpath:io/zenwave360/generator/resources/jdl/21-points.jh")
                .withOption("0.targetProperty", "asyncapi")
                .withOption("1.targetProperty", "openapi")
                .withOption("2.targetProperty", "jdl")
                .withOption("3.targetProperty", "asyncapi")
                .withOption("4.targetProperty", "openapi")
                .withChain(DefaultYamlParser.class, DefaultYamlParser.class, JDLParser.class, AsyncApiProcessor.class, OpenApiProcessor.class, NoOpPluginGenerator.class, TemplateFileWriter.class);

        new Generator(configuration).generate();

        Map<String, ?> contextModel = NoOpPluginGenerator.context;

        logCaptor.getLogs();
    }

}
