package io.zenwave360.sdk.e2e;

import java.io.File;

import io.zenwave360.sdk.options.DatabaseType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin;
import io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCustomerAddressRelationalProject {

    private static String project = "customer-address-relational";
    private static String sourceFolder = "src/test/resources/projects/" + project + "/";
    private static String targetFolder = "target/projects/" + project + "/";
    private String basePackage = "io.zenwave360.example";
    private String zdlFile = targetFolder + "/customer-address-relational.zdl";

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
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withSpecFile(zdlFile)
                .withTargetFolder(targetFolder)
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("databaseType", DatabaseType.mariadb)
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
    @Test
    public void generateApis() throws Exception {
        Plugin plugin = null;

        plugin = new ZDLToOpenAPIPlugin()
                .withSpecFile(zdlFile)
                .withOption("idType", "integer")
                .withOption("idTypeFormat", "int64")
                .withOption("targetFile", "/src/main/resources/apis/openapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);

        var replace = "        - name: \"identifier\"\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: integer\n" +
                "            format: int64";
        var replacement = "        - name: \"identifier\"\n" +
                "          in: path\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string";
        TextUtils.replaceInFile(new File(targetFolder + "/src/main/resources/apis/openapi.yml"), replace, replacement);

        plugin = new ZDLToAsyncAPIPlugin()
                .withSpecFile(zdlFile)
                .withOption("asyncapiVersion", "v3")
                .withOption("idType", "integer")
                .withOption("idTypeFormat", "int64")
                .withOption("targetFile", "/src/main/resources/apis/asyncapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);
    }

    @Order(2)
    @Test
    public void generateSourceFromAPIs() throws Exception {
        int exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

    @Order(3)
    @Test
    public void generateModule() throws Exception {
        Plugin plugin = null;
        int exitCode = 0;

        plugin = new BackendApplicationDefaultPlugin()
                .withSpecFile(zdlFile)
                .withTargetFolder(targetFolder)
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("databaseType", DatabaseType.mariadb)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useLombok", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);

        plugin = new OpenAPIControllersPlugin()
                .withSpecFile(targetFolder + "/src/main/resources/apis/openapi.yml")
                .withOption("zdlFile", zdlFile)
                .withOption("basePackage", basePackage)
                .withOption("controllersPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiApiPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiModelPackage", "{{basePackage}}.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder(targetFolder);

        new MainGenerator().generate(plugin);
        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

}
