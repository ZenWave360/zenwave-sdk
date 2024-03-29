package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.processors.ZDLProcessor;
import org.apache.commons.io.FileUtils;
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

//        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_customer_address_zdl_to_openapi_fileupload() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/documents.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        FileUtils.writeStringToFile(new File("target/out/openapi-fileupload.yml"), outputTemplates.get(0).getContent(), "UTF-8");

        //        System.out.println(outputTemplates.get(0).getContent());
    }


    @Test
    public void test_operationIdsToIncludeExclude() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();
        generator.operationIdsToInclude = List.of("getCustomer", "listCustomers");
        generator.operationIdsToExclude = List.of("getCustomer");
        var processor = new PathsProcessor();
        processor.operationIdsToInclude = generator.operationIdsToInclude;
        processor.operationIdsToExclude = generator.operationIdsToExclude;
        model = processor.process(model);

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());
        var apiText = outputTemplates.get(0).getContent();

//        System.out.println(apiText);

        Assertions.assertTrue(apiText.contains("listCustomers"));
        Assertions.assertFalse(apiText.contains("getCustomer"));
        Assertions.assertFalse(apiText.contains("updateCustomer"));
    }

    @Test
    public void test_order_faults_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_merge_customer_address_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();
        generator.openapiMergeFile = "classpath:io/zenwave360/sdk/resources/openapi/openapi-merger.yml";

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_overlay_customer_address_zdl_to_openapi() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToOpenAPIGenerator generator = new ZDLToOpenAPIGenerator();
        generator.openapiOverlayFiles = List.of("classpath:/io/zenwave360/sdk/resources/openapi/openapi-overlay.yml");

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

//        System.out.println(outputTemplates.get(0).getContent());
    }


}
