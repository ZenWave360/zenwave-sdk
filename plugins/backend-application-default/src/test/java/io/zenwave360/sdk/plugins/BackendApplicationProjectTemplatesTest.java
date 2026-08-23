package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.plugins.annotators.AnnotationHelper;

class BackendApplicationProjectTemplatesTest {

    @Test
    void backendHelpersAreBoundToTheDefaultGeneratorAndAnnotationHelperIsGeneric() {
        var backendGenerator = new BackendApplicationDefaultGenerator();
        backendGenerator.onPropertiesSet();
        var adapterGenerator = new BackendApplicationAsyncApiAdaptersGenerator();
        adapterGenerator.onPropertiesSet();

        var backendHelpers = backendGenerator.templates.getTemplateHelpers(backendGenerator);
        var adapterHelpers = adapterGenerator.templates.getTemplateHelpers(adapterGenerator);

        Assertions.assertTrue(backendHelpers.stream().anyMatch(BackendApplicationDefaultHelpers.class::isInstance));
        Assertions.assertTrue(backendHelpers.stream().anyMatch(BackendApplicationDefaultJpaHelpers.class::isInstance));
        Assertions.assertTrue(backendHelpers.contains(AnnotationHelper.class));

        Assertions.assertTrue(adapterHelpers.stream().noneMatch(BackendApplicationDefaultHelpers.class::isInstance));
        Assertions.assertTrue(adapterHelpers.stream().noneMatch(BackendApplicationDefaultJpaHelpers.class::isInstance));
        Assertions.assertTrue(adapterHelpers.contains(AnnotationHelper.class));
    }
}
