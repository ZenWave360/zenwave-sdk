package io.zenwave360.sdk.plugins;

import java.util.List;
import java.io.File;

import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import nl.altindag.log.LogCaptor;

import static org.junit.jupiter.api.Assertions.fail;

public class SpringCloudStreamsWithAvroDtosGeneratorTest {

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
        String targetFolder = "target/out/asyncapi_avro_from_classpath";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("classpath:asyncapi-avro/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("avroCompilerProperties.imports", "classpath:asyncapi-avro/avro")
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/CustomerEvent.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/Address.java").exists());
    }

    @Test
    public void test_generate_asyncapi_avro_from_classpath_with_excludes() throws Exception {
        String targetFolder = "target/out/asyncapi_avro_from_classpath_with_excludes";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("classpath:asyncapi-avro/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("avroCompilerProperties.imports", "classpath:asyncapi-avro/avro")
                .withOption("avroCompilerProperties.excludes", List.of("**/*Event.avsc"))
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/CustomerEvent.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/Address.java").exists());
    }

    @Test
    public void  test_generate_asyncapi_avro_from_classpath_with_excludes_incomplete() throws Exception {
        String targetFolder = "target/out/asyncapi_avro_from_classpath_with_excludes_incomplete";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("classpath:asyncapi-avro/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("avroCompilerProperties.imports", "classpath:asyncapi-avro/avro")
                .withOption("avroCompilerProperties.excludes", List.of("**/Address.avsc"))
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        try {
            new MainGenerator().generate(plugin);
            fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof org.apache.avro.AvroTypeException);
            Assertions.assertTrue(e.getMessage().contains("Undefined schema: io.example.producer.api.avro.Address"));
        }
    }


    @Test
    public void test_generate_asyncapi_avro_from_file() throws Exception {
        String targetFolder = "target/out/asyncapi_avro_from_file";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile("src/test/resources/asyncapi-avro/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("avroCompilerProperties.imports", "src/test/resources/asyncapi-avro/avro")
                // .withOption("modelPackage", "io.example.consumers.model")
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/CustomerEvent.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/Address.java").exists());
    }

    @Test
    public void test_generate_asyncapi_avro_from_http() throws Exception {
        String targetFolder = "target/out/asyncapi_avro_from_http";
        String baseUrl = "https://raw.githubusercontent.com/ZenWave360/zenwave-sdk/refs/heads/feature/asyncapi-spring-cloud-stream-with-avro-json/plugins/asyncapi-spring-cloud-streams-with-avro-json/src/test/resources/";
        Plugin plugin = new SpringCloudStreamsWithDtosPlugin()
                .withApiFile(baseUrl + "asyncapi-avro/asyncapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("avroCompilerProperties.imports", List.of(
                        baseUrl + "asyncapi-avro/avro/Address.avsc",
                        baseUrl + "asyncapi-avro/avro/PaymentMethod.avsc",
                        baseUrl + "asyncapi-avro/avro/PaymentMethodType.avsc"))
                .withOption("producerApiPackage", "io.example.producer.api")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("skipFormatting", false);

        new MainGenerator().generate(plugin);

        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/CustomerEvent.java").exists());
        Assertions.assertTrue(new File(targetFolder + "/src/main/java/io/example/producer/api/avro/Address.java").exists());
    }


}
