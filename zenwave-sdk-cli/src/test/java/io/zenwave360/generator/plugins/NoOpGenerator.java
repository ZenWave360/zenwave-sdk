package io.zenwave360.generator.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.Generator;
import io.zenwave360.generator.templating.TemplateOutput;

public class NoOpGenerator implements Generator {

    @DocumentedOption(description = "This is the plugin option description")
    public String pluginOption;

    public List<String> array = new ArrayList<>();

    public static Map<String, Object> context;

    @Override
    public List<TemplateOutput> generate(Map<String, Object> contextModel) {
        context = contextModel;
        return List.of(new TemplateOutput("nop.txt", "nop"), new TemplateOutput("nop.txt", "nop", "text/plain", true));
    }
}
