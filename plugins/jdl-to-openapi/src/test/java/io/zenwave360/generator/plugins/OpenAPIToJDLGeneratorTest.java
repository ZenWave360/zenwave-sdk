package io.zenwave360.generator.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.processors.utils.JSONPath;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenAPIToJDLGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, ?> loadApiModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, ?> model = new DefaultYamlParser().withSpecFile(file.getAbsolutePath()).parse();
        return new OpenApiProcessor().process(model);
    }

    @Test
    public void test_jdl_to_openapi_with_relationships() throws Exception {
        Map<String, ?> model = loadApiModelFromResource("io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        OpenAPIToJDLGenerator generator = new OpenAPIToJDLGenerator();
        generator.useRelationships = true;

        List<TemplateOutput> outputTemplates = generator.generate(model);
        System.out.println(outputTemplates.get(0).getContent());
        Assertions.assertEquals(1, outputTemplates.size());
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("enum PetStatus"));
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("relationship OneToMany "));
        Assertions.assertFalse(outputTemplates.get(0).getContent().contains("address List<Address>"));
    }

    @Test
    public void test_jdl_to_openapi_with_embedded() throws Exception {
        Map<String, ?> model = loadApiModelFromResource("io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        OpenAPIToJDLGenerator generator = new OpenAPIToJDLGenerator();
        generator.useRelationships = false;

        List<TemplateOutput> outputTemplates = generator.generate(model);
        System.out.println(outputTemplates.get(0).getContent());
        Assertions.assertEquals(1, outputTemplates.size());
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("enum PetStatus"));
        Assertions.assertFalse(outputTemplates.get(0).getContent().contains("relationship OneToMany "));
        Assertions.assertTrue(outputTemplates.get(0).getContent().contains("address Address[]"));

    }

}
