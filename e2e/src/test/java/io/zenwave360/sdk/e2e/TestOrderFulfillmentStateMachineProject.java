package io.zenwave360.sdk.e2e;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.options.DatabaseType;
import io.zenwave360.sdk.options.PersistenceType;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin;
import io.zenwave360.sdk.plugins.OpenAPIControllersPlugin;
import io.zenwave360.sdk.plugins.ZDLToAsyncAPIPlugin;
import io.zenwave360.sdk.plugins.ZDLToOpenAPIPlugin;
import io.zenwave360.sdk.testutils.MavenCompiler;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOrderFulfillmentStateMachineProject {
    private String basePackage = "io.zenwave360.example.orderfulfillment";

    @ParameterizedTest
    @ValueSource(strings = {"java", "kotlin"})
    public void testOrderFulfillmentStateMachine(String targetLanguage) throws Exception {
        String sourceFolder = "src/test/resources/projects/order-fulfillment-state-machine";
        String targetFolder = "target/projects/order-fulfillment-state-machine-" + targetLanguage;
        String zdlFile = targetFolder + "/order-fulfillment-state-machine.zdl";

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
                .withOption("targetFile", "/src/main/resources/public/apis/openapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);

        plugin = new ZDLToAsyncAPIPlugin()
                .withZdlFile(zdlFile)
                .withOption("asyncapiVersion", "v3")
                .withOption("idType", "integer")
                .withOption("idTypeFormat", "int64")
                .withOption("includeCloudEventsHeaders", true)
                .withOption("includeKafkaCommonHeaders", true)
                .withOption("targetFile", "/src/main/resources/public/apis/asyncapi.yml")
                .withTargetFolder(targetFolder);
        new MainGenerator().generate(plugin);

        plugin = new BackendApplicationDefaultPlugin()
                .withZdlFile(zdlFile)
                .withTargetFolder(targetFolder)
//                .withOption("templates", "new io.zenwave360.sdk.plugins.kotlin.BackendApplicationKotlinTemplates")
                .withOption("basePackage", basePackage)
                .withOption("persistence", PersistenceType.jpa)
                .withOption("databaseType", DatabaseType.postgresql)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useJSpecify", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);
        if ("kotlin".equals(targetLanguage)) {
            plugin.withOption("templates", "new io.zenwave360.sdk.plugins.kotlin.BackendApplicationKotlinTemplates");
        }

        new MainGenerator().generate(plugin);

        plugin = new OpenAPIControllersPlugin()
                .withApiFile(targetFolder + "/src/main/resources/public/apis/openapi.yml")
//                .withOption("templates", "new io.zenwave360.sdk.plugins.kotlin.OpenAPIControllersKotlinTemplates")
                .withOption("zdlFile", zdlFile)
                .withOption("basePackage", basePackage)
                .withOption("openApiApiPackage", "{{basePackage}}.web")
                .withOption("openApiModelPackage", "{{basePackage}}.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("style", ProgrammingStyle.imperative)
                .withTargetFolder(targetFolder);
        if ("kotlin".equals(targetLanguage)) {
            plugin.withOption("templates", "new io.zenwave360.sdk.plugins.kotlin.OpenAPIControllersKotlinTemplates");
        }

        new MainGenerator().generate(plugin);

        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

}
