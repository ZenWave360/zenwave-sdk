package io.zenwave360.sdk.plugins;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ForkPluginGeneratorTest {

    @Test
    public void testFork_JDLApplicationDefault() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-sdk/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.sdk.plugins.JDLBackendApplicationDefaultPlugin";
        plugin.targetPluginClassName = "io.zenwave360.sdk.plugins.forked.JDLBackendApplicationDefaultPluginForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }

    @Test
    public void testFork_JDLApplicationDefault_SamePackage() throws MalformedURLException {
        ForkPluginGenerator plugin = new ForkPluginGenerator();
        plugin.downloadURL = new URL("https://github.com/ZenWave360/zenwave-sdk/archive/refs/heads/main.zip");
        plugin.sourcePluginClassName = "io.zenwave360.sdk.plugins.JDLBackendApplicationDefaultPlugin";
        plugin.targetPluginClassName = "io.zenwave360.sdk.plugins.JDLBackendApplicationDefaultPluginForked";
        plugin.targetFolder = "target/forked" + System.currentTimeMillis();
        plugin.generate(null);
    }
}
