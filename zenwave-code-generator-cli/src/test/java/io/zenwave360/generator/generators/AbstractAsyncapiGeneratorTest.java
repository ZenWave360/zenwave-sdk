package io.zenwave360.generator.generators;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.zenwave360.generator.generators.AbstractAsyncapiGenerator.OperationRoleType;
import io.zenwave360.generator.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.generator.options.asyncapi.AsyncapiRoleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.processors.AsyncApiProcessor;
import io.zenwave360.generator.templating.TemplateOutput;

public class AbstractAsyncapiGeneratorTest {

    private Model loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "api";
        Map<String, Object> model = new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty(targetProperty).parse();
        return (Model) new AsyncApiProcessor().withTargetProperty(targetProperty).process(model).get(targetProperty);
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
    public void testOperationRoleType() {
        Assertions.assertEquals(OperationRoleType.EVENT_PRODUCER, OperationRoleType.valueOf(AsyncapiRoleType.provider, AsyncapiOperationType.publish));
        Assertions.assertEquals(OperationRoleType.COMMAND_CONSUMER, OperationRoleType.valueOf(AsyncapiRoleType.provider, AsyncapiOperationType.subscribe));
        Assertions.assertEquals(OperationRoleType.EVENT_CONSUMER, OperationRoleType.valueOf(AsyncapiRoleType.client, AsyncapiOperationType.publish));
        Assertions.assertEquals(OperationRoleType.COMMAND_PRODUCER, OperationRoleType.valueOf(AsyncapiRoleType.client, AsyncapiOperationType.subscribe));

        Assertions.assertTrue(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.provider, AsyncapiOperationType.publish));
        Assertions.assertTrue(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.client, AsyncapiOperationType.subscribe));
    }

    @Test
    public void test_filter_operations_for_provider_nobindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertEquals("doCreateProduct", consumerOperations.get("DefaultService").get(0).get("operationId"));
        Assertions.assertTrue(producerOperations.isEmpty());
    }

    @Test
    public void test_filter_operations_for_provider_with_matching_bindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        asyncapiGenerator.bindingTypes = Arrays.asList("kafka");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertTrue(producerOperations.isEmpty());
    }

    @Test
    public void test_filter_operations_for_provider_with_no_matching_bindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/generator/resources/asyncapi/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        asyncapiGenerator.bindingTypes = Arrays.asList("nomatchingbinding");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertTrue(consumerOperations.isEmpty());
        Assertions.assertTrue(producerOperations.isEmpty());
    }
}
