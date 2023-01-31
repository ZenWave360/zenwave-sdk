package io.zenwave360.generator.plugins;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.MainGenerator;
import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.testutils.MavenCompiler;
import io.zenwave360.generator.writers.TemplateWriter;
import org.junit.jupiter.api.Assertions;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RestAssuredGeneratorTest {

    private static final String OPENAPI_RESOURCES = "../../../../zenwave-sdk-test-resources/src/main/resources/io/zenwave360/generator/resources/openapi/";

    private Map<String, Object> loadApiModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).parse();
        return new OpenApiProcessor().process(model);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet",
            "openapi-orders.yml, getCustomer",
            "openapi-orders-relational.yml, createCustomerOrder"
    })
    public void test_output_partial_one_operation(String openapi, String operationId) throws Exception {
        Plugin plugin = new RestAssuredPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/" + openapi)
                .withOption("groupBy", RestAssuredGenerator.GroupByType.partial)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("operationIds",  List.of(operationId));

        plugin.addAfterInChain(RestAssuredGenerator.class, CapturingTemplateWriter.class);

        new MainGenerator().generate(plugin);

        var templateOutputList = CapturingTemplateWriter.templateOutputList;
        Assertions.assertEquals(1, templateOutputList.size());
        Assertions.assertEquals("io/example/controller/tests/Operation.java", templateOutputList.get(0).getTargetFile());
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApiIT,ControllersTestSet'",
            "openapi-orders.yml, createCustomer, 'CustomerApiIT,ControllersTestSet'"
    })
    public void test_output_by_one_service(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_one_service_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new RestAssuredPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder + "/src/test/java")
                .withOption("groupBy", RestAssuredGenerator.GroupByType.service)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/io/example/controller/tests/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

        int exitCode = MavenCompiler.compile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApiIT,ControllersTestSet'",
            "openapi-orders.yml, 'CustomerApiIT,ControllersTestSet'"
    })
    public void test_output_by_service(String openapi, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_service_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new RestAssuredPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder + "/src/test/java")
                .withOption("groupBy", RestAssuredGenerator.GroupByType.service)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/io/example/controller/tests/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

        int exitCode = MavenCompiler.compile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApi/AddPetIT,ControllersTestSet'",
            "openapi-orders.yml, 'CustomerOrderApi/CreateCustomerOrderIT,ControllersTestSet'"
    })
    public void test_output_by_operation(String openapi, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_operation_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new RestAssuredPlugin()
                .withSpecFile("classpath:io/zenwave360/generator/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder + "/src/test/java")
                .withOption("groupBy", RestAssuredGenerator.GroupByType.operation)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/io/example/controller/tests/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

        int exitCode = MavenCompiler.compile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
        Assertions.assertEquals(0, exitCode);
    }
    public static class CapturingTemplateWriter implements TemplateWriter {
        static List<TemplateOutput> templateOutputList;
        @Override
        public void write(List<TemplateOutput> templateOutputList) {
            this.templateOutputList = templateOutputList;
        }
    }

}
