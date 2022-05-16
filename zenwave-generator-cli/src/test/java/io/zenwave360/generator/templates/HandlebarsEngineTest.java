package io.zenwave360.generator.templates;

import io.zenwave360.generator.plugins.GeneratorPlugin;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlebarsEngineTest {

    @Test
    public void testHandlebarsEngine() {
        HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

        Map<String, Object> model = new HashMap<>();
        model.put("list", List.of(1, 2, 3));
        TemplateOutput templateOutput = handlebarsEngine.processTemplate(model, new TemplateInput("io/zenwave360/generator/templating/handlebars-test", ""));

        Assertions.assertTrue(templateOutput.getContent().contains("This is the assigned value"));
        Assertions.assertTrue(templateOutput.getContent().contains("List size is 3"));
        Assertions.assertTrue(templateOutput.getContent().contains("upperCase"));
    }
}
