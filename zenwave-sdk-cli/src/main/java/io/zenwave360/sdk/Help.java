package io.zenwave360.sdk;

import static io.zenwave360.sdk.MainGenerator.applyConfiguration;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.sdk.utils.NamingUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.utils.Maps;

public class Help {

    enum Format {
        list, help, detailed, json, markdown, html;
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected Map<String, Object> buildHelpModel(Plugin configuration) {
        var model = new LinkedHashMap<String, Object>();
        var options = new LinkedHashMap<String, Object>();
        var undocumentedOptions = new LinkedHashMap<String, Map<String, Object>>();
        var pluginList = new LinkedHashMap<Class, Object>();
        model.put("configClassName", configuration.getClass().getName());
        model.put("configClassSimpleName", configuration.getClass().getSimpleName());
        model.put("configHumanReadableName", NamingUtils.humanReadable(configuration.getClass().getSimpleName()));
        DocumentedPlugin pluginDocumentation = (DocumentedPlugin) configuration.getClass().getAnnotation(DocumentedPlugin.class);
        if (pluginDocumentation != null) {
            var title = ObjectUtils.firstNonNull(pluginDocumentation.title(), NamingUtils.humanReadable(configuration.getClass().getSimpleName()));
            model.put("plugin", Maps.of("title", title, "summary", pluginDocumentation.summary(), "description", pluginDocumentation.description()));
        }
        model.put("version", getJarVersion(configuration.getClass()));
        model.put("config", configuration);
        model.put("options", options);
        model.put("undocumentedOptions", undocumentedOptions);
        model.put("pluginChain", pluginList);

        List<String> hiddenOptions = List.of(pluginDocumentation.hiddenOptions());
        List<Field> fields = new ArrayList<>();
        Map<Field, Object> fieldOwners = new HashMap<>();
        List.of(FieldUtils.getAllFields(configuration.getClass())).forEach(f -> {
            fields.add(f);
            fieldOwners.put(f, configuration);
        });

        // adds options from processors chain
        int chainIndex = 0;
        for (Class pluginClass : configuration.getChain()) {
            Object plugin;
            try {
                plugin = newInstance(pluginClass);
                applyConfiguration(chainIndex++, plugin, configuration);
                pluginList.put(pluginClass, Generator.asConfigurationMap(plugin));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List.of(FieldUtils.getAllFields(pluginClass)).forEach(f -> {
                fields.add(f);
                fieldOwners.put(f, plugin);
            });
        }

        sortFields(fields, pluginDocumentation.mainOptions());
        for (Field field : fields) {
            DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
            if (documentedOption != null && !hiddenOptions.contains(field.getName())) {
                Object plugin = fieldOwners.get(field);
                options.put(field.getName(), asModel(plugin, field, documentedOption));
            } else if (isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                undocumentedOptions.put(field.getName(), Map.of("name", field.getName(), "ownerClass", field.getDeclaringClass().getName(), "type", field.getType()));
            }
        }

        return model;
    }

    private Object newInstance(Class pluginClass) {
        try {
            return pluginClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sortFields(List<Field> fields, String[] mainOptions) {
        if (mainOptions == null || mainOptions.length == 0) {
            return;
        }

        // Create a map of field name to priority (index in mainOptions array)
        Map<String, Integer> priorityMap = new HashMap<>();
        for (int i = 0; i < mainOptions.length; i++) {
            priorityMap.put(mainOptions[i], i);
        }

        // Sort the list in place
        Collections.sort(fields, (f1, f2) -> {
            Integer p1 = priorityMap.get(f1.getName());
            Integer p2 = priorityMap.get(f2.getName());

            if (p1 != null && p2 != null) {
                return p1.compareTo(p2);
            } else if (p1 != null) {
                return -1; // f1 is a main option, should come first
            } else if (p2 != null) {
                return 1;  // f2 is a main option, should come first
            }
            // For non-main options, maintain original order by comparing their positions in the original list
            return Integer.compare(fields.indexOf(f1), fields.indexOf(f2));
        });
    }

    protected Map<String, Object> discoverAvailablePlugins() {
        var plugins = new ArrayList<>();
        var allConfigClasses = new Reflections("io", "com", "org").getSubTypesOf(Plugin.class);
        for (Class<? extends Plugin> pluginClass : allConfigClasses) {
            try {
                plugins.add(buildHelpModel(Plugin.of(pluginClass.getName())));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Maps.of("plugins", plugins);
    }

    protected Map<String, Object> asModel(Object plugin, Field field, DocumentedOption documentedOption) {
        Class type = field.getType();
        List values = new ArrayList();
        if (type.isEnum()) {
            values.addAll(Arrays.stream(type.getEnumConstants()).map(v -> v.toString()).collect(Collectors.toList()));
        } else {
            values.addAll(Arrays.asList(documentedOption.values()));
        }
        Object defaultValue = null;
        try {
            defaultValue = FieldUtils.readField(field, plugin, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        defaultValue = defaultValue == null ? documentedOption.defaultValue() : defaultValue;
        if (defaultValue.getClass().isArray()) {
            defaultValue = Arrays.asList((Object[]) defaultValue);
        }
        return Map.of("description", documentedOption.description(), "type", type.getSimpleName(), "defaultValue", defaultValue, "values", values, "docLink", documentedOption.docLink());
    }

    public String help(Plugin plugin, Format format) {
        format = format == null ? Format.help : format;
        var model = plugin == null ? discoverAvailablePlugins() : buildHelpModel(plugin);
        model.put("version", getClass().getPackage().getImplementationVersion());
        if (format == Format.json) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        String template = "io/zenwave360/sdk/help/" + format.toString();
        return handlebarsEngine.processTemplate(model, new TemplateInput().withTemplateLocation(template).withTargetFile("")).getContent();
    }

    protected String getJarVersion(Class<?> clazz) {
        try {
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            if (classPath.startsWith("jar")) {
                // Get jar file path: "jar:file:/path/to/my-jar-1.0.0.jar!/..."
                String jarPath = classPath.substring(0, classPath.lastIndexOf("!")).substring("jar:file:".length());
                // Extract version from jar name assuming format: name-version.jar
                String jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1);
                String version = jarName.replaceAll(".*-(\\d+\\.\\d+\\.\\d+.*?)\\.jar", "$1");
                return version;
            }
            return clazz.getPackage().getImplementationVersion();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

}
