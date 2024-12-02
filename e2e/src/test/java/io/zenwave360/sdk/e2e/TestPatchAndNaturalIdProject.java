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

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestPatchAndNaturalIdProject {
    private String basePackage = "io.zenwave360.example";

    @Test
    public void testCustomerAddressPostgresJson() throws Exception {
        String sourceFolder = "src/test/resources/projects/patch-and-natural-id/";
        String targetFolder = "target/patch-and-natural-id/";
        String zdlFile = targetFolder + "/patch-and-natural-id.zdl";

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
                .withOption("databaseType", DatabaseType.postgresql)
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("useLombok", true)
                .withOption("includeEmitEventsImplementation", true)
                .withOption("forceOverwrite", true)
                .withOption("haltOnFailFormatting", false);

        new MainGenerator().generate(plugin);

        TextUtils.replaceInFile(new File(targetFolder + "/src/main/java/io/zenwave360/example/core/implementation/mappers/EventsMapper.java"),
                "io.zenwave360.example.core.outbound.events.dtos.Customer asCustomer\\(Customer customer\\);",
                """
                        	@org.mapstruct.Mapping(target = "extraProperties", ignore = true)
                        	io.zenwave360.example.core.outbound.events.dtos.Customer asCustomer(Customer customer);
                        """);

        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);

        plugin = new OpenAPIControllersPlugin()
                .withApiFile(targetFolder + "/src/main/resources/apis/openapi.yml")
                .withTargetFolder(targetFolder)
                .withOption("zdlFile", zdlFile)
                .withOption("basePackage", basePackage)
                .withOption("controllersPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiApiPackage", "{{basePackage}}.adapters.web")
                .withOption("openApiModelPackage", "{{basePackage}}.adapters.web.model")
                .withOption("openApiModelNameSuffix", "DTO")
                .withOption("style", ProgrammingStyle.imperative)
                .withOption("haltOnFailFormatting", false);
        new MainGenerator().generate(plugin);

        TextUtils.replaceInFile(new File(targetFolder + "/src/main/java/io/zenwave360/example/adapters/web/mappers/CustomerDTOsMapper.java"),
                "// request mappings",
                """
                        	// request mappings
                        	default Map map(Object value) { return new HashMap();}
                        """);

        exitCode = MavenCompiler.compile(new File(targetFolder));
        Assertions.assertEquals(0, exitCode);
    }

}
