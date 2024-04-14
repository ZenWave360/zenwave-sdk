package io.zenwave360.sdk.generators;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator.OperationRoleType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiOperationType;
import io.zenwave360.sdk.options.asyncapi.AsyncapiRoleType;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.processors.AsyncApiProcessor;

public class AbstractAsyncapiGeneratorTest {

    private Model loadAsyncapiModelFromResource(String resource) throws Exception {
        String targetProperty = "api";
        Map<String, Object> model = new DefaultYamlParser().withApiFile(URI.create(resource)).withTargetProperty(targetProperty).parse();
        return (Model) new AsyncApiProcessor().process(model).get(targetProperty);
    }

    private AbstractAsyncapiGenerator newAbstractAsyncapiGenerator() {
        return new AbstractAsyncapiGenerator() {
            @Override
            protected HandlebarsEngine getTemplateEngine() {
                return new NOPHandlebarsEngine();
            }

            @Override
            protected Templates configureTemplates() {
                var ts = new Templates("");
                ts.addTemplate(ts.commonTemplates, "", "");
                ts.addTemplate(ts.producerTemplates, "{{services}}", "");
                ts.addTemplate(ts.consumerTemplates, "{{services}}", "");
                ts.addTemplate(ts.producerByServiceTemplates, "{{service}}", "");
                ts.addTemplate(ts.consumerByServiceTemplates, "{{service}}", "");
                ts.addTemplate(ts.producerByOperationTemplates, "{{operation}}", "");
                ts.addTemplate(ts.consumerByOperationTemplates, "{{operation}}", "");
                return ts;
            }
        };
    }

    @Test
    public void test_generate_v2() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-orders-relational.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        var outputList = asyncapiGenerator.generate(Map.of(asyncapiGenerator.sourceProperty, model));
        Assertions.assertEquals(15, outputList.getAllTemplateOutputs().size());
    }

    @Test
    public void test_generate_v3() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-order-asyncapi.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        var outputList = asyncapiGenerator.generate(Map.of(asyncapiGenerator.sourceProperty, model));
        Assertions.assertEquals(11, outputList.getAllTemplateOutputs().size());
    }



    @Test
    public void testOperationRoleType() {
        Assertions.assertEquals(OperationRoleType.EVENT_PRODUCER, OperationRoleType.valueOf(AsyncapiRoleType.provider, AsyncapiOperationType.publish));
        Assertions.assertEquals(OperationRoleType.COMMAND_CONSUMER, OperationRoleType.valueOf(AsyncapiRoleType.provider, AsyncapiOperationType.subscribe));
        Assertions.assertEquals(OperationRoleType.EVENT_CONSUMER, OperationRoleType.valueOf(AsyncapiRoleType.client, AsyncapiOperationType.publish));
        Assertions.assertEquals(OperationRoleType.COMMAND_PRODUCER, OperationRoleType.valueOf(AsyncapiRoleType.client, AsyncapiOperationType.subscribe));

        Assertions.assertTrue(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.provider, AsyncapiOperationType.publish));
        Assertions.assertTrue(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.client, AsyncapiOperationType.subscribe));
        Assertions.assertFalse(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.provider, AsyncapiOperationType.subscribe));
        Assertions.assertFalse(newAbstractAsyncapiGenerator().isProducer(AsyncapiRoleType.client, AsyncapiOperationType.publish));
    }

    @Test
    public void test_filter_operations_for_provider_nobindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertEquals("doCreateProduct", consumerOperations.get("DefaultService").get(0).get("operationId"));
        Assertions.assertTrue(producerOperations.isEmpty());
    }

    @Test
    public void test_filter_operations_for_provider_nobindings_includes() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        asyncapiGenerator.operationIds = Arrays.asList("doCreateProduct");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(1, consumerOperations.size());
        Assertions.assertEquals("doCreateProduct", consumerOperations.get("DefaultService").get(0).get("operationId"));
        Assertions.assertTrue(producerOperations.isEmpty());
    }

    @Test
    public void test_filter_operations_for_provider_nobindings_excludes() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        asyncapiGenerator.excludeOperationIds = Arrays.asList("doCreateProduct");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(0, consumerOperations.size());
    }


    @Test
    public void test_filter_operations_for_provider_nobindings_v3() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-order-asyncapi.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertEquals(2, consumerOperations.size());
        Assertions.assertEquals("doCustomerRequest", consumerOperations.get("Customer").get(0).get("operationId"));
        Assertions.assertEquals(2, producerOperations.size());
        Assertions.assertEquals("onCustomerEvent", producerOperations.get("Customer").get(0).get("operationId"));
    }

    @Test
    public void test_filter_operations_for_provider_with_matching_bindings() throws Exception {
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
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
        Model model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AbstractAsyncapiGenerator asyncapiGenerator = newAbstractAsyncapiGenerator();
        asyncapiGenerator.role = AsyncapiRoleType.provider;
        asyncapiGenerator.bindingTypes = Arrays.asList("nomatchingbinding");
        Map<String, List<Map<String, Object>>> consumerOperations = asyncapiGenerator.getSubscribeOperationsGroupedByTag(model);
        Map<String, List<Map<String, Object>>> producerOperations = asyncapiGenerator.getPublishOperationsGroupedByTag(model);
        Assertions.assertTrue(consumerOperations.isEmpty());
        Assertions.assertTrue(producerOperations.isEmpty());
    }
}
