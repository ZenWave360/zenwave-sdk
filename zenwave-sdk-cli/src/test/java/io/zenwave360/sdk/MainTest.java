package io.zenwave360.sdk;

import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    void testMain_zdlFiles() {
        String[] args = {"-p", "ZdlToJsonPlugin", "zdlFiles=classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl,classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl"};
        Main.main(args);
    }

    @Test
    void testMain_layout() {
        String[] args = {
            "-p", "ZdlToJsonPlugin",
            "title=Clinical Tool Backend",
            "basePackage=io.zenwave360.example.clinicaltool",
            "persistence=jpa",
            "databaseType=postgresql",
                "layout=SimpleDomainProjectLayout",
            "layout.commonPackage={{basePackage}}.common",
            "layout.infrastructureRepositoryCommonPackage={{commonPackage}}",
            "layout.adaptersWebMappersCommonPackage={{commonPackage}}.mappers",
            "layout.coreImplementationMappersCommonPackage={{commonPackage}}.mappers",
            "layout.customWebModule={{layout.adaptersWebPackage}}.{{webModule}}",
            "layout.adaptersWebPackage={{layout.customWebModule}}",
            "layout.openApiApiPackage={{layout.customWebModule}}",
            "layout.openApiModelPackage={{layout.customWebModule}}.dtos",
            "openApiModelNameSuffix=DTO",
            "idType=integer",
            "idTypeFormat=int64",
            "useLombok=true",
            "useSpringModulith=true",
            "haltOnFailFormatting=false",
            "zdlFile=classpath:io/zenwave360/sdk/resources/zdl/documents.zdl"
        };
        Main.main(args);
    }
}
