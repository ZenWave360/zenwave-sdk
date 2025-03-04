package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.zdl.layout.ProjectLayout;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public interface ConfigurationProvider {

    static Logger log = LoggerFactory.getLogger(ConfigurationProvider.class);

    void updateConfiguration(Plugin configuration, Map<String, Object> model);

    /**
     * Layout option in plugins is an special property that will hold an instance of ProjectLayout configured with any overrides that come from ZDL models.
     * This method will process the layout option and replace the string value with the actual instance of ProjectLayout.
     *
     * @param configuration
     */
    static void processLayout(Plugin configuration) throws Exception {
        var optionsLayout = getLayout(configuration.getOptions());
        var pluginLayout = getLayout(configuration);
        var layout = optionsLayout != null ? optionsLayout : pluginLayout;
        if (layout != null) {
            processLayoutPlaceHolders(layout, configuration.getOptions());
            configuration.getOptions().put("layout", layout);
        }
    }

    static ProjectLayout getLayout(Plugin configuration) throws Exception {
        for (Class pluginClass : configuration.getChain()) {
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            try {
                Field layoutField = plugin.getClass().getField("layout");
                if (layoutField != null) {
                    return (ProjectLayout) layoutField.get(plugin);
                }
            } catch (NoSuchFieldException e) {
                // ignore
            }
        }
        return null;
    }

    static ProjectLayout getLayout(Map<String, Object> options) {
        if (options.containsKey("layout")) {
            var layoutName = options.get("layout");
            if (layoutName instanceof String) {
                ProjectLayout layout = null;
                Class layoutClass = null;
                try {
                    layoutClass = ClassUtils.getClass((String) layoutName);
                } catch (ClassNotFoundException e) {
                    try {
                        layoutClass = ClassUtils.getClass(ProjectLayout.class.getPackageName() + "." + layoutName);
                    } catch (ClassNotFoundException ex) {
                        // ignore
                    }
                }
                if (ProjectLayout.class.isAssignableFrom(layoutClass)) {
                    try {
                        layout = (ProjectLayout) layoutClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return layout;
            } else if (layoutName instanceof ProjectLayout) {
                return (ProjectLayout) layoutName;
            }
        }
        return null;
    }

    static void processLayoutPlaceHolders(ProjectLayout layout, Map<String, Object> options) {
        HandlebarsEngine engine = new HandlebarsEngine();
        try {
            var layoutAsMap = layout.asMap();
            layoutAsMap.putAll(filterLayoutOptions(options));
            var model = new HashMap<String, Object>();
            model.putAll(layoutAsMap);
            model.putAll(options);
            for (int i = 0; i < 3; i++) { // 3 iterations should be enough to resolve all placeholders
                for (Map.Entry<String, Object> entry : layoutAsMap.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        String processedValue = engine.processInline(value, model);
                        layoutAsMap.put(entry.getKey(), processedValue);
                    }
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.updateValue(layout, layoutAsMap);

        } catch (IOException e) {
            throw new RuntimeException("Error processing layout placeholders", e);
        }
    }

    /* Filters layout options like "layout.outboundEventsPackage" and removes prefix */
    static Map<String, Object> filterLayoutOptions(Map<String, Object> options) {
        if (options == null) {
            return new HashMap<>();
        }
        Map<String, Object> layoutOptions = new HashMap<>();
        String prefix = "layout.";
        options.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                String newKey = key.substring(prefix.length());
                layoutOptions.put(newKey, value);
            }
        });
        return layoutOptions;
    }
}
