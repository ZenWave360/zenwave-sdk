package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;

public class ZDLToAsyncAPIGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    public void test_zdl_to_asyncapi_v2() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.asyncapiVersion = AsyncapiVersionType.v2;
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }


    @Test
    public void test_zdl_to_asyncapi_v3() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }

    @Test
    public void test_zdl_to_asyncapi_relational_v3() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());

        var tmpFile = new File("target/customer-address.yml");
        FileUtils.writeStringToFile(tmpFile, outputTemplates.get(0).getContent(), "UTF-8");
        var api = new DefaultYamlParser().withApiFile(tmpFile.toURI()).parse();

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("username"));
    }
}
