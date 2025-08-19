package io.zenwave360.sdk.e2e;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import io.zenwave360.sdk.utils.Maps;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.List;
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
            "layout.adaptersWebMappersCommonPackage", "{{commonPackage}}.mappers",
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

    @ParameterizedTest
    @CsvSource({
            "clinical.zdl, webapp, getPatientProfileById, ",
            "clinical.zdl, mobile, , 'getPatientProfileById,requestOptOut'",
            "documents.zdl, documents, , ",
            "surveys.zdl, surveys-backoffice, 'getSurveyAndQuestionsForPatient,answerSurvey,updateSurveyAnswers,getSurveyAnswers',",
            "surveys.zdl, surveys-public, , 'getSurveyAndQuestionsForPatient,answerSurvey,updateSurveyAnswers,getSurveyAnswers'",
            "masterdata.zdl, masterdata, , ",
            "terms-and-conditions.zdl, terms-and-conditions, ,"
    })
    @Order(1)
    public void testGenerateOpenAPIs(String zdlFileNames, String webModule, String operationIdsToExclude, String operationIdsToInclude) throws Exception {
        List<String> zdlFiles = List.of(zdlFileNames.split(",")).stream().map(zdlFileName -> targetFolder + "/models/" + zdlFileName).toList();
        String openApiFile = targetFolder + "/src/main/resources/public/apis/" + webModule + "-openapi.yml";

        Plugin plugin = new ZDLToOpenAPIPlugin()
                .withZdlFiles(zdlFiles)
                .withOption("targetFile", openApiFile)
                .withOptions(options)
                .withOption("operationIdsToExclude", operationIdsToExclude)
                .withOption("operationIdsToInclude", operationIdsToInclude)
        ;
        new MainGenerator().generate(plugin);
    }

    @ParameterizedTest
    @CsvSource({
            "clinical.zdl",
            "documents.zdl",
            "surveys.zdl",
            "masterdata.zdl",
            "terms-and-conditions.zdl"
    })
    @Order(2)
    public void testGenerateBackendModules(String zdlFile) throws Exception {
        Plugin plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(targetFolder + "/models/" + zdlFile)
                .withTargetFolder(targetFolder)
                .withOptions(options);
        new MainGenerator().generate(plugin);
    }

    @Test
    @Order(3)
    @Disabled
    public void compileBackendModules() throws Exception {
        int exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

    @ParameterizedTest
    @CsvSource({
            "clinical.zdl,webapp-openapi.yml,webapp",
            "clinical.zdl,mobile-openapi.yml,mobile",
            "documents.zdl,documents-openapi.yml,documents",
            "surveys.zdl,surveys-backoffice-openapi.yml,surveys.backoffice",
            "surveys.zdl,surveys-public-openapi.yml,surveys.api",
            "masterdata.zdl,masterdata-openapi.yml,masterdata",
            "terms-and-conditions.zdl,terms-and-conditions-openapi.yml,termsandconditions"
    })
    @Order(4)
    public void testGenerateControllers(String zdl, String openapi, String webModule) throws Exception {
        String zdlFile = targetFolder + "/models/" + zdl;
        String openApiFile = targetFolder + "/src/main/resources/public/apis/" + openapi;

        Plugin plugin = new OpenAPIControllersPlugin()
                .withZdlFile(zdlFile)
                .withApiFile(openApiFile)
                .withTargetFolder(targetFolder)
                .withOptions(options)
                .withOption("customWebModule", "{{basePackage}}.adapters.web." + webModule)
                .withOption("layout.adaptersWebPackage", "{{customWebModule}}")
                .withOption("layout.openApiApiPackage", "{{layout.customWebModule}}")
        ;
        new MainGenerator().generate(plugin);
    }

    @Test
    @Order(5)
    public void fixControllers() throws Exception {

        TextUtils.replaceInFile(new File(targetFolder + "/src/main/java/com/example/clinical/adapters/web/masterdata/MasterDataApiController.java"),
                "masterDataService.listMasterDataOfType\\(type, lang\\)",
                "masterDataService.listMasterDataOfType(MasterDataType.valueOf(type), lang)");

        TextUtils.replaceInFile(new File(targetFolder + "/src/main/java/com/example/clinical/modules/documents/mappers/DocumentServiceMapper.java"),
                "DocumentInfo asDocumentInfo\\(List<Long> documentIds\\);",
                "default DocumentInfo asDocumentInfo(List<Long> documentIds) { return new DocumentInfo(); }");
        TextUtils.replaceInFile(new File(targetFolder + "/src/main/java/com/example/clinical/modules/documents/mappers/DocumentServiceMapper.java"),
                "DocumentInfo update\\(@MappingTarget DocumentInfo entity, List<Long> documentIds\\);",
                " default DocumentInfo update(@MappingTarget DocumentInfo entity, List<Long> documentIds) { return entity; }");
    }

    @Test
    @Order(6)
    public void compileControllers() throws Exception {
        int exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

}
