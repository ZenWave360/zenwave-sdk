package io.zenwave360.sdk.plugins.templates;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.plugins.AsyncAPIGenerator;
import io.zenwave360.sdk.options.ProgrammingStyle;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.utils.JSONPath;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AsyncAPIHandlebarsHelpers {

    private final AsyncAPIGenerator generator;

    public AsyncAPIHandlebarsHelpers(AsyncAPIGenerator generator) {
        this.generator = generator;
    }

    public List<String> channelNames(List<Map<String, Object>> operations, Options options) {
        var channelNames = JSONPath.get(operations, "$[*].x--channel", List.<String>of());
        return new ArrayList<>(new HashSet<>(channelNames));
    }

    public Object producerInterfaceName(String serviceName, Options options) {
        AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
        return AsyncAPIGenerator.getApiClassName(serviceName, operationRoleType);
    }

    public Object producerClassName(String serviceName, Options options) {
        AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
        return "Default" + AsyncAPIGenerator.getApiClassName(serviceName, operationRoleType);
    }

    public Object producerInMemoryName(String serviceName, Options options) {
        AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
        return "InMemory" + AsyncAPIGenerator.getApiClassName(serviceName, operationRoleType);
    }

    public Object consumerName(Object context, Options options) {
        return String.format("%s%s%s", generator.consumerPrefix, context, generator.consumerSuffix);
    }

    public Object messageType(Object operation, Options options) {
        List<String> messageTypes = JSONPath.get(operation, "$.x--messages[*].x--javaType");
        List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + generator.envelopeJavaTypeExtensionName);
        String operationEnvelop = JSONPath.get(operation, "$." + generator.envelopeJavaTypeExtensionName);
        if(operationEnvelop != null) {
            envelopTypes.add(operationEnvelop);
        }
        if(generator.useEnterpriseEnvelope && !envelopTypes.isEmpty()) {
            return envelopTypes.size() == 1 ? envelopTypes.get(0) : "Object";
        }
        return messageTypes.size() == 1 ? messageTypes.get(0) : "Object";
    }

    public Object hasEnterpriseEnvelope(Object operation, Options options) {
        List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + generator.envelopeJavaTypeExtensionName);
        String operationEnvelop = JSONPath.get(operation, "$." + generator.envelopeJavaTypeExtensionName);
        if(operationEnvelop != null) {
            envelopTypes.add(operationEnvelop);
        }
        return generator.useEnterpriseEnvelope && !envelopTypes.isEmpty();
    }

    public Object consumerServiceInterfaceName(Object context, Options options) {
        return String.format("%s%s%s", generator.consumerServicePrefix, context, generator.consumerServiceSuffix);
    }

    public Object consumerServiceName(Object context, Options options) {
        return String.format("%s%s", context, generator.consumerServiceSuffix);
    }

    public Object testDoubleName(Object context, Options options) {
        return String.format("%s%s%s", context, generator.consumerServiceSuffix, "TestDouble");
    }

    public Object methodSuffix(Object context, Options options) {
        boolean doExposeMessage = "true".equals(String.valueOf(options.hash.get("exposeMessage")));
        boolean isProducer = "true".equals(String.valueOf(options.hash.get("producer")));
        if (doExposeMessage || generator.exposeMessage || generator.style == ProgrammingStyle.reactive) {
            int messagesCount = JSONPath.get(options.param(0), "$.x--messages.length()", 0);
            if (messagesCount > 1) {
                String messageJavaType = JSONPath.get(context, "$.x--javaTypeSimpleName");
                return String.format("%s%s", generator.methodAndMessageSeparator, messageJavaType);
            }
        }
        return null;
    }

    public Object hasRuntimeHeaders(Object context, Options options) {
        var path = context instanceof List? "$[*].x--messages[*].headers.properties[*]" : "$.headers.properties[*]";
        return !JSONPath.get(context, path + generator.runtimeHeadersProperty, Collections.emptyList()).isEmpty();
    }

    public Object runtimeHeadersMap(Object message, Options options) {
        List<String> runtimeHeaders = new ArrayList<>();
        Map<String, Map> headers = JSONPath.get(message, "$.headers.properties");
        for (String header : headers.keySet()) {
            String location = JSONPath.get(headers.get(header), "$." + generator.runtimeHeadersProperty);
            if(location != null) {
                runtimeHeaders.add("\"" + header + "\"");
                runtimeHeaders.add("\"" + StringEscapeUtils.escapeJava(location) + "\"");
            }
        }
        return runtimeHeaders.stream().collect(Collectors.joining(", "));
    }

    public Object propertyType(Object context, Options options) {
        Map property = (Map) context;
        String type = (String) property.get("type");
        String format = (String) property.get("format");
        if ("date".equals(format)) {
            return "LocalDate";
        }
        if ("date-time".equals(format)) {
            return "Instant";
        }
        if ("integer".equals(type) && "int32".equals(format)) {
            return "Integer";
        }
        if ("integer".equals(type) && "int64".equals(format)) {
            return "Long";
        }
        if ("number".equals(type)) {
            return "BigDecimal";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if("string".equals(type)) {
            return "String";
        }
        return "Object";
    }

    public void registerHelpers(HandlebarsEngine handlebarsEngine) {
        handlebarsEngine.getHandlebars().registerHelper("producerInterfaceName", (serviceName, options) -> {
            AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
            return AsyncAPIGenerator.getApiClassName((String) serviceName, operationRoleType);
        });

        handlebarsEngine.getHandlebars().registerHelper("producerClassName", (serviceName, options) -> {
            AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
            return "Default" + AsyncAPIGenerator.getApiClassName((String) serviceName, operationRoleType);
        });

        handlebarsEngine.getHandlebars().registerHelper("producerInMemoryName", (serviceName, options) -> {
            AsyncAPIGenerator.OperationRoleType operationRoleType = options.param(0);
            return "InMemory" + AsyncAPIGenerator.getApiClassName((String) serviceName, operationRoleType);
        });

        handlebarsEngine.getHandlebars().registerHelper("consumerName", (context, options) -> {
            return String.format("%s%s%s", generator.consumerPrefix, context, generator.consumerSuffix);
        });

        handlebarsEngine.getHandlebars().registerHelper("messageType", (operation, options) -> {
            List<String> messageTypes = JSONPath.get(operation, "$.x--messages[*].x--javaType");
            List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + generator.envelopeJavaTypeExtensionName);
            String operationEnvelop = JSONPath.get(operation, "$." + generator.envelopeJavaTypeExtensionName);
            if(operationEnvelop != null) {
                envelopTypes.add(operationEnvelop);
            }
            if(generator.useEnterpriseEnvelope && !envelopTypes.isEmpty()) {
                return envelopTypes.size() == 1 ? envelopTypes.get(0) : "Object";
            }
            return messageTypes.size() == 1 ? messageTypes.get(0) : "Object";
        });

        handlebarsEngine.getHandlebars().registerHelper("hasEnterpriseEnvelope", (operation, options) -> {
            List<String> envelopTypes = JSONPath.get(operation, "$.x--messages[*]." + generator.envelopeJavaTypeExtensionName);
            String operationEnvelop = JSONPath.get(operation, "$." + generator.envelopeJavaTypeExtensionName);
            if(operationEnvelop != null) {
                envelopTypes.add(operationEnvelop);
            }
            return generator.useEnterpriseEnvelope && !envelopTypes.isEmpty();
        });

        handlebarsEngine.getHandlebars().registerHelper("consumerServiceInterfaceName", (context, options) -> {
            return String.format("%s%s%s", generator.consumerServicePrefix, context, generator.consumerServiceSuffix);
        });

        handlebarsEngine.getHandlebars().registerHelper("consumerServiceName", (context, options) -> {
            return String.format("%s%s", context, generator.consumerServiceSuffix);
        });

        handlebarsEngine.getHandlebars().registerHelper("testDoubleName", (context, options) -> {
            return String.format("%s%s%s", context, generator.consumerServiceSuffix, "TestDouble");
        });

        handlebarsEngine.getHandlebars().registerHelper("methodSuffix", (context, options) -> {
            boolean doExposeMessage = "true".equals(String.valueOf(options.hash.get("exposeMessage")));
            boolean isProducer = "true".equals(String.valueOf(options.hash.get("producer")));
            if (doExposeMessage || generator.exposeMessage || generator.style == ProgrammingStyle.reactive) {
                int messagesCount = JSONPath.get(options.param(0), "$.x--messages.length()", 0);
                if (messagesCount > 1) {
                    String messageJavaType = JSONPath.get(context, "$.x--javaTypeSimpleName");
                    return String.format("%s%s", generator.methodAndMessageSeparator, messageJavaType);
                }
            }
            return null;
        });

        handlebarsEngine.getHandlebars().registerHelper("hasRuntimeHeaders", (context, options) -> {
            var path = context instanceof List? "$[*].x--messages[*].headers.properties[*]" : "$.headers.properties[*]";
            return !JSONPath.get(context, path + generator.runtimeHeadersProperty, Collections.emptyList()).isEmpty();
        });

        handlebarsEngine.getHandlebars().registerHelper("runtimeHeadersMap", (message, options) -> {
            List<String> runtimeHeaders = new ArrayList<>();
            Map<String, Map> headers = JSONPath.get(message, "$.headers.properties");
            for (String header : headers.keySet()) {
                String location = JSONPath.get(headers.get(header), "$." + generator.runtimeHeadersProperty);
                if(location != null) {
                    runtimeHeaders.add("\"" + header + "\"");
                    runtimeHeaders.add("\"" + StringEscapeUtils.escapeJava(location) + "\"");
                }
            }
            return runtimeHeaders.stream().collect(Collectors.joining(", "));
        });

        handlebarsEngine.getHandlebars().registerHelper("propertyType", (context, options) -> {
            Map property = (Map) context;
            String type = (String) property.get("type");
            String format = (String) property.get("format");
            if ("date".equals(format)) {
                return "LocalDate";
            }
            if ("date-time".equals(format)) {
                return "Instant";
            }
            if ("integer".equals(type) && "int32".equals(format)) {
                return "Integer";
            }
            if ("integer".equals(type) && "int64".equals(format)) {
                return "Long";
            }
            if ("number".equals(type)) {
                return "BigDecimal";
            }
            if ("boolean".equals(type)) {
                return "Boolean";
            }
            if("string".equals(type)) {
                return "String";
            }
            return "Object";
        });
    }
}
