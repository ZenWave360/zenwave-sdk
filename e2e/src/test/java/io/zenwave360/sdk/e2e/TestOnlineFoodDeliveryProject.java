package io.zenwave360.sdk.e2e;

import java.io.File;

import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin;
import io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOnlineFoodDeliveryProject {

    private static String project = "online-food-delivery-mongo";
    private static String sourceFolder = "src/test/resources/projects/" + project + "/";
    private static String targetFolder = "target/projects/" + project + "/";

    private String basePackage = "io.zenwave360.example";

    @BeforeAll
    public static void beforeAll() throws Exception {
        // copy whole dir from sourceFolder to targetFolder
        FileUtils.deleteDirectory(new File(targetFolder));
        FileUtils.forceMkdir(new File(targetFolder));
        FileUtils.copyDirectory(new File(sourceFolder), new File(targetFolder));
        Assertions.assertTrue(new File(targetFolder).exists());
    }

//        @Test
    public void test() throws Exception {
        var module = "restaurants";
        var modulePackage = basePackage + "." + module;
        var moduleFolder = targetFolder + "modules/" + module;
        var zdlFile = targetFolder + "models/" + module + ".zdl";

        Plugin plugin = new BackendApplicationDefaultPlugin()
            .withZdlFile(zdlFile)
            .withTargetFolder(moduleFolder)
            .withOption("basePackage", modulePackage)
            .withOption("persistence", PersistenceType.mongodb)
            .withOption("style", ProgrammingStyle.imperative)
            .withOption("useLombok", true)
            .withOption("includeEmitEventsImplementation", true)
            .withOption("forceOverwrite", true)
            .withOption("haltOnFailFormatting", false);


        new MainGenerator().generate(plugin);
        //        int exitCode = MavenCompiler.compile(new File(targetFolder));
        //        Assertions.assertEquals(0, exitCode);
    }

    @Order(1)
    @ParameterizedTest
    @ValueSource(strings = {"customers", "orders", "restaurants", "delivery"})
    public void generateApis(String module) throws Exception {
        var modulePackage = basePackage + "." + module;
        var moduleFolder = targetFolder + "modules/" + module;
        var zdlFile = targetFolder + "models/" + module + ".zdl";

        Plugin plugin = null;

        plugin = new ZDLToOpenAPIPlugin()
                .withZdlFile(zdlFile)
                .withOption("idType", "string")
                .withOption("targetFile", "/src/main/resources/apis/openapi.yml")
                .withTargetFolder(moduleFolder);
        new MainGenerator().generate(plugin);

        plugin = new ZDLToAsyncAPIPlugin()
                .withZdlFile(zdlFile)
                .withOption("asyncapiVersion", "v3")
                .withOption("idType", "string")
                .withOption("targetFile", "/src/main/resources/apis/asyncapi.yml")
                .withTargetFolder(moduleFolder);
        new MainGenerator().generate(plugin);
    }

    @Order(2)
    @Test
    public void generateSourceFromAPIs() throws Exception {
        var pom = "/pom.xml";
        int exitCode = MavenCompiler.compile(pom, new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

    @Order(3)
    @ParameterizedTest
    @ValueSource(strings = {"customers", "orders", "restaurants", "delivery"})
    public void generateModule(String module) throws Exception {
        var pom = "modules/" + module + "/pom.xml";
        var modulePackage = basePackage + "." + module;
        var moduleFolder = targetFolder + "modules/" + module;
        var zdlFile = targetFolder + "models/" + module + ".zdl";

        Plugin plugin = null;
        int exitCode = 0;

        plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(zdlFile)
                .withTargetFolder(moduleFolder)
                .withOption("basePackage", modulePackage)
                .withOption("persistence", PersistenceType.mongodb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useLombok", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

//        exitCode = MavenCompiler.compile(pom, new File(targetFolder));
//        Assertions.assertEquals(0, exitCode);

        plugin = new OpenAPIControllersPlugin()
                .withApiFile(moduleFolder + "/src/main/resources/apis/openapi.yml")
                .withZdlFile(zdlFile)
                .withOption("basePackage", modulePackage)
                .withOption("layout", "DefaultProjectLayout")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("haltOnFailFormatting", false)
                .withTargetFolder(moduleFolder);

        new MainGenerator().generate(plugin);

//        exitCode = MavenCompiler.compile(pom, new File(targetFolder));
//        Assertions.assertEquals(0, exitCode);
    }

    @Order(4)
    @Test
    public void compileModules() throws Exception {
        var pom = "/pom.xml";
        int exitCode = MavenCompiler.compile(pom, new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }
}
