package io.zenwave360.sdk.plugins;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;

public class OpenAPIToJDLGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadApiModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withApiFile(URI.create(resource)).parse();
        return new OpenApiProcessor().process(model);
    }

    @Test
    public void test_jdl_to_openapi_with_relationships() throws Exception {
        Map<String, Object> model = loadApiModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        OpenAPIToJDLGenerator generator = new OpenAPIToJDLGenerator();
        generator.useRelationships = true;

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        System.out.println(outputTemplates.get(0).getContent());
        Assertions.assertEquals(1, outputTemplates.size());
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("enum PetStatus"));
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("relationship OneToMany "));
        Assertions.assertFalse(outputTemplates.get(0).getContent().contains("address List<Address>"));
    }

    @Test
    public void test_jdl_to_openapi_with_embedded() throws Exception {
        Map<String, Object> model = loadApiModelFromResource("classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml");
        OpenAPIToJDLGenerator generator = new OpenAPIToJDLGenerator();
        generator.useRelationships = false;

        List<TemplateOutput> outputTemplates = generator.generate(model).getAllTemplateOutputs();
        System.out.println(outputTemplates.get(0).getContent());
        Assertions.assertEquals(1, outputTemplates.size());
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("enum PetStatus"));
        Assertions.assertFalse(outputTemplates.get(0).getContent().contains("relationship OneToMany "));
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("address Address[]"));

    }

}
