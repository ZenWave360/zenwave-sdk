package io.zenwave360.sdk.plugins;

import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.processors.ZDLProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.templating.TemplateOutput;

public class ZDLToOpenAPIGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        return model;
    }

    @Test
    public void test_customer_address_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_order_faults_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }

}
