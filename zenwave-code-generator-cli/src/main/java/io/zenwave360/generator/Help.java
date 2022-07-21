package io.zenwave360.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateInput;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.swing.text.html.HTML;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
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

    protected Map<String, Object> buildHelpModel(Configuration configuration) {
        var model = new HashMap<String, Object>();
        var options = new HashMap<String, Object>();
        var undocumentedOptions = new ArrayList<Map<String, Object>>();
        model.put("options", options);
        model.put("undocumentedOptions", undocumentedOptions);
        model.put("config", configuration);
        model.put("configClassName", configuration.getClass().getName());
        DocumentedOption pluginDocumentation = (DocumentedOption) configuration.getClass().getAnnotation(DocumentedOption.class);
        if(pluginDocumentation != null) {
            model.put("plugin", asModel(pluginDocumentation));
        }
        for (Class pluginClass: configuration.getChain()) {
            for(Field field: FieldUtils.getAllFields(pluginClass)) {
                DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
                if (documentedOption != null) {
                    options.put(field.getName(), asModel(documentedOption));
                } else {
                    if(isPublic(field.getModifiers()) && !isStatic(field.getModifiers())) {
                        undocumentedOptions.add(Map.of("name", field.getName(),"ownerClass", pluginClass.getName(), "type", field.getType()));
                    }
                }
            }
        }
        for(Field field: FieldUtils.getAllFields(configuration.getClass())) {
            DocumentedOption documentedOption = field.getAnnotation(DocumentedOption.class);
            if (documentedOption != null) {
                options.put(field.getName(), asModel(documentedOption));
            }
        }
        return model;
    }

    protected Map<String, Object> asModel(DocumentedOption documentedOption) {
        return Map.of("description", documentedOption.description());
    }

    public String help(Configuration configuration, Format format) {
        if(format == Format.JSON) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildHelpModel(configuration));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        String template = "io/zenwave360/generator/help/" + format.toString();
        return handlebarsEngine.processTemplate(buildHelpModel(configuration), new TemplateInput().withTemplateLocation(template).withTargetFile("")).get(0).getContent();
    }

}
