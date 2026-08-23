package io.zenwave360.sdk.zdl.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.utils.Maps;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.zdl.layouts.DefaultProjectLayout;
import io.zenwave360.sdk.zdl.layouts.ProjectLayout;
import org.apache.commons.lang3.Strings;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Resolves {@code @listener} bindings into generation-ready groups: one group per referenced
 * {@code zdl} api, plus one same-module group per owning service.
 *
 * <p>Each {@code @listener} occurrence in {@code optionsList} is one binding. Cross-module payload
 * types are fully qualified from the referenced module's {@link ProjectLayout} ({@code domainEventsPackage}).
 */
public final class ZDLListenerUtils {

    public static final String MODE_MAPPER = "MAPPER";
    public static final String MODE_CUSTOM = "CUSTOM_REQUIRED";

    private ZDLListenerUtils() {
    }

    public static List<Map<String, Object>> listenerGroups(Map<String, Object> zdlModel) {
        return listenerGroups(zdlModel, null, Map.of());
    }

    public static List<Map<String, Object>> listenerGroups(Map<String, Object> zdlModel, ClassLoader projectClassLoader) {
        return listenerGroups(zdlModel, projectClassLoader, Map.of());
    }

    public static List<Map<String, Object>> listenerGroups(Map<String, Object> zdlModel,
            ClassLoader projectClassLoader, Map<String, Object> generatorOptions) {
        var groups = new LinkedHashMap<String, Map<String, Object>>();
        List<Map<String, Object>> services = JSONPath.get(zdlModel, "$.services[*]", List.of());
        for (Map<String, Object> service : services) {
            List<Map<String, Object>> methods = JSONPath.get(service, "$.methods[*]", List.of());
            for (Map<String, Object> method : methods) {
                for (Map<String, Object> occurrence : listenerOccurrences(method)) {
                    var binding = buildBinding(zdlModel, service, method, occurrence, projectClassLoader,
                            generatorOptions);
                    var group = groups.computeIfAbsent((String) binding.get("groupKey"),
                            key -> newGroup(binding));
                    addBinding(group, binding);
                }
            }
        }
        groups.values().forEach(ZDLListenerUtils::deduplicateListenerMethodNames);
        return new ArrayList<>(groups.values());
    }

    private static List<Map<String, Object>> listenerOccurrences(Map<String, Object> method) {
        var occurrences = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> optionsList = JSONPath.get(method, "$.optionsList[*]", List.of());
        for (Map<String, Object> option : optionsList) {
            if ("listener".equals(option.get("name"))) {
                occurrences.add(asOccurrenceMap(method, option.get("value")));
            }
        }
        if (occurrences.isEmpty() && JSONPath.get(method, "$.options.listener") != null) {
            occurrences.add(asOccurrenceMap(method, JSONPath.get(method, "$.options.listener")));
        }
        return occurrences;
    }

    private static Map<String, Object> asOccurrenceMap(Map<String, Object> method, Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("@listener on method '" + method.get("name")
                + "' requires named parameters, e.g. @listener(zdl: SomeApi, event: SomeEvent)");
    }

    private static Map<String, Object> buildBinding(Map<String, Object> zdlModel, Map<String, Object> service,
            Map<String, Object> method, Map<String, Object> occurrence, ClassLoader projectClassLoader,
            Map<String, Object> generatorOptions) {
        var methodLabel = service.get("name") + "." + method.get("name");
        if (occurrence.get("channel") != null || occurrence.get("topic") != null) {
            throw new IllegalArgumentException("@listener on '" + methodLabel
                    + "' must not restate channel or topic; they are publisher metadata on the event");
        }
        String eventName = trimToNull((String) occurrence.get("event"));
        if (eventName == null) {
            throw new IllegalArgumentException("@listener on '" + methodLabel + "' is missing the 'event' parameter");
        }
        String apiName = trimToNull((String) occurrence.get("zdl"));

        var binding = new LinkedHashMap<String, Object>();
        binding.put("serviceName", service.get("name"));
        binding.put("serviceInstanceName", NamingUtils.asInstanceName((String) service.get("name")));
        binding.put("method", method);
        binding.put("methodName", method.get("name"));
        binding.put("eventName", eventName);

        Map<String, Object> event;
        if (apiName != null) {
            Map<String, Object> api = JSONPath.get(zdlModel, "$.apis['" + apiName + "']");
            if (api == null) {
                throw new IllegalArgumentException("@listener on '" + methodLabel + "' references undeclared zdl api: " + apiName);
            }
            if (!"zdl".equals(api.get("type"))) {
                throw new IllegalArgumentException("@listener on '" + methodLabel + "' references api '" + apiName
                        + "' of type '" + api.get("type") + "' (must be a zdl api; use @asyncapi for asyncapi apis)");
            }
            Map<String, Object> referencedModel = ZDLParser.getReferencedZdlModel(api);
            if (referencedModel == null) {
                throw new IllegalArgumentException("@listener on '" + methodLabel + "' references zdl api '" + apiName
                        + "' whose model could not be loaded from: " + api.get("uri"));
            }
            event = JSONPath.get(referencedModel, "$.events['" + eventName + "']");
            if (event == null) {
                throw new IllegalArgumentException("@listener on '" + methodLabel + "' references event '" + eventName
                        + "' not found in zdl api '" + apiName + "'");
            }
            var eventClassName = eventClassName(event, eventName);
            binding.put("groupKey", "zdl:" + apiName);
            binding.put("apiName", apiName);
            binding.put("apiId", referencedApiId(api, referencedModel, apiName));
            binding.put("groupName", stripApiSuffix(apiName));
            binding.put("eventType", referencedDomainEventsPackage(api, referencedModel, methodLabel,
                    projectClassLoader, generatorOptions)
                    + "." + eventClassName);
            binding.put("listenerMethodName", "on" + eventClassName);
        } else {
            event = JSONPath.get(zdlModel, "$.events['" + eventName + "']");
            if (event == null) {
                throw new IllegalArgumentException("@listener on '" + methodLabel + "' references event '" + eventName
                        + "' not found in this model");
            }
            binding.put("groupKey", "local:" + service.get("name"));
            binding.put("apiId", localApiId(zdlModel, generatorOptions));
            binding.put("groupName", (String) service.get("name"));
            binding.put("eventType", eventClassName(event, eventName));
            binding.put("listenerMethodName", method.get("name"));
        }
        binding.put("eventClassName", eventClassName(event, eventName));
        classifyBinding(zdlModel, method, binding);
        return binding;
    }

    /**
     * A listener event is always mapped into the consuming module's local input object. Methods that do not
     * take exactly one local bean input remain compiling developer-owned customization points.
     */
    private static void classifyBinding(Map<String, Object> zdlModel, Map<String, Object> method,
            Map<String, Object> binding) {
        String parameterType = trimToNull((String) method.get("parameter"));
        boolean simpleShape = method.get("paramId") == null
                && JSONPath.get(method, "$.options.paginated") == null
                && !JSONPath.get(method, "$.parameterIsArray", false);

        if (simpleShape && parameterType != null) {
            Map<String, Object> parameterEntity = JSONPath.get(zdlModel, "$.inputs['" + parameterType + "']");
            boolean beanTarget = parameterEntity != null
                    && parameterEntity.get("fields") instanceof Map<?, ?>
                    && !JSONPath.get(parameterEntity, "$.options.inline", false)
                    && !"enums".equals(parameterEntity.get("type"));
            if (beanTarget) {
                binding.put("mode", MODE_MAPPER);
                binding.put("inputType", parameterType);
                binding.put("mapperMethodName", "as" + NamingUtils.asJavaTypeName(parameterType));
                return;
            }
        }
        binding.put("mode", MODE_CUSTOM);
    }

    private static String referencedDomainEventsPackage(Map<String, Object> api, Map<String, Object> referencedModel,
            String methodLabel, ClassLoader projectClassLoader, Map<String, Object> generatorOptions) {
        Map<String, Object> apiConfig = JSONPath.get(api, "$.config", Map.of());
        String explicitPackage = trimToNull((String) apiConfig.get("domainEventsPackage"));
        if (explicitPackage != null) {
            return explicitPackage;
        }
        Map<String, Object> referencedConfig = JSONPath.get(referencedModel, "$.config", Map.of());

        // Resolve the referenced module exactly like a generated module: project/CLI options establish
        // the project-wide base, then the referenced ZDL supplies its module-specific configuration,
        // and the api declaration can override that configuration for this reference.
        Map<String, Object> options = Maps.copy(generatorOptions != null ? generatorOptions : Map.of());
        Map<String, Object> referencedOptions = Maps.copy(referencedConfig);
        Map<String, Object> apiOptions = Maps.copy(apiConfig);
        apiOptions.remove("uri");

        String layoutClassName = layoutClassName(apiOptions, referencedOptions);
        removeLayoutClassName(referencedOptions);
        removeLayoutClassName(apiOptions);
        Maps.deepMerge(options, referencedOptions);
        Maps.deepMerge(options, apiOptions);

        return referencedLayout(layoutClassName, projectClassLoader, methodLabel, (String) api.get("name"))
                .processedLayout(options).domainEventsPackage;
    }

    private static String layoutClassName(Map<String, Object> apiOptions,
            Map<String, Object> referencedOptions) {
        if (apiOptions.get("layout") instanceof String value) {
            return trimToNull(value);
        }
        if (referencedOptions.get("layout") instanceof String value) {
            return trimToNull(value);
        }
        return null;
    }

    private static void removeLayoutClassName(Map<String, Object> options) {
        if (options.get("layout") instanceof String) {
            options.remove("layout");
        }
    }

    private static ProjectLayout referencedLayout(String layoutClassName, ClassLoader projectClassLoader,
            String methodLabel, String apiName) {
        if (layoutClassName == null) {
            return new DefaultProjectLayout();
        }
        var candidates = List.of(layoutClassName, ProjectLayout.class.getPackageName() + "." + layoutClassName);
        var classLoaders = new LinkedHashSet<ClassLoader>();
        if (projectClassLoader != null) {
            classLoaders.add(projectClassLoader);
        }
        if (Thread.currentThread().getContextClassLoader() != null) {
            classLoaders.add(Thread.currentThread().getContextClassLoader());
        }
        classLoaders.add(ZDLListenerUtils.class.getClassLoader());
        Exception lastFailure = null;
        for (String candidate : candidates) {
            for (ClassLoader classLoader : classLoaders) {
                try {
                    Class<?> layoutType = Class.forName(candidate, true, classLoader);
                    if (!ProjectLayout.class.isAssignableFrom(layoutType)) {
                        throw new IllegalArgumentException(candidate + " does not extend " + ProjectLayout.class.getName());
                    }
                    return (ProjectLayout) layoutType.getConstructor().newInstance();
                } catch (Exception e) {
                    lastFailure = e;
                }
            }
        }
        throw new IllegalArgumentException("@listener on '" + methodLabel + "': unable to load layout '"
                + layoutClassName + "' for referenced zdl api '" + apiName
                + "'; configure domainEventsPackage explicitly on that api", lastFailure);
    }

    private static String eventClassName(Map<String, Object> event, String eventName) {
        var className = trimToNull((String) event.get("className"));
        return className != null ? className : eventName;
    }

    private static String stripApiSuffix(String apiName) {
        var stripped = Strings.CS.removeEnd(Strings.CS.removeEnd(apiName, "Zdl"), "Api");
        return stripped.isEmpty() ? apiName : stripped;
    }

    private static String referencedApiId(Map<String, Object> api, Map<String, Object> referencedModel,
            String apiName) {
        String explicit = trimToNull((String) JSONPath.get(api, "$.config.apiId"));
        if (explicit != null) {
            return explicit;
        }
        explicit = trimToNull((String) JSONPath.get(referencedModel, "$.config.apiId"));
        if (explicit != null) {
            return explicit;
        }
        String moduleBasePackage = trimToNull((String) JSONPath.get(referencedModel, "$.config.moduleBasePackage"));
        if (moduleBasePackage == null) {
            moduleBasePackage = trimToNull((String) JSONPath.get(referencedModel, "$.config.basePackage"));
        }
        if (moduleBasePackage != null) {
            return moduleBasePackage.substring(moduleBasePackage.lastIndexOf('.') + 1);
        }
        String name = Strings.CS.removeEnd(Strings.CS.removeEnd(Strings.CS.removeEnd(apiName, "Module"), "Zdl"), "Api");
        return NamingUtils.asInstanceName(name.isEmpty() ? apiName : name);
    }

    private static String localApiId(Map<String, Object> zdlModel, Map<String, Object> generatorOptions) {
        String explicit = trimToNull((String) JSONPath.get(zdlModel, "$.config.apiId"));
        if (explicit != null) {
            return explicit;
        }
        String moduleBasePackage = trimToNull((String) JSONPath.get(zdlModel, "$.config.moduleBasePackage"));
        if (moduleBasePackage == null) {
            moduleBasePackage = trimToNull((String) JSONPath.get(zdlModel, "$.config.basePackage"));
        }
        if (moduleBasePackage == null) {
            moduleBasePackage = trimToNull((String) JSONPath.get(generatorOptions, "$.moduleBasePackage"));
        }
        String projectName = trimToNull((String) JSONPath.get(generatorOptions, "$.projectName"));
        if (moduleBasePackage == null && projectName != null) {
            return NamingUtils.asInstanceName(NamingUtils.camelCase(projectName));
        }
        if (moduleBasePackage == null) {
            moduleBasePackage = trimToNull((String) JSONPath.get(generatorOptions, "$.basePackage"));
        }
        return moduleBasePackage != null
                ? moduleBasePackage.substring(moduleBasePackage.lastIndexOf('.') + 1)
                : null;
    }

    private static Map<String, Object> newGroup(Map<String, Object> binding) {
        var group = new LinkedHashMap<String, Object>();
        var groupKey = (String) binding.get("groupKey");
        group.put("type", groupKey.startsWith("zdl:") ? "zdl" : "local");
        group.put("name", binding.get("apiName") != null ? binding.get("apiName") : binding.get("serviceName"));
        group.put("apiId", binding.get("apiId"));
        group.put("groupName", binding.get("groupName"));
        group.put("className", binding.get("groupName") + "EventsListener");
        group.put("services", new ArrayList<Map<String, Object>>());
        group.put("bindings", new ArrayList<Map<String, Object>>());
        group.put("mapperBindings", new ArrayList<Map<String, Object>>());
        return group;
    }

    private static void addBinding(Map<String, Object> group, Map<String, Object> binding) {
        var services = (List<Map<String, Object>>) group.get("services");
        if (services.stream().noneMatch(service -> Objects.equals(service.get("name"), binding.get("serviceName")))) {
            services.add(Map.of(
                    "name", binding.get("serviceName"),
                    "instanceName", binding.get("serviceInstanceName")));
        }
        ((List<Map<String, Object>>) group.get("bindings")).add(binding);
        if (MODE_MAPPER.equals(binding.get("mode"))) {
            var mapperBindings = (List<Map<String, Object>>) group.get("mapperBindings");
            boolean duplicate = mapperBindings.stream().anyMatch(other ->
                    Objects.equals(other.get("mapperMethodName"), binding.get("mapperMethodName"))
                            && Objects.equals(other.get("eventType"), binding.get("eventType"))
                            && Objects.equals(other.get("inputType"), binding.get("inputType")));
            if (!duplicate) {
                mapperBindings.add(binding);
            }
        }
    }

    private static void deduplicateListenerMethodNames(Map<String, Object> group) {
        var bindings = (List<Map<String, Object>>) group.get("bindings");
        var usedNames = new LinkedHashSet<String>();
        for (Map<String, Object> binding : bindings) {
            var name = (String) binding.get("listenerMethodName");
            if (!usedNames.add(name)) {
                name = name + capitalize((String) binding.get("methodName"));
                binding.put("listenerMethodName", name);
                if (!usedNames.add(name)) {
                    throw new IllegalArgumentException("Duplicate listener method name '" + name
                            + "' in generated listener " + group.get("className"));
                }
            }
        }
    }
}
