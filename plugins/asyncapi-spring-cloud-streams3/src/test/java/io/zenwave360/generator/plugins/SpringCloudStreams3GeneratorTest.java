package io.zenwave360.generator.plugins;

import java.io.IOException;
import java.util.List;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.options.ProgrammingStyle;
import io.zenwave360.generator.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.generator.testutils.MavenCompiler;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.MainGenerator;
import nl.altindag.log.LogCaptor;

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
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_events")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.imperative);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/IDefaultServiceEventsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_events/DefaultServiceEventsProducer.java"));
    }

    @Test
    public void test_generator_provider_for_commands_imperative() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_imperative")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_imperative/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_imperative/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_imperative_expose_message() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_imperative_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("exposeMessage", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_imperative_expose_message/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_imperative_expose_message/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_reactive() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_reactive")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.reactive);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_reactive/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_provider_for_commands_reactive_expose_message() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.provider_for_commands_reactive_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.provider)
                .withOption("style", ProgrammingStyle.reactive)
                .withOption("exposeMessage", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_reactive_expose_message/DoCreateProductConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/provider_for_commands_reactive_expose_message/IDoCreateProductConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_commands() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_commands")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_commands/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_commands/DefaultServiceCommandsProducer.java"));
    }

    @Test
    public void test_generator_client_for_events_imperative() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_imperative")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.imperative);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_imperative/OnProductCreatedConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_imperative/IOnProductCreated2ConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_events_imperative_expose_message() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-events.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_imperative_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("exposeMessage", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_imperative_expose_message/OnProductCreatedConsumer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_imperative_expose_message/IOnProductCreatedConsumerService.java"));
    }

    @Test
    public void test_generator_client_for_events_reactive() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_reactive")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.reactive);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_reactive/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_reactive/DefaultServiceCommandsProducer.java"));
    }

    @Test
    public void test_generator_client_for_events_reactive_expose_message() throws Exception {
        Plugin plugin = new SpringCloudStream3Plugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-commands.yml")
                .withTargetFolder("target/zenwave630/out")
                .withOption("apiPackage", "io.example.integration.test.api.client_for_events_reactive_expose_message")
                .withOption("modelPackage", "io.example.integration.test.api.model")
                .withOption("role", AsyncapiRoleType.client)
                .withOption("style", ProgrammingStyle.reactive)
                .withOption("exposeMessage", true);

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_reactive_expose_message/IDefaultServiceCommandsProducer.java"));
        Assertions.assertTrue(logs.contains("Writing template with targetFile: src/main/java/io/example/integration/test/api/client_for_events_reactive_expose_message/DefaultServiceCommandsProducer.java"));
    }

}
