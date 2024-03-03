package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;

public class ZdlToJsonPluginTest {

    @Test
    void testZdlToJsonPlugin() throws Exception {
        Plugin plugin = new ZdlToJsonPlugin().withSpecFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        new MainGenerator().generate(plugin);
    }
}
