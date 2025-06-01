package io.zenwave360.sdk.templates;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Options;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;

public class HandlebarsEngineTest {

    @Test
    public void testHandlebarsEngine() throws IOException {
        HandlebarsEngine handlebarsEngine = new HandlebarsEngine();
        handlebarsEngine.getHandlebars().registerHelpers(FirstHelper.class);
        handlebarsEngine.getHandlebars().registerHelpers(SecondHelper.class);

        Map<String, Object> model = new HashMap<>();
        model.put("list", List.of(1, 2, 3));
        model.put("booleanValue", true);
        model.put("nullValue", null);
        model.put("name", "nameFromParent");
        model.put("entities", Map.of("entity1", Map.of("name", "entity1"), "entity2", Map.of("name", "entity2")));
        String json = """
{
  "id" : 60,
  "name" : "doggie",
  "category" : {
    "id" : 46,
    "name" : "Dogs"
  },
  "photoUrls" : [ "photoUrls-fxqn80w4m" ],
  "tags" : [ {
    "id" : 75,
    "name" : "name-vfrtwybo0y7lpiy42pyzt"
  } ],
  "status" : "available"
}
        """;
        model.put("json", json);
        TemplateOutput templateOutput = handlebarsEngine.processTemplate(model, new TemplateInput("io/zenwave360/sdk/templating/handlebars-test", "").withSkipOverwrite(true));
        System.out.println(templateOutput.getContent());

        Assertions.assertTrue(templateOutput.getContent().contains("This is the assigned value"));
        Assertions.assertTrue(templateOutput.getContent().contains("List size is 3"));
        Assertions.assertTrue(templateOutput.getContent().contains("upperCase"));
        Assertions.assertTrue(templateOutput.getContent().contains("asInstanceName tratraTratra"));
        Assertions.assertTrue(templateOutput.getContent().contains("asJavaTypeName TratraTratra"));
        Assertions.assertTrue(templateOutput.getContent().contains("kebabCase some-camel-case-with-spaces"));
        Assertions.assertTrue(templateOutput.getContent().contains("asPackageFolder io/zenwave360/sdk/templating"));
        Assertions.assertTrue(templateOutput.getContent().contains("helperFunction: second"));
        Assertions.assertTrue(templateOutput.getContent().contains("jsonPath entity1"));
        Assertions.assertTrue(templateOutput.getContent().contains("Prefix2Suffix"));
        Assertions.assertTrue(templateOutput.getContent().contains("ifTruthy true: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("ifTruthy false: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("Inside if 1"));
        Assertions.assertTrue(templateOutput.getContent().contains("Inside else 2"));
        Assertions.assertTrue(templateOutput.getContent().contains("Inside if 3"));
        Assertions.assertTrue(templateOutput.getContent().contains("path: 'api/v1/users'"));
        Assertions.assertTrue(templateOutput.getContent().contains("This is from partial"));
        Assertions.assertTrue(templateOutput.getContent().contains("Starts with: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("Ends with: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with string: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with boolean: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("Not with null: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("and true: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("and false: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("or true: true"));
        Assertions.assertTrue(templateOutput.getContent().contains("or false: false"));
        Assertions.assertTrue(templateOutput.getContent().contains("    {\n      \"id\" : 60,"));
    }

    public static class FirstHelper {
        public static String helperFunction(String property, Options options) {
            return "first";
        }
    }
    public static class SecondHelper {
        public static String helperFunction(String property, Options options) {
            return "second";
        }

    }
}
