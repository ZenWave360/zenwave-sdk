package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import java.util.List;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class OpenAPIControllersTemplates extends ProjectTemplates {
    public OpenAPIControllersTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/OpenAPIControllersGenerator");

        var layoutNames = new ProjectLayout(); // layoutNames
        this.addTemplate(this.singleTemplates, "src/main/java", "web/mappers/BaseMapper.java",
                layoutNames.adaptersWebMappersCommonPackage, "BaseMapper.java", JAVA, null, false);
        this.addTemplate(this.serviceTemplates, "src/main/java", "web/mappers/ServiceDTOsMapper.java",
                layoutNames.adaptersWebMappersPackage, "{{serviceName}}DTOsMapper.java", JAVA, null, true);
        this.addTemplate(this.serviceTemplates, "src/main/java", "web/{{webFlavor}}/ServiceApiController.java",
                layoutNames.adaptersWebPackage, "{{serviceName}}ApiController.java", JAVA, null, false);
        this.addTemplate(this.serviceTemplates, "src/test/java", "web/{{webFlavor}}/ServiceApiControllerTest.java",
                layoutNames.adaptersWebPackage, "{{serviceName}}ApiControllerTest.java", JAVA, null, true);

    }

    @Override
    public List<Object> getTemplateHelpers(Generator generator) {
        if (generator instanceof OpenAPIControllersGenerator) {
            return List.of(new OpenAPIControllersHelpers(((OpenAPIControllersGenerator) generator).openApiModelNamePrefix, ((OpenAPIControllersGenerator) generator).openApiModelNameSuffix));
        }
        throw new RuntimeException("OpenAPIControllersTemplates only supports OpenAPIControllersGenerator");
    }
}
