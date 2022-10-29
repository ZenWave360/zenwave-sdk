package io.zenwave360.generator.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;

public class HandlebarsEngineTest {

    @Test
    public void testHandlebarsEngine() {
        HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

        Map<String, Object> model = new HashMap<>();
        model.put("list", List.of(1, 2, 3));
        model.put("booleanValue", true);
        model.put("nullValue", null);
        TemplateOutput templateOutput = handlebarsEngine.processTemplate(model, new TemplateInput("io/zenwave360/generator/templating/handlebars-test", "")).get(0);
//         System.out.println(templateOutput.getContent());

        Assertions.assertTrue(templateOutput.getContent().contains("This is the assigned value"));
        Assertions.assertTrue(templateOutput.getContent().contains("List size is 3"));
        Assertions.assertTrue(templateOutput.getContent().contains("upperCase"));
        Assertions.assertTrue(templateOutput.getContent().contains("asInstanceName tratraTratra"));
        Assertions.assertTrue(templateOutput.getContent().contains("asJavaTypeName TratraTratra"));
        Assertions.assertTrue(templateOutput.getContent().contains("kebabCase some-camel-case-with-spaces"));
        Assertions.assertTrue(templateOutput.getContent().contains("asPackageFolder io/zenwave360/generator/templating"));
        Assertions.assertTrue(templateOutput.getContent().contains("Prefix2Suffix"));
        Assertions.assertTrue(templateOutput.getContent().contains("Inside if 1"));
        Assertions.assertTrue(templateOutput.getContent().contains("Inside else 2"));
        Assertions.assertTrue(templateOutput.getContent().contains("This is from partial"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with string: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with boolean: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with null: true"));
    }
}
