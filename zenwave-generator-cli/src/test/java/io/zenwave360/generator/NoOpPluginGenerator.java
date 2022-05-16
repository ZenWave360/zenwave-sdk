package io.zenwave360.generator;

import io.zenwave360.generator.plugins.GeneratorPlugin;
import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;
import java.util.Map;

public class NoOpPluginGenerator implements GeneratorPlugin {

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        return List.of(new TemplateOutput("nop.txt", "nop"));
    }
}
