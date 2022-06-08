package io.zenwave360.generator.plugins;

import io.zenwave360.generator.GeneratorPlugin;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AbstractAsyncapiGeneratorTest {

    private Model loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "api";
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, ?> model = new DefaultYamlParser().withSpecFile(file.getAbsolutePath()).withTargetProperty(targetProperty).parse();
        return (Model) new AsyncApiProcessor().withTargetProperty(targetProperty).process(model).get(targetProperty);
    }

    private AbstractAsyncapiGenerator newAbstractAsyncapiGenerator() {
        return new AbstractAsyncapiGenerator() {
            @Override
            public List<TemplateOutput> generate(Map<String, ?> apiModel) {
                return null;
            }
        };
    }


    @Test
    public void test_filter_operations_for_provider_nobindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
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
        Model model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
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
        Model model = loadAsyncapiModelFromResource("io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = GeneratorPlugin.RoleType.PROVIDER;
        asyncapiGenerator.bindingTypes = Arrays.asList("nomatchingbinding");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertTrue(consumerOperations.isEmpty());
        Assertions.assertTrue(producerOperations.isEmpty());
    }
}
