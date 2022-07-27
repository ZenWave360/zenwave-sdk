package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Configuration;
import io.zenwave360.generator.MainGenerator;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        Configuration configuration = new JDLOpenAPIControllersConfiguration()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml")
                .withOption("jdlFile", "classpath:io/zenwave360/generator/resources/jdl/petstore.jdl")
                .withOption("basePackage", "io.zenwave360.example")
                .withOption("openApiApiPackage", "io.zenwave360.example.web.api")
                .withOption("openApiModelPackage", "io.zenwave360.example.web.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
//                .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", JDLOpenAPIControllersGenerator.ProgrammingStyle.imperative)
                .withTargetFolder("target/out")
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
//        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
//        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    @Disabled
    public void test_generator_jdl_openapi_controllers_registry() throws Exception {
        Configuration configuration = new JDLOpenAPIControllersConfiguration()
                .withSpecFile("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\openapi.yml")
                .withOption("jdlFile", "C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\api-registry.jdl")
                .withOption("basePackage", "io.zenwave360.registry")
                .withOption("openApiApiPackage", "io.zenwave360.registry.adapters.web")
                .withOption("openApiModelPackage", "io.zenwave360.registry.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                //                .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", JDLOpenAPIControllersGenerator.ProgrammingStyle.imperative)
                .withTargetFolder("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy")
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

    @Test
    @Disabled
    public void test_generator_jdl_openapi_controllers_registry_no_jdl() throws Exception {
        Configuration configuration = new JDLOpenAPIControllersConfiguration()
                .withSpecFile("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\openapi.yml")
//                .withOption("jdlFile", "C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy\\src\\main\\resources\\model\\api-registry.jdl")
                .withOption("basePackage", "io.zenwave360.registry")
                .withOption("controllersPackage", "io.zenwave360.registry.nojdl.adapters.web")
                .withOption("openApiApiPackage", "io.zenwave360.registry.adapters.web")
                .withOption("openApiModelPackage", "io.zenwave360.registry.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                //                .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", JDLOpenAPIControllersGenerator.ProgrammingStyle.imperative)
                .withTargetFolder("C:\\Users\\ivan.garcia\\workspace\\zenwave\\zenwave360-registy")
                ;

        new MainGenerator().generate(configuration);

        List<String> logs = logCaptor.getLogs();
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductConsumer.java"));
        //        Assertions.assertTrue(logs.contains("Writing template with targetFile: io/example/integration/test/api/provider_for_commands_reactive/DoCreateProductService.java"));
    }

}
