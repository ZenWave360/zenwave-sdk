package io.zenwave360.generator.plugins;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AbstractAsyncapiGeneratorTest {

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        System.out.println(getClass().getClassLoader().getResource(resource).toURI());
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, Object> model = new DefaultYamlParser().parse(file);
        return new AsyncApiProcessor().process(model);
    }

    private AbstractAsyncapiGenerator newAbstractAsyncapiGenerator() {
        return new AbstractAsyncapiGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, Object> apiModel) {
                return null;
            }
        };
    }


    @Test
    public void test_filter_operations_for_provider_nobindings() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = GeneratorPlugin.RoleType.PROVIDER;
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertEquals("doCreateProduct", consumerOperations.get("DefaultService").get(0).get("operationId"));
        Assertions.assertTrue(producerOperations.isEmpty());
    }

    @Test
    public void test_filter_operations_for_provider_with_matching_bindings() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = GeneratorPlugin.RoleType.PROVIDER;
        asyncapiGenerator.bindingTypes = Arrays.asList("kafka");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertTrue(producerOperations.isEmpty());
    }
    @Test
    public void test_filter_operations_for_provider_with_no_matching_bindings() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("io/zenwave360/generator/plugins/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = GeneratorPlugin.RoleType.PROVIDER;
        asyncapiGenerator.bindingTypes = Arrays.asList("nomatchingbinding");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertTrue(consumerOperations.isEmpty());
        Assertions.assertTrue(producerOperations.isEmpty());
    }
}
