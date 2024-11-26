package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ConfigurationProviderTest {

    @Test
    void testConfigurationProvider() throws Exception {
        Plugin plugin = new ZdlToJsonPlugin()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        new MainGenerator().generate(plugin);
    }

}
