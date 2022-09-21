package io.zenwave360.generator.plugins;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class ForkPluginGeneratorTest {

    @Test
    public void testFork_JDLApplicationDefault() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-code-generator/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration";
        plugin.targetPluginClassName = "io.zenwave360.generator.plugins.forked.JDLBackendApplicationDefaultConfigurationForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }

    @Test
    public void testFork_JDLApplicationDefault_SamePackage() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-code-generator/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfiguration";
        plugin.targetPluginClassName = "io.zenwave360.generator.plugins.JDLBackendApplicationDefaultConfigurationForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }
}
