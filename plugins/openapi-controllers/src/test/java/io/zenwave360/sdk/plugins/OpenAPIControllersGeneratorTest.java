package io.zenwave360.sdk.plugins;

import java.util.List;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import nl.altindag.log.LogCaptor;

public class OpenAPIControllersGeneratorTest {

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
    public void test_generator_openapi_controllers_no_zdl() throws Exception {
        Plugin plugin = new OpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/openapi/oas-controllers-with-no-zdl.yml")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.web.api")
                .withOption("openApiModelPackage", "io.zenwave360.example.web.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                //                .withOption("haltOnFailFormatting", false)
                .withTargetFolder("target/out/oas-controllers-with-no-zdl");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    public void test_generator_openapi_controllers_zdl_customer_address() throws Exception {
        Plugin plugin = new OpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml")
                .withOption("zdlFile", "classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.web.api")
                .withOption("openApiModelPackage", "io.zenwave360.example.web.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
//                .withOption("haltOnFailFormatting", false)
                .withTargetFolder("target/out/customer_address");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    public void test_generator_openapi_controllers_zdl_customer_address_simple_domain_packaging() throws Exception {
        Plugin plugin = new OpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml")
                .withOption("zdlFile", "classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("simpleDomainPackaging", true)
                .withOption("openApiApiPackage", "io.zenwave360.example.web.api")
                .withOption("openApiModelPackage", "io.zenwave360.example.web.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                //                .withOption("haltOnFailFormatting", false)
                .withTargetFolder("target/out/customer_address_simple_domain_packaging");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }


}
