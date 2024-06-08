package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

import java.util.List;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class SpringWebTestClientTemplates extends ProjectTemplates {
    private static final ProjectLayout layoutNames = new ProjectLayout();

    public SpringWebTestClientTemplates() {
        setTemplatesFolder("io/zenwave360/sdk/plugins/SpringWebTestClientGenerator");
    }

    public TemplateInput partialTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "partials/Operation.java"))
                .withTargetFile(joinPath("src/test/java", "{{asPackageFolder testsPackage}}/Operation.java"))
                .withMimeType(JAVA)
                .withSkipOverwrite(false);
    }

    public TemplateInput baseTestClassTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "BaseWebTestClientTest.java"))
                .withTargetFile(joinPath("src/test/java", "{{asPackageFolder baseTestClassPackage}}/{{baseTestClassName}}.java"))
                .withMimeType(JAVA)
                .withSkipOverwrite(true);
    }

    public TemplateInput businessFlowTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "BusinessFlowTest.java"))
                .withTargetFile(joinPath("src/test/java", "{{asPackageFolder testsPackage}}/{{businessFlowTestName}}.java"))
                .withMimeType(JAVA)
                .withSkipOverwrite(false);
    }

    public TemplateInput serviceTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "ServiceIT.java"))
                .withTargetFile(joinPath("src/test/java", "{{asPackageFolder testsPackage}}/{{serviceName}}{{testSuffix}}.java"))
                .withMimeType(JAVA)
                .withSkipOverwrite(false);
    }

    public TemplateInput operationTestTemplate() {
        return new TemplateInput()
                .withTemplateLocation(joinPath(templatesFolder, "OperationIT.java"))
                .withTargetFile(joinPath("src/test/java", "{{asPackageFolder testsPackage}}/{{serviceName}}/{{asJavaTypeName operationId}}{{testSuffix}}.java"))
                .withMimeType(JAVA)
                .withSkipOverwrite(false);
    }

    @Override
    public List<Object> getTemplateHelpers(Generator generator) {
        if (generator instanceof SpringWebTestClientGenerator gen) {
            return List.of(new SpringWebTestClientHelpers(gen.openApiModelNamePrefix, gen.openApiModelNameSuffix));
        }
        throw new RuntimeException("SpringWebTestClientTemplates only supports SpringWebTestClientGenerator");
    }
}
