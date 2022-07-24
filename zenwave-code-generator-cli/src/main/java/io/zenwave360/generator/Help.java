package io.zenwave360.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.processors.utils.Maps;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateInput;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.zenwave360.generator.Main.applyConfiguration;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class Help {

    enum Format {
        help, detailed, json, markdown, html
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected Map<String, Object> buildHelpModel(Configuration configuration, Format format) {
        var model = new LinkedHashMap<String, Object>();
        var options = new LinkedHashMap<String, Object>();
        var undocumentedOptions = new LinkedHashMap<String, Map<String, Object>>();
        var pluginList = new LinkedHashMap<Class, Object>();
        model.put("configClassName", configuration.getClass().getName());
        DocumentedPlugin pluginDocumentation = (DocumentedPlugin) configuration.getClass().getAnnotation(DocumentedPlugin.class);
        if(pluginDocumentation != null) {
            model.put("plugin", Maps.of("title", pluginDocumentation.value(),"description", pluginDocumentation.description()));
        }
        model.put("config", configuration);
        model.put("options", options);
        model.put("undocumentedOptions", undocumentedOptions);
        model.put("pluginChain", pluginList);

        // adds options from config class
        for(Field field: FieldUtils.getAllFields(configuration.getClass())) {
            DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
            if (documentedOption != null) {
                options.put(field.getName(), asModel(configuration, field, documentedOption));
            }
        }

        // adds options from processors chain
        int chainIndex = 0;
        for (Class pluginClass: configuration.getChain()) {
            Object plugin;
            try {
                plugin = pluginClass.getDeclaredConstructor().newInstance();
                applyConfiguration(chainIndex++, plugin, configuration);
                pluginList.put(pluginClass, Utils.asConfigurationMap(plugin));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for(Field field: FieldUtils.getAllFields(pluginClass)) {
                DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
                if (documentedOption != null) {
                    options.put(field.getName(), asModel(plugin, field, documentedOption));
                } else {
                    if(isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                        undocumentedOptions.put(field.getName(), Map.of("name", field.getName(),"ownerClass", pluginClass.getName(), "type", field.getType()));
                    }
                }
            }
        }

        return model;
    }

    protected Map<String, Object> asModel(Object plugin, Field field, DocumentedOption documentedOption) {
        Class type = field.getType();
        List values = new ArrayList();
        if(type.isEnum()) {
            values.addAll(Arrays.stream(type.getEnumConstants()).map(v -> v.toString()).collect(Collectors.toList()));
        }
        Object defaultValue = null;
        try {
            defaultValue = FieldUtils.readField(field, plugin, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        defaultValue = defaultValue == null? documentedOption.defaultValue() : defaultValue;
        if(defaultValue.getClass().isArray()) {
            defaultValue = Arrays.asList((Object[]) defaultValue);
        }
        return Maps.of("description", documentedOption.description(), "type", type.getSimpleName(), "defaultValue", defaultValue, "values", values);
    }

    public String help(Configuration configuration, Format format) {
        if(format == Format.json) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildHelpModel(configuration, format));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        String template = "io/zenwave360/generator/help/" + format.toString();
        return handlebarsEngine.processTemplate(buildHelpModel(configuration, format), new TemplateInput().withTemplateLocation(template).withTargetFile("")).get(0).getContent();
    }

}
