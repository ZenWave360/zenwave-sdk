package io.zenwave360.sdk;

import java.util.Map;

import io.zenwave360.sdk.generators.ZDLProjectGenerator;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;
import io.zenwave360.sdk.zdl.ProjectTemplates;
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
                .withLayout("LayeredProjectLayout")
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class)
                .withOption("basePackage", "io.zenwave360.sdk")
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }

    @Test
    public void testGeneratorWithConfigurationProvider() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withLayout("LayeredProjectLayout")
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, ZDLParser.class, NoOpGenerator.class, TemplateFileWriter.class)
                .withOption("basePackage", "io.zenwave360.sdk")
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }


    @Test
    public void testGeneratorWithStdout() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateStdoutWriter.class)
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }

    @Test
    public void testGeneratorWithMultipleFiles() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withTargetFolder("target/zenwave630/out")
                .withOption("0.apiFile", "classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withOption("1.apiFile", "classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml")
                .withOption("2.zdlFile", "classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
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


    @Test
    public void testZdlProjectGenerator() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withLayout("LayeredProjectLayout")
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withTargetFolder("target/zenwave630/out/customer-address")
                .withChain(ZDLParser.class, ZDLProcessor.class, ZDLProjectGenerator.class)
                .withOption("basePackage", "io.zenwave360.sdk")
                .withOption("style", "imperative")
                .withOption("persistence", "mongodb")
                .withOption("includeEmitEventsImplementation", false)
                .withOption("templates", new ProjectTemplates())
                .withOption("forceOverwrite", true);

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }


    @Test
    public void testCustomJacksonDeserializers() throws Exception {
        // File file = new File(getClass().getClassLoader().getResource("io/zenwave360/sdk/parsers/asyncapi-circular-refs.yml").toURI());
        Plugin plugin = new Plugin()
                .withLayout("LayeredProjectLayout")
                .withApiFile("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml")
                .withTargetFolder("target/zenwave630/out")
                .withChain(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class)
                .withOption("basePackage", "io.zenwave360.sdk")
                .withOption("forceOverwrite", true)
                .withOption("array", "el1,el2")
                .withOption("templates", "new " + ProjectTemplates.class.getName())
                ;

        new MainGenerator().generate(plugin);

        logCaptor.getLogs();
    }
}
