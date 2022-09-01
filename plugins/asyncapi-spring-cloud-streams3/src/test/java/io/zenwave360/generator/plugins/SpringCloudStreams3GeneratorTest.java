package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.MainGenerator;
import io.zenwave360.generator.generators.AbstractAsyncapiGenerator;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SpringCloudStreams3GeneratorTest {

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
    public void test_generator_provider_for_events() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_events")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.PROVIDER)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.IMPERATIVE)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/IDefaultServiceEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_events/DefaultServiceEventsProducer.java"));
    }

    @Test
    public void test_generator_provider_for_commands_imperative() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_imperative")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.PROVIDER)
        ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_imperative/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_imperative/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_imperative_expose_message() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_imperative_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.PROVIDER)
                .withOption("exposeMessage", true)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_imperative_expose_message/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_imperative_expose_message/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_reactive() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_reactive")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.PROVIDER)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.REACTIVE)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_reactive_expose_message() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_reactive_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.PROVIDER)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.REACTIVE)
                .withOption("exposeMessage", true)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive_expose_message/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive_expose_message/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_commands() throws Exception {
         Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_commands")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.CLIENT)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_commands/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_commands/DefaultServiceCommandsProducer.java"));
    }

    @Test
    public void test_generator_client_for_events_imperative() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_imperative")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.CLIENT)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.IMPERATIVE)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_imperative/OnProductCreatedConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_imperative/IOnProductCreated2ConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_events_imperative_expose_message() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_imperative_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.CLIENT)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.IMPERATIVE)
                .withOption("exposeMessage", true)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_imperative_expose_message/OnProductCreatedConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_imperative_expose_message/IOnProductCreatedConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_events_reactive() throws Exception {
         Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_reactive")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.CLIENT)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.REACTIVE)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_reactive/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_reactive/DefaultServiceCommandsProducer.java"));
    }

    @Test
    public void test_generator_client_for_events_reactive_expose_message() throws Exception {
        Configuration configuration = new SpringCloudStream3Configuration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_reactive_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AbstractAsyncapiGenerator.RoleType.CLIENT)
                .withOption("style", SpringCloudStreams3Generator.ProgrammingStyle.REACTIVE)
                .withOption("exposeMessage", true)
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_reactive_expose_message/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/client_for_events_reactive_expose_message/DefaultServiceCommandsProducer.java"));
    }
}
