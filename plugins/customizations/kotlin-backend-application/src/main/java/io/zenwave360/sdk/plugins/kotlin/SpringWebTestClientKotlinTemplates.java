package io.zenwave360.sdk.plugins.kotlin;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.plugins.SpringWebTestClientGenerator;
import io.zenwave360.sdk.plugins.SpringWebTestClientHelpers;
import io.zenwave360.sdk.plugins.SpringWebTestClientTemplates;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import java.util.List;

import static io.zenwave360.sdk.templating.OutputFormatType.KOTLIN;

public class SpringWebTestClientKotlinTemplates extends SpringWebTestClientTemplates {
    private static final ProjectLayout layoutNames = new ProjectLayout();

    public SpringWebTestClientKotlinTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/kotlin/SpringWebTestClientGenerator");
    }

    public TemplateInput partialTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "partials/Operation.kt"))
                .withTargetFile(joinPath("src/test/kotlin", "{{asPackageFolder testsPackage}}/Operation.kt"))
                    .withMimeType(KOTLIN)
                .withSkipOverwrite(false);
    }

    public TemplateInput baseTestClassTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "BaseWebTestClientTest.kt"))
                .withTargetFile(joinPath("src/test/kotlin", "{{asPackageFolder baseTestClassPackage}}/{{baseTestClassName}}.kt"))
                .withMimeType(KOTLIN)
                .withSkipOverwrite(true);
    }

    public TemplateInput businessFlowTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "BusinessFlowTest.kt"))
                .withTargetFile(joinPath("src/test/kotlin", "{{asPackageFolder testsPackage}}/{{businessFlowTestName}}.kt"))
                .withMimeType(KOTLIN)
                .withSkipOverwrite(false);
    }

    public TemplateInput serviceTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "ServiceIT.kt"))
                .withTargetFile(joinPath("src/test/kotlin", "{{asPackageFolder testsPackage}}/{{serviceName}}{{testSuffix}}.kt"))
                .withMimeType(KOTLIN)
                .withSkipOverwrite(false);
    }

    public TemplateInput operationTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "OperationIT.kt"))
                .withTargetFile(joinPath("src/test/kotlin", "{{asPackageFolder testsPackage}}/{{serviceName}}/{{asJavaTypeName operationId}}{{testSuffix}}.kt"))
                .withMimeType(KOTLIN)
                .withSkipOverwrite(false);
    }

    @Override
    public List<Object> getTemplateHelpers(Generator generator) {
        if (generator instanceof SpringWebTestClientGenerator gen) {
            return List.of(
                    new SpringWebTestClientHelpers(gen.openApiModelNamePrefix, gen.openApiModelNameSuffix),
                    new SpringWebTestClientKotlinHelpers(gen.openApiModelNamePrefix, gen.openApiModelNameSuffix)
            );
        }
        throw new RuntimeException("SpringWebTestClientKotlinTemplates only supports SpringWebTestClientGenerator");
    }
}
