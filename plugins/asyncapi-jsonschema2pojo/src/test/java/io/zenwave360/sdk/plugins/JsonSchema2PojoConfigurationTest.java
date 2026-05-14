package io.zenwave360.sdk.plugins;

import java.util.Map;

import org.jsonschema2pojo.AllFileFilter;
import org.jsonschema2pojo.util.JavaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonSchema2PojoConfigurationTest {

    @Test
    void should_bind_upstream_parity_options() {
        JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(Map.of(
                "includeGeneratedAnnotation", "true",
                "useInnerClassBuilders", "true",
                "removeOldOutput", "true",
                "fileFilter", AllFileFilter.class.getName(),
                "useJakartaValidation", "false"));

        Assertions.assertTrue(config.isIncludeGeneratedAnnotation());
        Assertions.assertTrue(config.isUseInnerClassBuilders());
        Assertions.assertTrue(config.isRemoveOldOutput());
        Assertions.assertInstanceOf(AllFileFilter.class, config.getFileFilter());
        Assertions.assertFalse(config.isUseJakartaValidation());
    }

    @Test
    void should_support_deprecated_use_jakarta_validation_alias() {
        JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(Map.of(
                "isUseJakartaValidation", "false"));

        Assertions.assertFalse(config.isUseJakartaValidation());
    }

    @Test
    void should_prefer_canonical_use_jakarta_validation_name() {
        JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(Map.of(
                "useJakartaValidation", "true",
                "isUseJakartaValidation", "false"));

        Assertions.assertTrue(config.isUseJakartaValidation());
    }

    @Test
    void should_default_target_version_to_current_runtime() {
        JsonSchema2PojoConfiguration config = JsonSchema2PojoConfiguration.of(Map.of());

        Assertions.assertEquals(JavaVersion.parse(System.getProperty("java.version")), config.getTargetVersion());
    }
}
