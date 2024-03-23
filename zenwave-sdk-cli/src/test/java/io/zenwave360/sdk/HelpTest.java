package io.zenwave360.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import io.zenwave360.sdk.plugins.NoOpPluginConfiguration;
import org.slf4j.Logger;

public class HelpTest {

    @Test
    public void testNoOptionsHelp() {
        Main.main();
    }

    @Test
    public void testMainHelp() {
        Main.main("-h");
    }

    @Test
    public void testPluginsJsonHelp() {
        Main.main("-h", "json");
    }

    @Test
    public void testPluginHelp() {
        Main.main("-h", "-p", NoOpPluginConfiguration.class.getName());
    }

    @Test
    public void testPluginMarkdownHelp() {
        Main.main("-h", "markdown", "-p", NoOpPluginConfiguration.class.getName());
    }

    @Test
    public void testDiscoverAvailablePlugins() {
        Main.main("-h", Help.Format.list.toString());
    }

    @Test
    public void testGetJarVersion() {
        Help help = new Help();
        String version = help.getJarVersion(Test.class);
        Assertions.assertNotNull(version);
        Assertions.assertFalse(version.isEmpty());
        Assertions.assertTrue(version.startsWith("5."));
    }
}
