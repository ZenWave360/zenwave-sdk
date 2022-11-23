package io.zenwave360.generator.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.Plugin;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.junit.jupiter.api.*;

import io.zenwave360.generator.MainGenerator;
import io.zenwave360.generator.options.PersistenceType;
import io.zenwave360.generator.options.ProgrammingStyle;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import nl.altindag.log.LogCaptor;

public class JDLBackendApplicationJpaImperativeGeneratorTest {

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
    public void test_generator_hexagonal_jpa_imperative() throws Exception {
        String targetFolder = "target/test_generator_hexagonal_jpa_imperative";
        Plugin plugin = new JDLBackendApplicationDefaultPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/jdl/orders-model-relational.jdl")
                .withTargetFolder(targetFolder)
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("persistence", PersistenceType.jpa)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));

        compile("src/test/resources/jpa-elasticsearch-scs3-pom.xml", targetFolder);
    }

    public void compile(String pom, String baseDir) throws MavenInvocationException, IOException {
        System.out.println("Maven Invoker - compile:" + pom + " - " + baseDir);
        FileUtils.copyFile(new File(pom), new File(baseDir, "pom.xml"));

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(new File(baseDir));
        request.setGoals(Collections.singletonList("test-compile"));

        Invoker invoker = new DefaultInvoker();
        var results = invoker.execute(request);
        Assertions.assertEquals(0, results.getExitCode());
    }
}
