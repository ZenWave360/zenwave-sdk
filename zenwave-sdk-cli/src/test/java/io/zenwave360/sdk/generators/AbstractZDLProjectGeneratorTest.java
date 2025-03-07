package io.zenwave360.sdk.generators;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class AbstractZDLProjectGeneratorTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    private AbstractZDLGenerator zdlProjectGenerator = new AbstractZDLProjectGenerator() {
        @Override
        protected HandlebarsEngine getTemplateEngine() {
            return new NOPHandlebarsEngine();
        }

        @Override
        protected ProjectTemplates configureProjectTemplates() {
            var templates = new ProjectTemplates("");
            templates.entityTemplates.add(new TemplateInput("entity {{entity.name}}", ""));
            templates.enumTemplates.add(new TemplateInput("enum {{enum.name}}", ""));
            templates.inputTemplates.add(new TemplateInput("input {{entity.name}}", ""));
            templates.inputEnumTemplates.add(new TemplateInput("inputEnum {{enum.name}}", ""));
            templates.outputTemplates.add(new TemplateInput("output {{entity.name}}", ""));
            templates.serviceTemplates.add(new TemplateInput("service {{service.name}}", ""));
            templates.domainEventsTemplates.add(new TemplateInput("event {{event.name}}", ""));
            templates.eventEnumTemplates.add(new TemplateInput("event enum {{enum.name}}", ""));
            //                templates.allEntitiesTemplates.add(new TemplateInput("entities {{size entities}}", ""));
            //                templates.allEnumsTemplates.add(new TemplateInput("enums {{size enums}}", ""));
            templates.allEventsTemplates.add(new TemplateInput("events {{size events}}", ""));
            //                templates.allInputsTemplates.add(new TemplateInput("inputs {{size inputs}}", ""));
            //                templates.allOutputsTemplates.add(new TemplateInput("outputs {{size outputs}}", ""));
            templates.allServicesTemplates.add(new TemplateInput("services {{size services}}", ""));
            templates.singleTemplates.add(new TemplateInput("singleTemplate", ""));
            templates.addTemplate(templates.singleTemplates, "src/main/java", "singleTemplate", "basePackage", "singleTemplate.java", null, null, false);
            return templates;
        }

        @Override
        protected boolean isGenerateEntity(java.util.Map<String, Object> entity) {
            return true;
        }
    };

    @Test
    void testAbstractZDLProjectGenerator() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var outputList = zdlProjectGenerator.generate(model);
        Assertions.assertEquals(14, outputList.size());
    }

    @Test
    void testAbstractZDLProjectGeneratorLocalEvents() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var outputList = zdlProjectGenerator.generate(model);
//        outputList.stream().map(TemplateOutput::getContent).forEach(System.out::println);
        Assertions.assertEquals(12, outputList.size());
    }

    @Test
    void testAbstractZDLProjectGeneratorWithAggregates() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var outputList = zdlProjectGenerator.generate(model);
//        outputList.stream().map(TemplateOutput::getContent).forEach(System.out::println);
        Assertions.assertEquals(22, outputList.size());
    }


}
