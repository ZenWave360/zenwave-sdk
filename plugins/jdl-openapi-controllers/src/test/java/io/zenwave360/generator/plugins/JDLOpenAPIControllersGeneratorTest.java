package io.zenwave360.generator.plugins;

import java.util.List;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.options.ProgrammingStyle;
import org.junit.jupiter.api.*;

import io.zenwave360.generator.MainGenerator;
import nl.altindag.log.LogCaptor;

public class JDLOpenAPIControllersGeneratorTest {

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
    public void test_generator_jdl_openapi_controllers() throws Exception {
        Plugin plugin = new JDLOpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml")
                .withOption("jdlFile", "classpath:io/zenwave360/generator/resources/jdl/petstore.jdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.web.api")
                .withOption("openApiModelPackage", "io.zenwave360.example.web.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder("target/out");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    @Disabled
    public void test_generator_jdl_openapi_controllers_registry() throws Exception {
        Plugin plugin = new JDLOpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/openapi-orders.yml")
                .withOption("jdlFile", "classpath:io/zenwave360/generator/resources/jdl/orders-model.jdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.adapters.web")
                .withOption("openApiModelPackage", "io.zenwave360.example.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder("target/examples/spring-boot-mongo-elasticsearch");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    @Disabled
    public void test_generator_jdl_openapi_controllers_relational() throws Exception {
        Plugin plugin = new JDLOpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/openapi-orders-relational.yml")
                .withOption("jdlFile", "classpath:io/zenwave360/generator/resources/jdl/orders-model-relational.jdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.adapters.web")
                .withOption("openApiModelPackage", "io.zenwave360.example.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder("target/examples/spring-boot-jpa-elasticsearch");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }


    @Test
    // @Disabled
    public void test_generator_jdl_openapi_controllers_registry_no_jdl() throws Exception {
        Plugin plugin = new JDLOpenAPIControllersPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml")
                // .withOption("jdlFile", "../../examples/spring-boot-mongo-elasticsearch/src/main/resources/model/orders-model.jdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("controllersPackage", "io.zenwave360.example.nojdl.adapters.web")
                .withOption("openApiApiPackage", "io.zenwave360.example.adapters.web")
                .withOption("openApiModelPackage", "io.zenwave360.example.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder("target/examples/spring-boot-mongo-elasticsearch");

        new MainGenerator().generate(plugin);

        List<String> logs = logCaptor.getLogs();
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        // Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

}
