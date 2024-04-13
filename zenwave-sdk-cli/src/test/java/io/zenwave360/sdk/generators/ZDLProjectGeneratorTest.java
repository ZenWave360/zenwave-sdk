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

public class ZDLProjectGeneratorTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    private ProjectTemplates projectTemplates = new ProjectTemplates() {
        {
            this.entityTemplates.add(new TemplateInput("entity {{entity.name}}", ""));
            this.enumTemplates.add(new TemplateInput("enum {{enum.name}}", ""));
            this.inputTemplates.add(new TemplateInput("input {{entity.name}}", ""));
            this.inputEnumTemplates.add(new TemplateInput("inputEnum {{enum.name}}", ""));
            this.outputTemplates.add(new TemplateInput("output {{entity.name}}", ""));
            this.serviceTemplates.add(new TemplateInput("service {{service.name}}", ""));
            this.domainEventsTemplates.add(new TemplateInput("event {{event.name}}", ""));
            this.eventEnumTemplates.add(new TemplateInput("event enum {{enum.name}}", ""));
            //                this.allEntitiesTemplates.add(new TemplateInput("entities {{size entities}}", ""));
            //                this.allEnumsTemplates.add(new TemplateInput("enums {{size enums}}", ""));
            this.allDomainEventsTemplates.add(new TemplateInput("events {{size events}}", ""));
            //                this.allInputsTemplates.add(new TemplateInput("inputs {{size inputs}}", ""));
            //                this.allOutputsTemplates.add(new TemplateInput("outputs {{size outputs}}", ""));
            this.allServicesTemplates.add(new TemplateInput("services {{size services}}", ""));
            this.singleTemplates.add(new TemplateInput("singleTemplate", ""));
            this.addTemplate(this.singleTemplates, "src/main/java", "singleTemplate", "basePackage", "singleTemplate.java", null, null, false);
        }
    };

    private ZDLProjectGenerator zdlProjectGenerator = new ZDLProjectGenerator() {

        {
            templates = projectTemplates;
        }

        @Override
        protected HandlebarsEngine getTemplateEngine() {
            return new NOPHandlebarsEngine();
        }

    };

    @Test
    void testAbstractZDLProjectGenerator() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var outputList = zdlProjectGenerator.generateProjectFiles(model);
        Assertions.assertEquals(14, outputList.getAllTemplateOutputs().size());
    }

    @Test
    void testAbstractZDLProjectGeneratorLocalEvents() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address-local-events.zdl");
        var outputList = zdlProjectGenerator.generateProjectFiles(model);
//        outputList.stream().map(TemplateOutput::getContent).forEach(System.out::println);
        Assertions.assertEquals(12, outputList.getAllTemplateOutputs().size());
    }

    @Test
    void testAbstractZDLProjectGeneratorWithAggregates() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var outputList = zdlProjectGenerator.generateProjectFiles(model);
        outputList.getAllTemplateOutputs().stream().map(o -> o.getTemplateInput().getTemplateLocation()).forEach(System.out::println);
        Assertions.assertEquals(23, outputList.getAllTemplateOutputs().size());
    }


}
