package io.zenwave360.generator;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

public class MainTest {

    @Test
    public void testMain() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpPluginGenerator.class, TemplateFileWriter.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "specFile=classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml",
                "targetFolder=target/zenwave/out",
                "inner.specFile=target/zenwave/out",
                "inner.targetFolder=target/zenwave/out"
        );
    }

    @Test
    public void testMainWithMultipleSpecFiles() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, DefaultYamlParser.class, AsyncApiProcessor.class, NoOpPluginGenerator.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "0.specFile=classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml",
                "2.specFile=classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml",
                "0.targetProperty=asyncapi1",
                "1.targetProperty=asyncapi1",
                "2.targetProperty=asyncapi2",
                "3.targetProperty=asyncapi2",
                "targetFolder=target/zenwave/out",
                "inner.specFile=target/zenwave/out",
                "inner.targetFolder=target/zenwave/out"
        );

        Assertions.assertTrue(NoOpPluginGenerator.context != null);
        Assertions.assertTrue(NoOpPluginGenerator.context.get("asyncapi1") != null);
        Assertions.assertTrue(NoOpPluginGenerator.context.get("asyncapi2") != null);
        Assertions.assertNotSame(NoOpPluginGenerator.context.get("asyncapi2"), NoOpPluginGenerator.context.get("asyncapi1"));
    }
}
