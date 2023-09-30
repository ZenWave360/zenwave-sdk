package io.zenwave360.sdk;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.plugins.NoOpGenerator;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import nl.altindag.log.LogCaptor;

public class MainGeneratorTest {

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
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class)
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }

    @Test
    public void testGeneratorWithMultipleFiles() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withTargetFolder("target/zenwave630/out")
                .withOption("0.specFile", "classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withOption("1.specFile", "classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml")
                .withOption("2.specFile", "classpath:io/zenwave360/sdk/resources/jdl/21-points.jh")
                .withOption("0.targetProperty", "asyncapi")
                .withOption("1.targetProperty", "openapi")
                .withOption("2.targetProperty", "zdl")
                .withOption("3.targetProperty", "asyncapi")
                .withOption("4.targetProperty", "openapi")
                .withChain(DefaultYamlParser.class, DefaultYamlParser.class, ZDLParser.class, AsyncApiProcessor.class, OpenApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class);

        new MainGenerator().generate(plugin);

        Map<String, Object> contextModel = NoOpGenerator.context;

        logCaptor.getLogs();
    }

}
