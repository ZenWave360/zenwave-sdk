package io.zenwave360.sdk;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.plugins.NoOpPluginConfiguration;

public class HelpTest {

    @Test
    public void testMainHelp() {
        Main.main("-h", "-p", NoOpPluginConfiguration.class.getName());
    }

    @Test
    public void testDiscoverAvailablePlugins() {
        Main.main("-h", "-f", Help.Format.list.toString());
    }

}
