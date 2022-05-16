package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.Generator;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.DefaultTemplateWriter;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SpringCloudStreams3GeneratorTest {

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
        Configuration configuration = new SpringCloudStream3ConfigurationPreset()
                .withSpecFile("classpath:io/zenwave360/generator/plugins/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", "CLIENT")
                .withOption("style", "REACTIVE");

        new Generator(configuration).generate();

        logCaptor.getLogs();
    }
}
