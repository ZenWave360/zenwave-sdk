package io.zenwave360.sdk.plugins;

import java.util.List;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

public class SpringCloudStreamsWithDtosGeneratorTest {

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
    public void test_generate_asyncapi_avro_from_classpath() throws Exception {
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("classpath:asyncapi-avro/asyncapi.yml")
                .withTargetFolder("target/out/asyncapi_avro_from_classpath")
                .withOption("avroCompilerProperties.imports", "classpath:asyncapi-avro/avro")
                .withOption("modelPackage", "io.example.consumers.model")
                .withOption("producerApiPackage", "io.example.consumers.provider.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }

    @Test
    public void test_generate_asyncapi_avro_from_file() throws Exception {
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("src/test/resources/asyncapi-avro/asyncapi.yml")
                .withTargetFolder("target/out/asyncapi_avro_from_file")
                .withOption("avroCompilerProperties.imports", "src/test/resources/asyncapi-avro/avro")
                .withOption("modelPackage", "io.example.consumers.model")
                .withOption("producerApiPackage", "io.example.consumers.provider.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }

    @Test
    public void test_generate_asyncapi_avro_from_http() throws Exception {
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("src/test/resources/asyncapi-avro/asyncapi.yml")
                .withTargetFolder("target/out/asyncapi_avro_from_file")
                .withOption("avroCompilerProperties.imports", "src/test/resources/asyncapi-avro/avro")
                .withOption("modelPackage", "io.example.consumers.model")
                .withOption("producerApiPackage", "io.example.consumers.provider.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);
    }


}
