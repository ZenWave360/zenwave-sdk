package io.zenwave360.generator.plugins;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;

public class PactConsumerGeneratorTest {

    private Map<String, Object> loadApiModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).parse();
        return new OpenApiProcessor().process(model);
    }

    @Test
    public void test_output_partial_one_operation() throws Exception {
        Map<String, Object> model = loadApiModelFromResource("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        PactConsumerGenerator generator = new PactConsumerGenerator();
        generator.groupBy = PactConsumerGenerator.GroupByType.partial;
        generator.basePackage = "io.example";
        generator.openApiApiPackage = "io.example.api";
        generator.openApiModelPackage = "io.example.api.model";
        generator.operationIds = List.of("addPet");
        List<TemplateOutput> outputTemplates = generator.generate(model);
        Assertions.assertEquals(1, outputTemplates.size());
//        Assertions.assertEquals("io/example/api/AddPetConsumerContractTest.java", outputTemplates.get(0).getTargetFile());
        System.out.println(outputTemplates.get(0).getContent());
    }

    @Test
    public void test_output_by_operation() throws Exception {
        Map<String, Object> model = loadApiModelFromResource("classpath:io/zenwave360/generator/resources/openapi/openapi-petstore.yml");
        PactConsumerGenerator generator = new PactConsumerGenerator();
        generator.groupBy = PactConsumerGenerator.GroupByType.operation;
        generator.basePackage = "io.example";
        generator.openApiApiPackage = "io.example.api";
        generator.openApiModelPackage = "io.example.api.model";
        List<TemplateOutput> outputTemplates = generator.generate(model);
//        Assertions.assertEquals(2, outputTemplates.size());
//        Assertions.assertEquals("io/example/api/AddPetConsumerContractTest.java", outputTemplates.get(0).getTargetFile());
        System.out.println(outputTemplates.get(0).getContent());
    }
}
