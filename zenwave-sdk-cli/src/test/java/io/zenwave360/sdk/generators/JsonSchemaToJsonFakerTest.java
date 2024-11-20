package io.zenwave360.sdk.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.Map;

public class JsonSchemaToJsonFakerTest {

    private JsonSchemaToJsonFaker jsonSchemaToJsonFaker = new JsonSchemaToJsonFaker();

    private Map<String, Object> loadOpenAPIModelFromResource(String resource) throws Exception {
        return new DefaultYamlParser().withApiFile(URI.create(resource)).parse();
    }

    @ParameterizedTest
    @CsvSource({
            "classpath:io/zenwave360/sdk/resources/openapi/openapi-petstore.yml",
            "classpath:io/zenwave360/sdk/resources/openapi/customer-address-openapi.yml",
            "classpath:io/zenwave360/sdk/resources/openapi/openapi-orders.yml",
            "classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-address.yml",
            "classpath:io/zenwave360/sdk/resources/asyncapi/v3/customer-order-asyncapi.yml",
    })
    public void generateJsonFakerExample_Petstore(String resource) throws Exception {
        Map<String, Object> model = loadOpenAPIModelFromResource(resource);
        Map<String, Map<String, Object>> schemas = JSONPath.get(model, "$.api.components.schemas");
        for (var keyValue : schemas.entrySet()) {
            String jsonExample = jsonSchemaToJsonFaker.generateExampleAsJson(keyValue.getValue());
            System.out.println(keyValue.getKey() + " = " + jsonExample);
        }
    }

    private String asJson(Object object) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(object);
    }
}
