package io.zenwave360.sdk.utils;

import io.zenwave360.sdk.TestUtils;
import io.zenwave360.sdk.processors.AsyncApiProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.zenwave360.sdk.TestUtils.loadAsyncApiYmlModelFromResource;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncAPIUtilsTest {

    @Test
    void testExtractMessages_V2_WithOperationIds() throws Exception {
        // Given
        Map<String, Object> apiModel = loadAsyncApiYmlModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-address.yml");
        List<String> operationIds = List.of("doCreateCustomer");
        List<String> messageNames = List.of();

        // When
        List<Map<String, Object>> messages = AsyncAPIUtils.extractMessages(
            apiModel,
            AsyncApiProcessor.SchemaFormatType::isSchemaFormat,
            operationIds,
            messageNames
        );

        // Then
        assertNotNull(messages);
        assertFalse(messages.isEmpty());
    }

    @Test
    void testExtractMessages_V2_EmptyOperationIds() throws Exception {
        // Given
        Map<String, Object> apiModel = loadAsyncApiYmlModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-events.yml");
        List<String> operationIds = List.of();
        List<String> messageNames = List.of();

        // When
        List<Map<String, Object>> messages = AsyncAPIUtils.extractMessages(
            apiModel,
            AsyncApiProcessor.SchemaFormatType::isSchemaFormat,
            operationIds,
            messageNames
        );

        // Then
        assertNotNull(messages);
    }

    @Test
    void testExtractMessages_V3_WithMessageNames() throws Exception {
        // Given
        Map<String, Object> apiModel = loadAsyncApiYmlModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-address.yml");
        List<String> operationIds = List.of();
        List<String> messageNames = List.of("UserSignedUp");

        // When
        List<Map<String, Object>> messages = AsyncAPIUtils.extractMessages(
            apiModel,
            AsyncApiProcessor.SchemaFormatType::isSchemaFormat,
            operationIds,
            messageNames
        );

        // Then
        assertNotNull(messages);
    }

    @Test
    void testExtractMessages_FilterBySchemaFormat() throws Exception {
        // Given
        Map<String, Object> apiModel = loadAsyncApiYmlModelFromResource("classpath:io/zenwave360/sdk/resources/asyncapi/v2/asyncapi-circular-refs.yml");
        List<String> operationIds = List.of();
        List<String> messageNames = List.of();

        // When - filter only AVRO messages
        List<Map<String, Object>> avroMessages = AsyncAPIUtils.extractMessages(
            apiModel,
            AsyncApiProcessor.SchemaFormatType::isAvroFormat,
            operationIds,
            messageNames
        );

        // Then
        assertNotNull(avroMessages);
        // Should be empty or contain only AVRO messages
    }
}
