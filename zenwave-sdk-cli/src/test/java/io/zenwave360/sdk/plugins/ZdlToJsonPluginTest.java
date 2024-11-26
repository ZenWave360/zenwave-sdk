package io.zenwave360.sdk.plugins;

import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;

import java.util.List;

public class ZdlToJsonPluginTest {

    @Test
    void testZdlToJsonPlugin() throws Exception {
        Plugin plugin = new ZdlToJsonPlugin().withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        new MainGenerator().generate(plugin);
    }

    @Test
    void testZdlToJsonPlugin_MultipleZdlFiles() throws Exception {
        Plugin plugin = new ZdlToJsonPlugin().withZdlFiles(List.of(
                "classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl",
                "classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl"));
        new MainGenerator().generate(plugin);
    }
}
