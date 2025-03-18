package io.zenwave360.sdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.plugins.NoOpPluginConfiguration;

public class PluginTest {

    @Test
    public void testLoadConfigFromFullClassName() throws Exception {
        String simpleClassName = NoOpPluginConfiguration.class.getName();
        Plugin config = Plugin.of(simpleClassName);
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigFromSimpleClassName() throws Exception {
        String simpleClassName = NoOpPluginConfiguration.class.getSimpleName();
        Plugin config = Plugin.of(simpleClassName);
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigKebabCase() throws Exception {
        Plugin config = Plugin.of("no-op-plugin-configuration");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigSimplifiedKebabCase() throws Exception {
        Plugin config = Plugin.of("no-op-plugin");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(NoOpPluginConfiguration.class, config.getClass());
    }

    @Test
    public void testLoadConfigNotFound() throws Exception {
        Plugin config = Plugin.of("not-found");
        Assertions.assertNotNull(config);
        Assertions.assertEquals(config.getClass(), Plugin.class);
    }
}
