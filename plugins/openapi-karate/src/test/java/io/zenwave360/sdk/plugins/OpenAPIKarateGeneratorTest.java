package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;
import io.zenwave360.sdk.writers.TemplateWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static io.zenwave360.sdk.utils.NamingUtils.camelCase;

public class OpenAPIKarateGeneratorTest {


    @ParameterizedTest(name = "[{index}] {displayName} {0} {1}")
    @CsvSource({
            "openapi-petstore.yml, 'addPet,getPetById,updatePet,deletePet,getPetById'",
            "openapi-orders.yml, 'createCustomer,getCustomer,updateCustomer,deleteCustomer,getCustomer'"
    })
    public void test_output_business_flow(String openapi, String operationIds) throws Exception {
        String targetFolder = "target/test_output_business_flow_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new OpenAPIKaratePlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", OpenAPIKarateGenerator.GroupByType.businessFlow)
                .withOption("businessFlowTestName", camelCase(operationIds.replaceAll(",", "_")))
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("operationIds",  operationIds);

        plugin.addAfterInChain(OpenAPIKarateGenerator.class, CapturingTemplateWriter.class);
        plugin.addAfterInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);

        new MainGenerator().generate(plugin);

        var templateOutputList = CapturingTemplateWriter.templateOutputList;
        Assertions.assertEquals(1, templateOutputList.size());
        Assertions.assertEquals("src/test/resources/io/example/controller/tests/" + camelCase(operationIds.replaceAll(",", "_")) +".feature", templateOutputList.get(0).getTargetFile());

    }


    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApi'",
            "openapi-orders.yml, createCustomer, 'CustomerApi'"
    })
    public void test_output_by_one_service(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_one_service_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new OpenAPIKaratePlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", OpenAPIKarateGenerator.GroupByType.service)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/resources/io/example/controller/tests/" + controller + ".feature");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApi'",
            "openapi-orders.yml, createCustomer, 'CustomerApi'"
    })
    public void test_output_by_one_service_simple_domain_packaging(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_one_service_simple_domain_packaging_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new OpenAPIKaratePlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", OpenAPIKarateGenerator.GroupByType.service)
                .withOption("layout", "SimpleDomainProjectLayout")
                .withOption("basePackage", "io.example")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/resources/io/example/" + controller + ".feature");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApi'",
            "openapi-orders.yml, 'CustomerApi'"
    })
    public void test_output_by_service(String openapi, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_service_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new OpenAPIKaratePlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", OpenAPIKarateGenerator.GroupByType.service)
                .withOption("testsPackage", "io.example.controller.tests");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/resources/io/example/controller/tests/" + controller + ".feature");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApi/AddPet'",
            "openapi-orders.yml, 'CustomerOrderApi/CreateCustomerOrder'"
    })
    public void test_output_by_operation(String openapi, String controllers) throws Exception {
        String targetFolder = "target/test_output_by_operation_" + openapi.replaceAll("\\.", "_");
        Plugin plugin = new OpenAPIKaratePlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", OpenAPIKarateGenerator.GroupByType.operation)
                .withOption("testsPackage", "io.example.controller.tests");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/resources/io/example/controller/tests/" + controller + ".feature");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

    }
    public static class CapturingTemplateWriter implements TemplateWriter {
        static List<TemplateOutput> templateOutputList;
        @Override
        public void write(List<TemplateOutput> templateOutputList) {
            this.templateOutputList = templateOutputList;
        }
    }

}
