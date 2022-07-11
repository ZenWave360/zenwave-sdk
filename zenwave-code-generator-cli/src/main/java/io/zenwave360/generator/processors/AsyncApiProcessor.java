package io.zenwave360.generator.processors;

import com.jayway.jsonpath.JsonPath;
import io.zenwave360.generator.processors.utils.JSONPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AsyncApiProcessor extends AbstractBaseProcessor implements Processor {

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        Map<String, Object> apiModel = targetProperty != null? (Map<String, Object>) contextModel.get(targetProperty) : contextModel;

        List<Map<String, Object>> traitsParents = JSONPath.get(apiModel, "$..[?(@.traits)]");
        for (Map<String, Object> traitParent : traitsParents) {
            List<Map<String, Object>> traitsList = (List) traitParent.get("traits");
            for (Map<String, Object> traits : traitsList) {
                traitParent.putAll(traits);
            }
        }

        Map<String, Object> channels = JSONPath.get(apiModel, "$.channels", Collections.emptyMap());
        for (Map.Entry<String, Object> channel : channels.entrySet()) {
            Map<String, Map<String, Object>> value = (Map) channel.getValue();
            if(value != null){
                addChannelNameToOperation(value.get("publish"), channel.getKey());
                addChannelNameToOperation(value.get("subscribe"), channel.getKey());
                addOperationType(value.get("publish"), "publish");
                addOperationType(value.get("subscribe"), "subscribe");
                addNormalizedTagName(value.get("publish"));
                addNormalizedTagName(value.get("subscribe"));
                addOperationIdVariants(value.get("publish"));
                addOperationIdVariants(value.get("subscribe"));
                collectMessages(value.get("publish"));
                collectMessages(value.get("subscribe"));
            }
        }

        Map<String, Map> componentsMessages = JSONPath.get(apiModel, "$.components.messages", Collections.emptyMap());
        for(Map.Entry<String, Map> message: componentsMessages.entrySet()) {
            if(!message.getValue().containsKey("name")) {
                message.getValue().put("name", message.getKey());
            }
        }

        List<Map<String, Object>> messages = JSONPath.get(apiModel, "$.channels..x--messages[*]");
        for (Map<String, Object> message : messages) {
            calculateMessageParamType(message);
        }

        return contextModel;
    }

    private void addChannelNameToOperation(Map<String, Object> operation, String channelName) {
        if (operation != null) {
            operation.put("x--channel", channelName);
        }
    }

    private void addOperationType(Map<String, Object> operation, String operationType) {
        if (operation != null) {
            operation.put("x--operationType", operationType);
        }
    }

     public void collectMessages(Map<String, Object> operation) {
        if (operation != null) {
            Map message = (Map) operation.get("message");
            List messages = new ArrayList();
            if(message.containsKey("oneOf")) {
                messages.addAll((List) message.get("oneOf"));
            } else {
                messages.add(message);
            }
            operation.put("x--messages", messages);
        }
    }

    public void calculateMessageParamType(Map<String, Object> message) {
        String schemaFormat = normalizeSchemaFormat((String) message.get("schemaFormat"));
        String javaType = null;
        if("avro".equals(schemaFormat)) {
            String name = JsonPath.read(message, "payload.name");
            String namespace = JsonPath.read(message, "payload.namespace");
            javaType = namespace + "." + name;
        }
        if("jsonSchema".equals(schemaFormat)) {
            javaType = JsonPath.read(message, "payload.javaType");
        }
        if("asyncapi".equals(schemaFormat) || "openapi".equals(schemaFormat)) {
            javaType = normalizeTagName((String) message.getOrDefault("x-javaType", message.get("name")));
        }

        if(javaType != null) {
            message.put("x--javaType", javaType);
        }
    }

    private String normalizeSchemaFormat(String schemaFormat) {
        if(schemaFormat == null) {
            return "asyncapi";
        }
        if(schemaFormat.matches("application\\/vnd\\.aai\\.asyncapi(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "asyncapi";
        }
        if(schemaFormat.matches("application\\/vnd\\.oai\\.openapi(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "openapi";
        }
        if(schemaFormat.matches("application\\/schema(\\+json|\\+yaml)*;version=draft-\\d+")) {
            return "jsonSchema";
        }
        if(schemaFormat.matches("application\\/vnd\\.apache\\.avro(\\+json|\\+yaml)*;version=[\\d.]+")) {
            return "avro";
        }
        return null;
    }
}
