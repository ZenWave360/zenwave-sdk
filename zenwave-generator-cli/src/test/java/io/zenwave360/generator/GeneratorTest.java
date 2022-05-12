package io.zenwave360.generator;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.plugins.GeneratorPlugin;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.DefaultTemplateWriter;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
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
                .withSpecFile("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpPluginGenerator.class, DefaultTemplateWriter.class);

        new Generator(configuration).generate();

        logCaptor.getLogs();
    }

    public static class NoOpPluginGenerator implements GeneratorPlugin {

        @Override
        public List<TemplateOutput> generate(Map<String, Object> apiModel) {
            return List.of(new TemplateOutput("nop.txt", "nop"));
        }
    }
}
