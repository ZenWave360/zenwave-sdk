package io.zenwave360.sdk.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class ObjectInstantiatorDeserializationHandler extends DeserializationProblemHandler {

    @Override
    public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta, JsonParser p, String msg) throws IOException {
        if (p.currentToken().isScalarValue()) {
            String value = p.getValueAsString();
            if (value != null && value.startsWith("new ")) {
                String className = StringUtils.substringAfter(value, "new ").replaceAll("\\(\\)$", "").trim();
                try {
                    Class<?> clazz = Class.forName(className);
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    if (instClass.isAssignableFrom(instance.getClass())) {
                        return instance;
                    }
                } catch (Exception e) {
                    throw new IOException("Failed to instantiate class: " + className, e);
                }
            }
        }
        return super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg);
    }
}
