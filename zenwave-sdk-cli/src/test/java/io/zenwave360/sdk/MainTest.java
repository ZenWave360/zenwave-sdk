package io.zenwave360.sdk;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.plugins.NoOpGenerator;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;

public class MainTest {

    @Test
    public void testMain() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "specFile=classpath:io/zenwave360/sdk/resources/asyncapi/asyncapi-circular-refs.yml",
                "targetFolder=target/zenwave/out",
                "inner.specFile=target/zenwave/out",
                "inner.targetFolder=target/zenwave/out");
    }

    @Test
    public void testMainWithMultipleSpecFiles() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "0.specFile=classpath:io/zenwave360/sdk/resources/asyncapi/asyncapi-circular-refs.yml",
                "2.specFile=classpath:io/zenwave360/sdk/resources/asyncapi/asyncapi-circular-refs.yml",
                "0.targetProperty=asyncapi1",
                "1.targetProperty=asyncapi1",
                "2.targetProperty=asyncapi2",
                "3.targetProperty=asyncapi2",
                "targetFolder=target/zenwave/out",
                "inner.specFile=target/zenwave/out",
                "inner.targetFolder=target/zenwave/out");

        Assertions.assertTrue(NoOpGenerator.context != null);
        Assertions.assertTrue(NoOpGenerator.context.get("asyncapi1") != null);
        Assertions.assertTrue(NoOpGenerator.context.get("asyncapi2") != null);
        Assertions.assertNotSame(NoOpGenerator.context.get("asyncapi2"), NoOpGenerator.context.get("asyncapi1"));
    }

    @Test
    public void testMain_with_array_options() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpGenerator.class, TemplateFileWriter.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "specFile=classpath:io/zenwave360/sdk/resources/asyncapi/asyncapi-circular-refs.yml",
                "targetFolder=target/zenwave/out",
                "inner.specFile=target/zenwave/out",
                "inner.targetFolder=target/zenwave/out",
                "array=one,two");
    }
}
