package io.zenwave360.sdk.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.layouts.DefaultProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;

public class NoOpGenerator implements Generator {

    @DocumentedOption(description = "Project layout")
    public ProjectLayout layout = new DefaultProjectLayout();

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
