package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.plugins.OpenAPIControllersGenerator;
import io.zenwave360.sdk.plugins.OpenAPIControllersHelpers;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import java.util.List;

import static io.zenwave360.sdk.templating.OutputFormatType.KOTLIN;

public class OpenAPIControllersKotlinTemplates extends ProjectTemplates {
    public OpenAPIControllersKotlinTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/kotlin/OpenAPIControllersGenerator");

        var layoutNames = new ProjectLayout(); // layoutNames
        this.addTemplate(this.singleTemplates, "src/main/kotlin", "web/mappers/BaseMapper.kt",
                layoutNames.adaptersWebMappersCommonPackage, "BaseMapper.kt", KOTLIN, null, false);
        this.addTemplate(this.serviceTemplates, "src/main/kotlin", "web/mappers/ServiceDTOsMapper.kt",
                layoutNames.adaptersWebMappersPackage, "{{serviceName}}DTOsMapper.kt", KOTLIN, null, true);
        this.addTemplate(this.serviceTemplates, "src/main/kotlin", "web/{{webFlavor}}/ServiceApiController.kt",
                layoutNames.adaptersWebPackage, "{{serviceName}}ApiController.kt", KOTLIN, null, false);
        this.addTemplate(this.serviceTemplates, "src/test/kotlin", "web/{{webFlavor}}/ServiceApiControllerTest.kt",
                layoutNames.adaptersWebPackage, "{{serviceName}}ApiControllerTest.kt", KOTLIN, null, true);

    }

    @Override
    public List<Object> getTemplateHelpers(Generator generator) {
        if (generator instanceof OpenAPIControllersGenerator openAPIControllersGenerator) {
            return List.of(
                    new OpenAPIControllersHelpers(openAPIControllersGenerator.openApiModelNamePrefix, openAPIControllersGenerator.openApiModelNameSuffix),
                    new OpenAPIControllersKotlinHelpers(openAPIControllersGenerator.openApiModelNamePrefix, openAPIControllersGenerator.openApiModelNameSuffix));
        }
        throw new RuntimeException("OpenAPIControllersKotlinTemplates only supports OpenAPIControllersGenerator");
    }
}
