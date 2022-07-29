package io.zenwave360.generator.plugins;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

public class ForkPluginGeneratorTest {

    @Test
    public void testFork_JDLApplicationDefault() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.sourcePluginClassName = "io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration";
        plugin.targetPluginClassName  = "io.zenwave360.generator.plugins.forked.JDLBackendApplicationDefaultConfigurationForked";
        plugin.targetFolder = "target/forked"; // + System.currentTimeMillis();
        plugin.generate(null);
    }

}
