package io.zenwave360.sdk.zdl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.generators.Generator;

class ProjectTemplatesTest {

    @Test
    void selectsHelpersByGeneratorTypeAndSharesGenericHelpers() {
        var templates = new ProjectTemplates();
        templates.addTemplateHelpers(PrimaryGenerator.class, "primary");
        templates.addTemplateHelpers(null, "generic");

        var primaryHelpers = templates.getTemplateHelpers(new PrimaryGenerator());
        var subclassHelpers = templates.getTemplateHelpers(new PrimaryChildGenerator());
        var secondaryHelpers = templates.getTemplateHelpers(new SecondaryGenerator());

        Assertions.assertEquals(List.of("primary", "generic"), primaryHelpers);
        Assertions.assertEquals(List.of("primary", "generic"), subclassHelpers);
        Assertions.assertEquals(List.of("generic"), secondaryHelpers);
    }

    private static class PrimaryGenerator extends Generator {
        @Override
        public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
            return new GeneratedProjectFiles();
        }
    }

    private static class PrimaryChildGenerator extends PrimaryGenerator {
    }

    private static class SecondaryGenerator extends Generator {
        @Override
        public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
            return new GeneratedProjectFiles();
        }
    }
}
