package io.zenwave360.sdk.utils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;

public class CommaSeparatedCollectionDeserializationHandler extends DeserializationProblemHandler {

    public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta, JsonParser p, String msg) throws IOException {
        JavaType type = ctxt.getTypeFactory().constructType(instClass);
        if (p.currentToken() == JsonToken.VALUE_STRING && (type.isCollectionLikeType() || type.isArrayType())) {
            List deserialized = deserializeAsList(type, p);
            if (type.isCollectionLikeType()) {
                Collection collection = (Collection) valueInsta.createUsingDefault(ctxt);
                collection.addAll(deserialized);
                return collection;
            }
            if(type.isArrayType()) {
                var array = Array.newInstance(type.getRawClass().getComponentType(), deserialized.size());
                return deserialized.toArray((Object[]) array);
            }
        }
        return super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg);
    }

    private List deserializeAsList(JavaType listType, JsonParser parser) throws IOException {
        String[] values = parser.getText().split(",");

        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        List<Object> result = new ArrayList<>();
        for (String value : values) {
            Object item = mapper.readValue("\"" + value.trim() + "\"", listType.getContentType());
            result.add(item);
        }

        return result;
    }

}
