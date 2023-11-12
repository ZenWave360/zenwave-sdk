package io.zenwave360.sdk.plugins;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ForkPluginGeneratorTest {

    @Test
    public void testFork_JDLApplicationDefault() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-sdk/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin"; // FIXME: replace this plugin name when promoted to main
        plugin.targetPluginClassName = "io.zenwave360.sdk.plugins.forked.BackendApplicationDefaultPluginForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }

    @Test
    public void testFork_JDLApplicationDefault_SamePackage() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-sdk/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.sdk.plugins.BackendApplicationDefaultPlugin";
        plugin.targetPluginClassName = "io.zenwave360.sdk.plugins.BackendApplicationDefaultPluginForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }
}
