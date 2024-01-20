package io.zenwave360.sdk.generators;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class AbstractZDLProjectGeneratorTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    void testAbstractZDLProjectGenerator() throws IOException {
        var abstractZDLProjectGenerator = new AbstractZDLProjectGenerator() {
            @Override
            protected HandlebarsEngine getTemplateEngine() {
                return new NOPHandlebarsEngine();
            }

            @Override
            protected ZDLProjectTemplates configureProjectTemplates() {
                var templates = new ZDLProjectTemplates("");
                templates.entityTemplates.add(new TemplateInput("entity {{entity.name}}", ""));
                templates.enumTemplates.add(new TemplateInput("enum {{enum.name}}", ""));
                templates.inputTemplates.add(new TemplateInput("input {{entity.name}}", ""));
                templates.inputEnumTemplates.add(new TemplateInput("inputEnum {{enum.name}}", ""));
                templates.outputTemplates.add(new TemplateInput("output {{entity.name}}", ""));
                templates.serviceTemplates.add(new TemplateInput("service {{service.name}}", ""));
//                templates.eventTemplates.add(new TemplateInput("{{event.name}}", ""));
//                templates.allEntitiesTemplates.add(new TemplateInput("entities {{size entities}}", ""));
//                templates.allEnumsTemplates.add(new TemplateInput("enums {{size enums}}", ""));
//                templates.allEventsTemplates.add(new TemplateInput("events {{size events}}", ""));
//                templates.allInputsTemplates.add(new TemplateInput("inputs {{size inputs}}", ""));
//                templates.allOutputsTemplates.add(new TemplateInput("outputs {{size outputs}}", ""));
                templates.allServicesTemplates.add(new TemplateInput("services {{size services}}", ""));
                templates.singleTemplates.add(new TemplateInput("singleTemplate", ""));
                return templates;
            }

            @Override
            protected boolean isGenerateEntity(java.util.Map<String, Object> entity) {
                return true;
            }
        };

        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var outputList = abstractZDLProjectGenerator.generate(model);
        Assertions.assertEquals(10, outputList.size());
    }

    static class NOPHandlebarsEngine extends HandlebarsEngine {

        @Override
        public String processInline(String template, Map<String, Object> model) {
            try {
                return super.processInline(template, model);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
            public java.util.List<TemplateOutput> processTemplates(Map<String, Object> model, java.util.List<TemplateInput> templateInputs) {
                return templateInputs.stream()
                        .map(templateInput -> new TemplateOutput(templateInput.getTargetFile(), processInline(templateInput.getTemplateLocation(), model)))
                        .collect(java.util.stream.Collectors.toList());
            }
    }
}
