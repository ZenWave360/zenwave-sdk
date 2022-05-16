package io.zenwave360.generator;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.writers.DefaultTemplateWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

public class MainTest {

    @Test
    public void testMain() {
        List<String> processors = List.of(DefaultYamlParser.class, AsyncApiProcessor.class, NoOpPluginGenerator.class, DefaultTemplateWriter.class)
                .stream().map(c -> c.getName()).collect(Collectors.toList());

        Main.main(
                "-c", StringUtils.join(processors, ","),
                "-o", "specFile=classpath:io/zenwave360/generator/parsers/asyncapi-circular-refs.yml",
                "-o", "targetFolder=target/zenwave/out",
                "-o", "inner.specFile=target/zenwave/out",
                "-o", "inner.targetFolder=target/zenwave/out"
        );
    }
}
