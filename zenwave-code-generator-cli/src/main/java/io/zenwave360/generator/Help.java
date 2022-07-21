package io.zenwave360.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.processors.utils.Maps;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateInput;
import io.zenwave360.generator.templating.TemplateOutput;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class Help {

    enum Format {
        SHORT,
        DETAILED,
        JSON,
        MARKDOWN,
        HTML
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private HandlebarsEngine handlebarsEngine = new HandlebarsEngine();

    protected Map<String, Object> buildHelpModel(Configuration configuration, Format format) {
        var model = new LinkedHashMap<String, Object>();
        var options = new LinkedHashMap<String, Object>();
        var undocumentedOptions = new LinkedHashMap<String, Map<String, Object>>();
        model.put("configClassName", configuration.getClass().getName());
        DocumentedOption pluginDocumentation = (DocumentedOption) configuration.getClass().getAnnotation(DocumentedOption.class);
        if(pluginDocumentation != null) {
            model.put("plugin", asModel(configuration.getClass(), pluginDocumentation));
        }
        model.put("config", configuration);
        model.put("options", options);
        model.put("undocumentedOptions", undocumentedOptions);
        for (Class pluginClass: configuration.getChain()) {
            for(Field field: FieldUtils.getAllFields(pluginClass)) {
                DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
                if (documentedOption != null) {
                    options.put(field.getName(), asModel(field.getType(), documentedOption));
                } else {
                    if(isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                        undocumentedOptions.put(field.getName(), Map.of("name", field.getName(),"ownerClass", pluginClass.getName(), "type", field.getType()));
                    }
                }
            }
        }
        for(Field field: FieldUtils.getAllFields(configuration.getClass())) {
            DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
            if (documentedOption != null) {
                options.put(field.getName(), asModel(field.getType(), documentedOption));
            }
        }

        if(format != Format.SHORT) {
            Generator generator = new Generator(configuration);
            GeneratorPlugin generatorPlugin = new GeneratorPlugin() {
                @Override
                public List<TemplateOutput> generate(Map<String, Object> contextModel) {
                    return null;
                }
            };
            Map<Class, Object> pluginList = new LinkedHashMap<>();
            int chainIndex = 0;
            for (Class pluginClass: configuration.getChain()) {
                try {
                    Object plugin = pluginClass.getDeclaredConstructor().newInstance();
                    generator.applyConfiguration(chainIndex++, plugin, configuration);
                    pluginList.put(pluginClass, Utils.asConfigurationMap(plugin));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            model.put("pluginChain", pluginList);
        }

        return model;
    }

    protected Map<String, Object> asModel(Class<?> type, DocumentedOption documentedOption) {
        return Maps.of("description", documentedOption.description(), "type", type, "default", documentedOption.defaultValue());
    }

    public String help(Configuration configuration, Format format) {
        if(format == Format.JSON) {
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
