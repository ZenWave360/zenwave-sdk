package io.zenwave360.generator.processors;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.parsers.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AsyncApiProcessorTest {

    String targetProperty = "_api";
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    private Map<String, Model> loadAsyncapiModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        return new DefaultYamlParser(file.getAbsolutePath(), targetProperty).parse();
    }

    @Test
    public void testProcessAsyncApiChannelName() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml");
        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        String channelName = processed.getJsonPath("$.channels.createProductNotification.subscribe.x--channel");
        Assertions.assertEquals("createProductNotification", channelName);
    }

    @Test
    public void testProcessAsyncApiOperationType() throws Exception {
        Map<String, ?> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml");
        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        String operationType = processed.getJsonPath("$.channels.createProductNotification.subscribe.x--operationType");
        Assertions.assertEquals("subscribe", operationType);
    }

    @Test
    public void testProcessAsyncApiTraits() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-shoping-cart.yml");

        String headers = model.get(targetProperty).getJsonPath("$.components.messages.LinesRemoved.headers");
        Assertions.assertEquals(null, headers);

        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        String headerType = processed.getJsonPath("$.components.messages.LinesRemoved.headers.properties.ecommerce-metadata-session.type");
        Assertions.assertEquals("string", headerType);
    }


    @Test
    public void testCollectMessagesOneOf() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-shoping-cart.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<List> messages = processed.getJsonPath("$.channels..x--messages");
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(3, messages.get(0).size());
    }

    @Test
    public void testCollectMessagesSingleMessage() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-circular-refs.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<List> messages = processed.getJsonPath("$.channels..x--messages");
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(1, messages.get(0).size());
    }

    @Test
    public void testCalculateMessagesParamTypeForAvros() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-shoping-cart.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<String> paramTypes = processed.getJsonPath("$..x--messages..x--javaType");
        Assertions.assertEquals(3, paramTypes.size());
    }

    @Test
    public void testCalculateMessagesParamTypeForAsyncAPISchema() throws Exception {
        Map<String, Model> model = loadAsyncapiModelFromResource("io/zenwave360/generator/parsers/asyncapi-javaType.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor(targetProperty);
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<String> paramTypes = processed.getJsonPath("$..x--messages..x--javaType");
        Assertions.assertEquals(4, paramTypes.size());
        Assertions.assertEquals("org.asyncapi.tools.example.event.cart.v1.LinesAddedEvent", paramTypes.get(0));
        Assertions.assertEquals("CreateProduct", paramTypes.get(1));
        Assertions.assertEquals("CreateProductWithSchemaNoName", paramTypes.get(2));
        Assertions.assertEquals("io.example.schema.TransportNotificationEventData", paramTypes.get(3));
    }
}
