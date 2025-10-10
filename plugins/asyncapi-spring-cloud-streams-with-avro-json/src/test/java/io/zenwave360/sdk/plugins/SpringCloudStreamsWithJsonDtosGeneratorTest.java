package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

import static org.junit.jupiter.api.Assertions.fail;

public class SpringCloudStreamsWithJsonDtosGeneratorTest {

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
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("classpath:asyncapi-v3.yml")
                .withTargetFolder("target/out/asyncapi_json_from_classpath")
                .withOption("modelPackage", "io.example.producer.api.model")
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }

    @Test
    public void test_generate_asyncapi_json_from_file() throws Exception {
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("src/test/resources/asyncapi-v3.yml")
                .withTargetFolder("target/out/asyncapi_json_from_file")
                .withOption("modelPackage", "io.example.producer.api.model")
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }

    @Test
    public void test_generate_asyncapi_json_from_http() throws Exception {
        String baseUrl = "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/refs/heads/feature/asyncapi-spring-cloud-stream-with-avro-json/plugins/asyncapi-spring-cloud-streams-with-avro-json/src/test/resources/";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile(baseUrl + "asyncapi-v3.yml")
                .withTargetFolder("target/out/asyncapi_json_from_http")
                 .withOption("modelPackage", "io.example.producer.api.model")
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }
}
