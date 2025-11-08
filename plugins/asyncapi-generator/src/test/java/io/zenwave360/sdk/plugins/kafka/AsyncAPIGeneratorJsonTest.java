package io.zenwave360.sdk.plugins.kafka;

import java.io.File;

import io.zenwave360.sdk.plugins.AsyncAPIGeneratorPlugin;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

import static org.junit.jupiter.api.Assertions.fail;

public class AsyncAPIGeneratorJsonTest {

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
    public void test_generate_asyncapi_json_from_classpath() throws Exception {
        String targetFolder = "target/out/asyncapi_json_from_classpath";
        Plugin plugin = new AsyncAPIGeneratorPlugin()
                .withApiFile("classpath:asyncapi-v3.yml")
                .withTargetFolder(targetFolder)
                .withOption("modelPackage", "io.example.api.model")
                .withOption("producerApiPackage", "io.example.api.producer")
                .withOption("consumerApiPackage", "io.example.api.consumer")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("templates", "SpringKafka")
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/api/model/CustomerEvent.java").exists());
    }

    @Test
    public void test_generate_asyncapi_json_from_file() throws Exception {
        String targetFolder = "target/out/asyncapi_json_from_file";
        Plugin plugin = new AsyncAPIGeneratorPlugin()
                .withApiFile("src/test/resources/asyncapi-v3.yml")
                .withTargetFolder(targetFolder)
                .withOption("modelPackage", "io.example.api.model")
                .withOption("producerApiPackage", "io.example.api.producer")
                .withOption("consumerApiPackage", "io.example.api.consumer")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("templates", "SpringKafka")
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/api/model/CustomerEvent.java").exists());
    }

}
