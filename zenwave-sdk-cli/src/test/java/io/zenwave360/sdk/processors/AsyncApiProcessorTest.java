package io.zenwave360.sdk.processors;

import static io.zenwave360.sdk.utils.JSONPath.get;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;

public class AsyncApiProcessorTest {

    String targetProperty = "api";
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        return new DefaultYamlParser().withSpecFile(URI.create(resource)).withTargetProperty(targetProperty).parse();
    }

    @Test
    public void testProcessAsyncApiChannelName() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        String channelName = get(processed, "$.channels.createProductNotification.subscribe.x--channel");
        Assertions.assertEquals("createProductNotification", channelName);
    }

    @Test
    public void testProcessAsyncApiOperationType() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        String operationType = get(processed, "$.channels.createProductNotification.subscribe.x--operationType");
        Assertions.assertEquals("subscribe", operationType);
    }

    @Test
    public void testProcessAsyncApiTraits() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-shoping-cart.yml");

        String headers = get(model.get(targetProperty), "$.components.messages.LinesRemoved.headers");
        Assertions.assertEquals(null, headers);

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        String headerType = get(processed, "$.components.messages.LinesAdded.headers.properties.ecommerce-metadata-session.type");
        Assertions.assertEquals("string", headerType);
    }

    @Test
    public void testProcessAsyncApiMergeTraits() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-shoping-cart.yml");

        Object headers = get(model.get(targetProperty), "$.components.messages.LinesAdded.headers");
        Assertions.assertNotNull(get(headers, "$.properties.some-header.type"));

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        String headerType = get(processed, "$.components.messages.LinesAdded.headers.properties.some-header.type");
        Assertions.assertEquals("string", headerType);
        String headerType2 = get(processed, "$.components.messages.LinesAdded.headers.properties.ecommerce-metadata-session.type");
        Assertions.assertEquals("string", headerType2);
    }

    @Test
    public void testCollectMessagesOneOf() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-shoping-cart.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<List> messages = get(processed, "$.channels..x--messages");
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(3, messages.get(0).size());
    }

    @Test
    public void testCollectMessagesSingleMessage() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<List> messages = get(processed, "$.channels..x--messages");
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(1, messages.get(0).size());
    }

    @Test
    public void testCalculateMessagesParamTypeForAvros() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-shoping-cart.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<String> paramTypes = get(processed, "$..x--messages..x--javaType");
        Assertions.assertEquals(3, paramTypes.size());
    }

    @Test
    public void testCalculateMessagesParamTypeForAsyncAPISchema() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-javaType.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<String> paramTypes = get(processed, "$..x--messages..x--javaType");
        Assertions.assertEquals(4, paramTypes.size());
        Assertions.assertEquals("org.asyncapi.tools.example.event.cart.v1.LinesAddedEvent", paramTypes.get(0));
        Assertions.assertEquals("ProductPayload", paramTypes.get(1));
        Assertions.assertEquals("ProductPayload", paramTypes.get(2));
        Assertions.assertEquals("io.example.schema.TransportNotificationEventData", paramTypes.get(3));
    }
}
