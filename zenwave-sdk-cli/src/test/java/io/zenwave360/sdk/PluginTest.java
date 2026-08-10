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
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> Plugin.of("not-found"));
        Assertions.assertEquals("Plugin not found: 'not-found'. Check the plugin name or use --help to list available plugins.", exception.getMessage());
    }

    @Test
    public void testLoadConfigFullClassNameNotFound() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> Plugin.of("io.zenwave360.sdk.plugins.NotFoundPlugin"));
        Assertions.assertEquals("Plugin not found: 'io.zenwave360.sdk.plugins.NotFoundPlugin'. Check the plugin name or use --help to list available plugins.", exception.getMessage());
    }

    @Test
    public void testMultilineStringOptionsAreTrimmed() {
        Plugin config = new Plugin();

        config.withOption("applicationExtensions",
                "\n"
                        + "            x-application-bindings:\n"
                        + "              x-principal: payments_processing\n"
                        + "              x-clientId: payments_processing\n"
                        + "              x-groupId: payments_processing\n"
                        + "            ");

        Assertions.assertEquals(
                "x-application-bindings:\n"
                        + "  x-principal: payments_processing\n"
                        + "  x-clientId: payments_processing\n"
                        + "  x-groupId: payments_processing",
                config.getOptions().get("applicationExtensions"));
    }

    @Test
    public void testTripleQuotedMultilineStringOptionsAreTrimmed() {
        Plugin config = new Plugin();

        config.withOption("applicationExtensions",
                "\"\"\"\n"
                        + "            x-application-bindings:\n"
                        + "              x-principal: payments_processing\n"
                        + "            \"\"\"");

        Assertions.assertEquals(
                "x-application-bindings:\n"
                        + "  x-principal: payments_processing",
                config.getOptions().get("applicationExtensions"));
    }
}
