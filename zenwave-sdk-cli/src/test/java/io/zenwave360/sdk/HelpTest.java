package io.zenwave360.sdk;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.plugins.NoOpPluginConfiguration;

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

}
