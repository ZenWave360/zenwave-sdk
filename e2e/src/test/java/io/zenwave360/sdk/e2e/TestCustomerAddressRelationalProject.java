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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCustomerAddressRelationalProject {
    private String basePackage = "io.zenwave360.example";

    @ParameterizedTest
    @ValueSource(strings = {"one-to-many", "one-to-one", "one-to-one-aggregates", "one-to-one-map-id", "many-to-one"})
    public void testCustomerAddressRelational(String flavor) throws Exception {
        String sourceFolder = "src/test/resources/projects/customer-address-relational/";
        String targetFolder = "target/projects/customer-address-relational/" + flavor;
        String zdlFile = targetFolder + "/customer-address-relational-" + flavor + ".zdl";

        // copy whole dir from sourceFolder to targetFolder
        FileUtils.deleteDirectory(new File(targetFolder));
        FileUtils.forceMkdir(new File(targetFolder));
        FileUtils.copyDirectory(new File(sourceFolder), new File(targetFolder));
        Assertions.assertTrue(new File(targetFolder).exists());

        Plugin plugin = null;
        int exitCode = 0;

        plugin = new ZDLToOpenAPIPlugin()
                .withZdlFile(zdlFile)
                .withOption("idType", "integer")
                .withOption("idTypeFormat", "int64")
                .withOption("targetFile", "/src/main/resources/apis/openapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);

//        var replace = "        - name: \"identifier\"\n" +
//                "          in: path\n" +
//                "          required: true\n" +
//                "          schema:\n" +
//                "            type: integer\n" +
//                "            format: int64";
//        var replacement = "        - name: \"identifier\"\n" +
//                "          in: path\n" +
//                "          required: true\n" +
//                "          schema:\n" +
//                "            type: string";
//        TextUtils.replaceInFile(new File(targetFolder + "/src/main/resources/apis/openapi.yml"), replace, replacement);

        plugin = new ZDLToAsyncAPIPlugin()
                .withZdlFile(zdlFile)
                .withOption("asyncapiVersion", "v3")
                .withOption("idType", "integer")
                .withOption("idTypeFormat", "int64")
                .withOption("targetFile", "/src/main/resources/apis/asyncapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);

        plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(zdlFile)
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
                .withApiFile(targetFolder + "/src/main/resources/apis/openapi.yml")
                .withZdlFile(zdlFile)
                .withOption("basePackage", basePackage)
                .withOption("controllersPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiApiPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiModelPackage", "{{basePackage}}.adapters.web.dtos")
                .withOption("openApiModelNameSuffix", "DTO")
                // .withOption("operationIds", List.of("addPet", "updatePet"))
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder(targetFolder);

        new MainGenerator().generate(plugin);
        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

}
