package io.zenwave360.generator;

import io.zenwave360.generator.plugins.NoOpPluginConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigurationTest {

    @Test
    public void testLoadConfigFromFullClassName() throws Exception {
        String simpleClassName = NoOpPluginConfiguration.class.getName();
        Configuration config = Configuration.of(simpleClassName);
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigFromSimpleClassName() throws Exception {
        String simpleClassName = NoOpPluginConfiguration.class.getSimpleName();
        Configuration config = Configuration.of(simpleClassName);
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigKebabCase() throws Exception {
        Configuration config = Configuration.of("no-op-plugin-configuration");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }


    @Test
    public void testLoadConfigSimplifiedKebabCase() throws Exception {
        Configuration config = Configuration.of("no-op-plugin");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigFromConfigId() throws Exception {
        Configuration config = Configuration.of("no-op");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigNotFound() throws Exception {
        Configuration config = Configuration.of("not-found");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(config.getClass(), Configuration.class);
    }
}
