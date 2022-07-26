package io.zenwave360.generator;

import io.zenwave360.generator.plugins.NoOpPluginConfiguration;
import org.junit.jupiter.api.Test;

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
