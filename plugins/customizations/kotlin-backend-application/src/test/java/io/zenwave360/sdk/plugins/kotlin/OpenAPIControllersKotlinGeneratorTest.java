package io.zenwave360.sdk.plugins.kotlin;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.testutils.MavenCompiler;
import io.zenwave360.sdk.utils.JSONPath;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.net.URI;
import java.util.List;
import java.util.Map;

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

    @Test
    public void test_asMethodParametersInitializer() throws Exception {
        OpenAPIControllersKotlinHelpers helpers = new OpenAPIControllersKotlinHelpers("", "DTO");

        // Test with empty operation
        Map<String, Object> emptyOperation = Map.of();
        CharSequence result = helpers.asMethodParametersInitializer(emptyOperation, null);
        Assertions.assertEquals("", result.toString());

        // Test with operation containing parameters
        Map<String, Object> operation = Map.of(
            "parameters", List.of(
                Map.of(
                    "name", "id",
                    "schema", Map.of("type", "integer")
                ),
                Map.of(
                    "name", "name",
                    "schema", Map.of("type", "string")
                )
            )
        );

        result = helpers.asMethodParametersInitializer(operation, null);
        String resultStr = result.toString();

        Assertions.assertTrue(resultStr.contains("val id: Int = 0"));
        Assertions.assertTrue(resultStr.contains("val name: String = \"\""));
    }

    @Test
    public void test_asMethodParametersInitializer_uploadDocument() throws Exception {
        // Load the documents OpenAPI spec
        Map<String, Object> model = new DefaultYamlParser()
            .withApiFile(URI.create("classpath:apis/documents-openapi.yml"))
            .withTargetProperty("api")
            .parse();

        Map<String, Object> processedModel = new OpenApiProcessor().process(model);
        Map<String, Object> api = (Map<String, Object>) processedModel.get("api");

        // Get the uploadDocument operation using JSONPath
        Map<String, Object> uploadOperation = JSONPath.get(api, "$.paths['/documents/upload'].post");

        OpenAPIControllersKotlinHelpers helpers = new OpenAPIControllersKotlinHelpers("", "DTO");

        var options = Mockito.mock(Options.class);
        Mockito.when(options.get("openapi")).thenReturn(api);
        CharSequence result = helpers.asMethodParametersInitializer(uploadOperation, options);
        String resultStr = result.toString();

        // Verify the generated parameter initialization for uploadDocument
        Assertions.assertNotNull(result);
        // The uploadDocument operation should generate appropriate parameter initialization
        System.out.println("Generated parameters: \n" + resultStr);
    }
}
