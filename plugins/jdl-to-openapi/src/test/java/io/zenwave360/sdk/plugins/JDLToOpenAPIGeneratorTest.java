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

public class JDLToOpenAPIGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        return new JDLProcessor().process(model);
    }

    @Test
    public void test_jdl_to_openapi() throws Exception {
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        JDLToOpenAPIGenerator generator = new JDLToOpenAPIGenerator();

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.OrderStatus.enum")).contains("DELIVERED"));
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("firstName"));
        Assertions.assertEquals("3", JSONPath.get(oasSchema, "$.components.schemas.Customer.properties.firstName.minLength").toString());
        Assertions.assertEquals("#/components/schemas/OrderStatus", JSONPath.get(oasSchema, "$.components.schemas.CustomerOrder.properties.status.$ref"));
    }

    @Test
    public void test_jdl_to_openapi_integer_id() throws Exception {
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        JDLToOpenAPIGenerator generator = new JDLToOpenAPIGenerator();
        generator.idType = "integer";
        generator.idTypeFormat = "int64";

        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());

        System.out.println(outputTemplates.get(0).getContent());

        Map<String, Object> oasSchema = mapper.readValue(outputTemplates.get(0).getContent(), Map.class);
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.OrderStatus.enum")).contains("DELIVERED"));
        Assertions.assertTrue(((List) JSONPath.get(oasSchema, "$.components.schemas.Customer.required")).contains("firstName"));
        Assertions.assertEquals("3", JSONPath.get(oasSchema, "$.components.schemas.Customer.properties.firstName.minLength").toString());
        Assertions.assertEquals("#/components/schemas/OrderStatus", JSONPath.get(oasSchema, "$.components.schemas.CustomerOrder.properties.status.$ref"));
    }

}