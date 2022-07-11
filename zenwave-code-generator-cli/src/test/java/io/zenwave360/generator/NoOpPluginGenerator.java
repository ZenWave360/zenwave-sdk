package io.zenwave360.generator;

import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;
import java.util.Map;

public class NoOpPluginGenerator implements GeneratorPlugin {

    public static Map<String, Object> context;

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        context = contextModel;
        return List.of(new TemplateOutput("nop.txt", "nop"));
    }
}
