package io.zenwave360.sdk.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@JsonDeserialize(using = MultiValueMap.MultiValueMapDeserializer.class)
public class MultiValueMap<T extends Serializable> extends LinkedHashMap<String, List<T>> {

    public void add(String key, T value) {
        List<T> values = this.computeIfAbsent(key, k -> new ArrayList<>());
        values.add(value);
    }

    public static class MultiValueMapDeserializer extends JsonDeserializer<MultiValueMap> {
        @Override
        public MultiValueMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            MultiValueMap<String> result = new MultiValueMap<>();

            if (p.currentToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected START_OBJECT");
            }

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String key = p.getCurrentName();
                p.nextToken();

                if (p.currentToken() == JsonToken.START_ARRAY) {
                    // Handle standard JSON serialization: {"key": ["value1", "value2"]}
                    while (p.nextToken() != JsonToken.END_ARRAY) {
                        String value = p.getValueAsString();
                        result.add(key, value);
                    }
                } else {
                    // Handle flat object with duplicate keys: {"key": "value"}
                    String value = p.getValueAsString();
                    result.add(key, value);
                }
            }

            return result;
        }
    }
}
