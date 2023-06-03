package io.zenwave360.sdk.plugins;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;

public class ZDLToOpenAPIGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        model = new JDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        return model;
    }

    @Test
    public void test_customer_address_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        JDLToOpenAPIGenerator generator = new JDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_order_faults_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        JDLToOpenAPIGenerator generator = new JDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());
    }

}
