package io.zenwave360.sdk.e2e;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMonolithClinicalProject {
    static String sourceFolder = "src/test/resources/projects/monolith-clinical-project/";
    static String targetFolder = "target/projects/monolith-clinical-project";
    static String basePackage = "com.example.clinical";
    static Map<String, Object> options = Maps.of(
        "title", "Clinical Tool Backend",
        "persistence", PersistenceType.jpa,
        "databaseType", DatabaseType.postgresql,
        "basePackage", basePackage,
        "layout.coreImplementationMappersCommonPackage", "{{commonPackage}}.mappers",
        "layout.infrastructureRepositoryCommonPackage", "{{commonPackage}}",
        "openApiModelNameSuffix", "DTO",
        "idType", "integer",
        "idTypeFormat", "int64",
        "useLombok", true,
        "includeEmitEventsImplementation", false,
        "haltOnFailFormatting", false
    );

    @BeforeAll
    public static void beforeAll() throws Exception {
        // copy whole dir from sourceFolder to targetFolder
        FileUtils.deleteDirectory(new File(targetFolder));
        FileUtils.forceMkdir(new File(targetFolder));
        FileUtils.copyDirectory(new File(sourceFolder), new File(targetFolder));
        Assertions.assertTrue(new File(targetFolder).exists());
    }

    @Test
    @Order(1)
    public void testMonolithClinicalProject() throws Exception {

        String zdlFile = targetFolder + "/models/clinical.zdl";
        String openApiFile = targetFolder + "/src/main/resources/apis/webapp-openapi.yml";

        Plugin plugin = null;
        int exitCode = 0;

        plugin = new ZDLToOpenAPIPlugin()
                .withZdlFile(zdlFile)
                .withApiFile(openApiFile)
                .withOptions(options)
                .withOptions(Maps.of(
                    "title", "Clinical Tool - WebApp API",
                    "operationIdsToExclude", "getPatientProfileById"
                ))
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);


        plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(zdlFile)
                .withTargetFolder(targetFolder)
                .withOptions(options);
        new MainGenerator().generate(plugin);

//        exitCode = MavenCompiler.compile(new File(targetFolder));
//        Assertions.assertEquals(0, exitCode);

//        plugin = new OpenAPIControllersPlugin()
//                .withApiFile(targetFolder + "/src/main/resources/apis/openapi.yml")
//                .withOption("zdlFile", zdlFile)
//                .withOption("basePackage", basePackage)
//                .withOption("controllersPackage", "{{basePackage}}")
//                .withOption("openApiApiPackage", "{{basePackage}}")
//                .withOption("openApiModelPackage", "{{basePackage}}.dtos")
//                .withOption("openApiModelNameSuffix", "DTO")
//
//                .withOption("entitiesPackage", "{{basePackage}}.model")
//                .withOption("inboundDtosPackage", "{{basePackage}}.dtos")
//                .withOption("servicesPackage", "{{basePackage}}")
//
//                // .withOption("operationIds", List.of("addPet", "updatePet"))
//                .withOption("style", ProgrammingStyle.imperative)
//                .withTargetFolder(targetFolder);
//        new MainGenerator().generate(plugin);
//
//        exitCode = MavenCompiler.compile(new File(targetFolder));
//        Assertions.assertEquals(0, exitCode);
    }

}
