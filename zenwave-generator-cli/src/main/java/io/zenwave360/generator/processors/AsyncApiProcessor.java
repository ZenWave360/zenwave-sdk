package io.zenwave360.generator.processors;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AsyncApiProcessor implements Processor {

    public String targetProperty = "api";

    public AsyncApiProcessor withTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    @Override
    public Map<String, ?> process(Map<String, ?> contextModel) {
        Map<String, ?> apiModel = targetProperty != null? (Map<String, ?>) contextModel.get(targetProperty) : contextModel;

        List<Map<String, Object>> traitsParents = JsonPath.read(apiModel, "$..[?(@.traits)]");
        for (Map<String, Object> traitParent : traitsParents) {
            List<Map<String, Object>> traitsList = (List) traitParent.get("traits");
            for (Map<String, Object> traits : traitsList) {
                traitParent.putAll(traits);
            }
        }

        Map<String, Object> channels = JsonPath.read(apiModel, "$.channels");
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

        Map<String, Map> componentsMessages = JsonPath.read(apiModel, "$.components.messages");
        for(Map.Entry<String, Map> message: componentsMessages.entrySet()) {
            if(!message.getValue().containsKey("name")) {
                message.getValue().put("name", message.getKey());
            }
        }

        List<Map<String, Object>> messages = JsonPath.read(apiModel, "$.channels..x--messages[*]");
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

    private void addNormalizedTagName(Map<String, Object> operation) {
        if (operation != null) {
            String normalizedTagName = null;
            List tags = (List) operation.get("tags");
            if(tags != null) {
                String tag = (String) (tags.get(0) instanceof Map? (String) ((Map) tags.get(0)).get("name") : tags.get(0));
                normalizedTagName = normalizeTagName(tag);
            }
            operation.put("x--normalizedTagName", normalizedTagName);
        }
    }

    public String normalizeTagName(String tagName) {
        if(tagName == null) {
            return null;
        }
        String[] tokens = RegExUtils.replaceAll(tagName, "[\\s-.]", " ").split(" ");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = StringUtils.capitalize(tokens[i]);
        }
        return RegExUtils.removePattern(StringUtils.join(tokens), "^(\\d+)");
    }

    private void addOperationIdVariants(Map<String, Object> operation) {
        if (operation != null) {
            operation.put("x--operationIdCamelCase", StringUtils.capitalize((String) operation.get("operationId")));
            operation.put("x--operationIdKebabCase", StringUtils.capitalize((String) operation.get("operationId")));
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
