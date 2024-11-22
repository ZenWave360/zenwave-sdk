package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;

import java.util.Map;

public interface ConfigurationProvider {

    void updateConfiguration(Plugin configuration, Map<String, Object> model);
}
