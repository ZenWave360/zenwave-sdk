package io.zenwave360.sdk.plugins;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.testutils.GitUtils;
import io.zenwave360.sdk.testutils.MavenCompiler;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;
import io.zenwave360.sdk.writers.TemplateWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.zenwave360.sdk.utils.NamingUtils.camelCase;

public class SpringWebTestClientGeneratorTest {

    private static final String OPENAPI_RESOURCES = "../../../../../../zenwave-sdk-test-resources/src/main/resources/io/zenwave360/sdk/resources/openapi/";

    @AfterAll
    public static void testCompileAllTargetFolders() throws Exception {
        var hasModuleChanged = GitUtils.hasModuleChangedSinceLastTag("plugins/openapi-spring-webtestclient");
        if (!hasModuleChanged) {
            return;
        }

        String[] openapis = {
                "openapi-petstore.yml",
                "openapi-orders.yml",
                "openapi-orders-relational.yml"
        };

        for (String openapi : openapis) {
            int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder(openapi), "openapi.yml=" + OPENAPI_RESOURCES + openapi);
            Assertions.assertEquals(0, exitCode, "Compilation failed for " + openapi);
        }
    }

    String testPackage(String... parts) {
        parts = Arrays.stream(parts)
                .map(p -> p
                        .replaceAll("/", "_")
                        .replaceAll("\\.", "_")
                        .replaceAll("-", "_")
                        .replaceAll(",", "_"))
                .toArray(String[]::new);
        return String.join("_", parts);
    }

    private static String targetFolder(String openapi) {
        return "target/projects/spring-webtestclient/" + openapi.replaceAll("\\.", "_");
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, getPetById",
            "openapi-orders.yml, searchCustomers",
            "openapi-orders-relational.yml, createCustomer"
    })
    public void test_output_partial_one_operation(String openapi, String operationId) throws Exception {
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.partial)
                .withOption("transactional", false)
                .withOption("testsPackage", "io.example.controller.tests")
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("operationIds",  List.of(operationId));

        plugin.addAfterInChain(SpringWebTestClientGenerator.class, CapturingTemplateWriter.class);

        new MainGenerator().generate(plugin);

        var templateOutputList = CapturingTemplateWriter.templateOutputList;
        Assertions.assertEquals(1, templateOutputList.size());
        Assertions.assertEquals("src/test/java/io/example/controller/tests/Operation.java", templateOutputList.get(0).getTargetFile());
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0} {1} {2}")
    @CsvSource({
            "openapi-petstore.yml, 'addPet,getPetById,updatePet,deletePet,getPetById', json",
            "openapi-petstore.yml, 'addPet,getPetById,updatePet,deletePet,getPetById', dto",
            "openapi-orders.yml, 'createCustomer,getCustomer,updateCustomer,deleteCustomer,getCustomer', json",
            "openapi-orders.yml, 'createCustomer,getCustomer,updateCustomer,deleteCustomer,getCustomer', dto",
    })
    public void test_output_business_flow(String openapi, String operationIds, String requestPayloadType) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example.controller.tests." + testPackage("business_flow", openapi, operationIds, requestPayloadType);
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.businessFlow)
                .withOption("businessFlowTestName", camelCase(operationIds.replaceAll(",", "_")))
                .withOption("requestPayloadType", requestPayloadType)
                .withOption("transactional", false)
                .withOption("testsPackage", testPackage)
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("operationIds",  operationIds);

        plugin.addAfterInChain(SpringWebTestClientGenerator.class, CapturingTemplateWriter.class);
        plugin.addAfterInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);

        new MainGenerator().generate(plugin);

        var templateOutputList = CapturingTemplateWriter.templateOutputList;
        Assertions.assertEquals(2, templateOutputList.size());
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Assertions.assertEquals("src/test/java/" + testPackageFolder + "/" + camelCase(operationIds.replaceAll(",", "_")) +".java", templateOutputList.get(0).getTargetFile());

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }


    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApiIntegrationTest'",
            "openapi-orders.yml, createCustomer, 'CustomerApiIntegrationTest'"
    })
    public void test_output_by_one_service(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example.controller.tests." + testPackage("one_service", openapi, operationId, controllers);
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.service)
                .withOption("transactional", false)
                .withOption("testsPackage", testPackage)
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/" + testPackageFolder + "/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApiIntegrationTest'",
            "openapi-orders.yml, createCustomer, 'CustomerApiIntegrationTest'"
    })
    public void test_output_by_one_service_with_layout(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example." + testPackage("one_service_with_layout", openapi, operationId, controllers);
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.service)
                .withOption("transactional", false)
                .withOption("layout", "DefaultProjectLayout")
                .withOption("layout.openApiApiPackage", "io.example.api")
                .withOption("layout.openApiModelPackage", "io.example.api.model")
                .withOption("basePackage", testPackage)
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/" + testPackageFolder + "/adapters/web/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }


    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, addPet, 'PetApiIntegrationTest'",
            "openapi-orders.yml, createCustomer, 'CustomerApiIntegrationTest'"
    })
    public void test_output_by_one_service_simple_domain_packaging(String openapi, String operationId, String controllers) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example." + testPackage("one_service_simple_domain_packaging", openapi, operationId, controllers);
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.service)
                .withOption("transactional", false)
                .withLayout("SimpleDomainProjectLayout")
                .withOption("basePackage", testPackage)
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("operationIds",  List.of(operationId));

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/" + testPackageFolder + "/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApiIntegrationTest'",
            "openapi-orders.yml, 'CustomerApiIntegrationTest'"
    })
    public void test_output_by_service(String openapi, String controllers) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example.controller.tests." + testPackage("service", openapi, controllers);
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.service)
                .withOption("transactional", false)
                .withOption("testsPackage", testPackage)
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/" + testPackageFolder + "/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest(name = "[{index}] {displayName} {0}")
    @CsvSource({
            "openapi-petstore.yml, 'PetApi/AddPetIntegrationTest'",
            "openapi-orders.yml, 'CustomerOrderApi/CreateCustomerOrderIntegrationTest'"
    })
    public void test_output_by_operation(String openapi, String controllers) throws Exception {
        String targetFolder = targetFolder(openapi);
        String testPackage = "io.example.controller.tests." + testPackage("operation", openapi, controllers);
        String testPackageFolder = testPackage.replaceAll("\\.", "/");
        Plugin plugin = new SpringWebTestClientPlugin()
                .withApiFile("classpath:io/zenwave360/sdk/resources/openapi/" + openapi)
                .withTargetFolder(targetFolder)
                .withOption("groupBy", SpringWebTestClientGenerator.GroupByType.operation)
                .withOption("transactional", false)
                .withOption("testsPackage", testPackage)
                .withOption("openApiApiPackage", "io.example.api")
                .withOption("openApiModelPackage",  "io.example.api.model")
                .withOption("openApiModelNameSuffix", "DTO");

        new MainGenerator().generate(plugin);

        Arrays.stream(controllers.split(",")).forEach(controller -> {
            File file = new File(targetFolder + "/src/test/java/" + testPackageFolder + "/" + controller + ".java");
            Assertions.assertTrue(file.exists(), "File " + file.getAbsolutePath() + " does not exist");
        });

//        int exitCode = MavenCompiler.copyPomAndCompile("src/test/resources/pom.xml", targetFolder, "openapi.yml=" + OPENAPI_RESOURCES + openapi);
//        Assertions.assertEquals(0, exitCode);
    }

    public static class CapturingTemplateWriter implements TemplateWriter {
        static List<TemplateOutput> templateOutputList;
        @Override
        public void write(List<TemplateOutput> templateOutputList) {
            this.templateOutputList = templateOutputList;
        }
    }

}
