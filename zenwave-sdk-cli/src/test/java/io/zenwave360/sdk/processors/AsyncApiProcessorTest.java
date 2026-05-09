package io.zenwave360.sdk.processors;

import static io.zenwave360.sdk.utils.JSONPath.get;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;

import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.Model;

public class AsyncApiProcessorTest {

    String targetProperty = "api";
    Configuration config = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    private Map<String, Object> loadAsyncapiModelFromResource(String resource) throws Exception {
        return new DefaultYamlParser().withApiFile(URI.create(resource)).withTargetProperty(targetProperty).parse();
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
    public void testCollectChannelMessagesV3() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-address.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);
        List<List> channeldWithMessages = get(processed, "$.channels..x--messages");
        Assertions.assertEquals(3, channeldWithMessages.size());
        Assertions.assertEquals(1, channeldWithMessages.get(0).size());
        Assertions.assertEquals(1, channeldWithMessages.get(1).size());
        Assertions.assertEquals(3, channeldWithMessages.get(2).size());
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

    @Test
    public void testProcessAsyncApiMarksRuntimeHeadersAndOriginalSchemaRefs() throws Exception {
        Map<String, Object> model = loadAsyncapiModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml");

        AsyncApiProcessor processor = new AsyncApiProcessor();
        Model processed = (Model) processor.process(model).get(targetProperty);

        Assertions.assertEquals(true, get(processed, "$.channels.createProductNotification2.publish.x--has-runtime-headers"));
        Assertions.assertEquals("CreateProductPayload", get(processed, "$.components.messages.createProductMsg.payload.x--schema-name"));
        Assertions.assertEquals("#/components/schemas/CreateProductPayload", get(processed, "$.components.messages.createProductMsg.payload.x--original-$ref"));
    }

    @ParameterizedTest
    @MethodSource("schemaFormats")
    public void testSchemaFormatTypeHelpers(String schemaFormat, AsyncApiProcessor.SchemaFormatType expectedType,
                                            boolean schemaFormatType, boolean jsonSchemaFormat, boolean avroFormat,
                                            boolean nativeFormat, boolean yamlFormat) {
        AsyncApiProcessor.SchemaFormatType resolved = AsyncApiProcessor.SchemaFormatType.getFormat(schemaFormat);

        Assertions.assertEquals(expectedType, resolved);
        Assertions.assertEquals(schemaFormatType, AsyncApiProcessor.SchemaFormatType.isSchemaFormat(resolved));
        Assertions.assertEquals(jsonSchemaFormat, AsyncApiProcessor.SchemaFormatType.isJsonSchemaFormat(resolved));
        Assertions.assertEquals(avroFormat, AsyncApiProcessor.SchemaFormatType.isAvroFormat(resolved));
        Assertions.assertEquals(nativeFormat, AsyncApiProcessor.SchemaFormatType.isNativeFormat(resolved));
        Assertions.assertEquals(yamlFormat, AsyncApiProcessor.SchemaFormatType.isYamlFormat(resolved));
    }

    @Test
    public void testSchemaFormatTypeFallbacksForUnknownAndNullValues() {
        Assertions.assertEquals(AsyncApiProcessor.SchemaFormatType.ASYNCAPI_YAML, AsyncApiProcessor.SchemaFormatType.getFormat(null));
        Assertions.assertNull(AsyncApiProcessor.SchemaFormatType.getFormat("application/unknown;version=1.0.0"));
        Assertions.assertTrue(AsyncApiProcessor.SchemaFormatType.isSchemaFormat(null));
        Assertions.assertTrue(AsyncApiProcessor.SchemaFormatType.isNativeFormat(null));
        Assertions.assertFalse(AsyncApiProcessor.SchemaFormatType.isYamlFormat(null));
        Assertions.assertEquals("application/vnd.apache.avro+json;version=3.0.0",
                AsyncApiProcessor.SchemaFormatType.AVRO_JSON.getSchemaFormat("3.0.0"));
    }

    private static Stream<Arguments> schemaFormats() {
        return Stream.of(
                Arguments.of("application/vnd.aai.asyncapi+yaml;version=3.0.0", AsyncApiProcessor.SchemaFormatType.ASYNCAPI_YAML, true, false, false, true, true),
                Arguments.of("application/vnd.oai.openapi+json;version=3.0.0", AsyncApiProcessor.SchemaFormatType.OPENAPI_JSON, true, false, false, true, false),
                Arguments.of("application/schema+json;version=draft-07", AsyncApiProcessor.SchemaFormatType.JSONSCHEMA_JSON, true, true, false, false, false),
                Arguments.of("application/vnd.apache.avro+yaml;version=1.9.0", AsyncApiProcessor.SchemaFormatType.AVRO_YAML, false, false, true, false, true)
        );
    }
}
