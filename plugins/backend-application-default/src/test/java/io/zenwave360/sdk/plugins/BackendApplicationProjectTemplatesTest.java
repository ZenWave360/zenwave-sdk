package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;

class BackendApplicationProjectTemplatesTest {

    /**
     * Backend specific helpers stay bound to the backend generator. The annotation helper is no
     * longer registered here at all: it lives in HandlebarsEngine so {@code {{annotate}}} resolves
     * for every generator.
     */
    @Test
    void backendHelpersAreBoundToTheDefaultGenerator() {
        var backendGenerator = new BackendApplicationDefaultGenerator();
        backendGenerator.onPropertiesSet();
        var adapterGenerator = new BackendApplicationAsyncApiAdaptersGenerator();
        adapterGenerator.onPropertiesSet();

        var backendHelpers = backendGenerator.templates.getTemplateHelpers(backendGenerator);
        var adapterHelpers = adapterGenerator.templates.getTemplateHelpers(adapterGenerator);

        Assertions.assertTrue(backendHelpers.stream().anyMatch(BackendApplicationDefaultHelpers.class::isInstance));
        Assertions.assertTrue(backendHelpers.stream().anyMatch(BackendApplicationDefaultJpaHelpers.class::isInstance));

        Assertions.assertTrue(adapterHelpers.stream().noneMatch(BackendApplicationDefaultHelpers.class::isInstance));
        Assertions.assertTrue(adapterHelpers.stream().noneMatch(BackendApplicationDefaultJpaHelpers.class::isInstance));
    }

    /**
     * Flat plugin options are bound onto the generator's {@code templates} object as well as onto the
     * generator itself, by {@code MainGenerator.applyConfiguration}. This is what makes
     * {@code @DocumentedOption} fields declared on a {@link io.zenwave360.sdk.zdl.ProjectTemplates}
     * subclass configurable, and it is why annotator flags must be declared in exactly one place: a
     * field of the same name on both the processor and the templates would be set twice.
     */
    @Test
    void flatOptionsAreBoundOntoProjectTemplates() throws Exception {
        var generator = new BackendApplicationDefaultGenerator();
        var plugin = new BackendApplicationDefaultPlugin().withOption("useSpringModulith", true);

        MainGenerator.applyConfiguration(0, generator, plugin);

        var templates = (BackendApplicationProjectTemplates) generator.templates;
        Assertions.assertTrue(templates.useSpringModulith,
                "options must reach ProjectTemplates fields, not only generator fields");
    }
}
