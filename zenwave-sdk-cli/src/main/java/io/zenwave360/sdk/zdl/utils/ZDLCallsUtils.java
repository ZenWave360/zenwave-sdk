package io.zenwave360.sdk.zdl.utils;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.NamingUtils;

/** Resolves repeatable {@code @calls} annotations into generation-ready method metadata. */
public final class ZDLCallsUtils {

    private ZDLCallsUtils() {
    }

    /** Resolves and attaches an ordered {@code calls} list to every service method. */
    public static void resolveCalls(Map<String, Object> zdlModel) {
        List<Map<String, Object>> methods = JSONPath.get(zdlModel, "$.services[*].methods[*]", List.of());
        for (Map<String, Object> method : methods) {
            List<Map<String, Object>> calls = methodCalls(zdlModel, method);
            if (calls.isEmpty()) {
                method.remove("calls");
            } else {
                method.put("calls", calls);
            }
        }
    }

    /** Returns one resolved entry per {@code @calls} occurrence, preserving declaration order. */
    public static List<Map<String, Object>> methodCalls(Map<String, Object> zdlModel,
            Map<String, Object> method) {
        var calls = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> optionsList = JSONPath.get(method, "$.optionsList[*]", List.of());
        for (Map<String, Object> option : optionsList) {
            if ("calls".equals(option.get("name"))) {
                calls.add(resolveCall(zdlModel, method, asOccurrenceMap(method, option.get("value"))));
            }
        }
        return calls;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asOccurrenceMap(Map<String, Object> method, Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("@calls on method '" + methodLabel(method)
                + "' requires named parameters, e.g. @calls(zdl: InventoryZdl, command: InventoryService.reserveStock)");
    }

    private static Map<String, Object> resolveCall(Map<String, Object> zdlModel, Map<String, Object> method,
            Map<String, Object> occurrence) {
        String command = trimToNull((String) occurrence.get("command"));
        if (command == null) {
            throw new IllegalArgumentException("@calls on method '" + methodLabel(method)
                    + "' is missing the 'command' parameter");
        }

        String apiName = trimToNull((String) occurrence.get("zdl"));
        Map<String, Object> targetModel = zdlModel;
        if (apiName != null) {
            Map<String, Object> apis = JSONPath.get(zdlModel, "$.apis", Map.of());
            Map<String, Object> api = apis == null ? null : (Map<String, Object>) apis.get(apiName);
            if (api == null) {
                throw new IllegalArgumentException("@calls on method '" + methodLabel(method)
                        + "' references undeclared zdl api: " + apiName);
            }
            if (!"zdl".equals(api.get("type"))) {
                throw new IllegalArgumentException("@calls on method '" + methodLabel(method) + "' references api '"
                        + apiName + "' of type '" + api.get("type") + "' (must be a zdl api)");
            }
            targetModel = ZDLParser.getReferencedZdlModel(api);
            if (targetModel == null) {
                throw new IllegalArgumentException("@calls on method '" + methodLabel(method)
                        + "' references zdl api '" + apiName + "' whose model could not be loaded from: "
                        + api.get("uri")
                        + (api.get(ZDLParser.REFERENCED_API_ERROR_PROPERTY) != null ? " (" + api.get(ZDLParser.REFERENCED_API_ERROR_PROPERTY) + ")" : ""));
            }
        }

        ResolvedCommand resolved = findCommand(targetModel, command, methodLabel(method), apiName);
        Map<String, Object> targetMethod = resolved.method();
        var call = new LinkedHashMap<String, Object>();
        call.put("apiName", apiName);
        call.put("serviceName", resolved.serviceName());
        call.put("serviceInstanceName", NamingUtils.asInstanceName(resolved.serviceName()));
        call.put("commandName", targetMethod.get("name"));
        call.put("parameterType", targetMethod.get("parameter"));
        call.put("returnType", targetMethod.get("returnType"));
        call.put("withEvents", flattenEvents(targetMethod.get("withEvents")));
        return call;
    }

    private static ResolvedCommand findCommand(Map<String, Object> targetModel, String command, String methodLabel,
            String apiName) {
        Map<String, Map<String, Object>> services = JSONPath.get(targetModel, "$.services", Map.of());
        int separator = command.lastIndexOf('.');
        if (separator >= 0) {
            String serviceName = trimToNull(command.substring(0, separator));
            String commandName = trimToNull(command.substring(separator + 1));
            if (serviceName == null || commandName == null) {
                throw unknownCommand(methodLabel, command, apiName);
            }
            Map<String, Object> service = services.get(serviceName);
            Map<String, Object> targetMethod = service == null ? null
                    : JSONPath.get(service, "$.methods['" + commandName + "']");
            if (targetMethod == null) {
                throw unknownCommand(methodLabel, command, apiName);
            }
            return new ResolvedCommand(serviceName, targetMethod);
        }

        var matches = new ArrayList<ResolvedCommand>();
        for (Map.Entry<String, Map<String, Object>> serviceEntry : services.entrySet()) {
            Map<String, Object> targetMethod = JSONPath.get(serviceEntry.getValue(), "$.methods['" + command + "']");
            if (targetMethod != null) {
                matches.add(new ResolvedCommand(serviceEntry.getKey(), targetMethod));
            }
        }
        if (matches.isEmpty()) {
            throw unknownCommand(methodLabel, command, apiName);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("@calls on method '" + methodLabel + "' references ambiguous command '"
                    + command + "'" + modelSuffix(apiName) + "; qualify it as ServiceName." + command);
        }
        return matches.get(0);
    }

    private static IllegalArgumentException unknownCommand(String methodLabel, String command, String apiName) {
        return new IllegalArgumentException("@calls on method '" + methodLabel + "' references unknown command '"
                + command + "'" + modelSuffix(apiName));
    }

    private static String modelSuffix(String apiName) {
        return apiName == null ? " in this model" : " in zdl api '" + apiName + "'";
    }

    private static String methodLabel(Map<String, Object> method) {
        String serviceName = trimToNull((String) method.get("serviceName"));
        return serviceName == null ? String.valueOf(method.get("name")) : serviceName + "." + method.get("name");
    }

    private static List<String> flattenEvents(Object value) {
        var events = new ArrayList<String>();
        addEvents(value, events);
        return events;
    }

    private static void addEvents(Object value, List<String> events) {
        if (value instanceof List<?> list) {
            list.forEach(item -> addEvents(item, events));
        } else if (value != null) {
            events.add(String.valueOf(value));
        }
    }

    private record ResolvedCommand(String serviceName, Map<String, Object> method) {
    }
}
