package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.*;

public class OpenAPIControllersKotlinGeneratorTest {

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
    public void test_generator_openapi_controllers_zdl_customer_address() throws Exception {
        String targetFolder = "target/projects/kustomer-address-jpa-web";

        Plugin plugin = new OpenAPIControllersPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml")
                .withOption("zdlFile", "classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl")

                .withOption("templates", "new " + OpenAPIControllersKotlinTemplates.class.getName())

                .withOption("basePackage", "io.zenwave360.examples.kotlin")
                .withOption("openApiApiPackage", "io.zenwave360.examples.kotlin.web")
                .withOption("openApiModelPackage", "io.zenwave360.examples.kotlin.web.dtos")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
//                .withOption("haltOnFailFormatting", false)
                .withTargetFolder(targetFolder);

        new MainGenerator().generate(plugin);

    }
}
