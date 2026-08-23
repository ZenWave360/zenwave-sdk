package io.zenwave360.sdk;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.WithProjectClassLoader;
import io.zenwave360.sdk.plugins.ConfigurationProvider;
import io.zenwave360.sdk.processors.GeneratedFilesProcessor;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.templating.HandlebarsEngine;
import io.zenwave360.sdk.utils.CommaSeparatedCollectionDeserializationHandler;
import io.zenwave360.sdk.utils.ObjectInstantiatorDeserializationHandler;
import io.zenwave360.sdk.writers.TemplateWriter;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import io.zenwave360.sdk.zdl.ProjectTemplates;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public void generate(Plugin configuration) throws Exception {
        log.debug("Executing 'generate' with config Options {}", configuration.getOptions());
        log.debug("Processed Options {}", configuration.processOptions());
        List<Class> chain = configuration.getChain();
        if (chain == null || chain.isEmpty()) {
            throw new IllegalArgumentException("Plugin '" + configuration.getClass().getName() + "' does not define a processor chain.");
        }
        log.debug("Processors chain is {}", chain.stream().map(c -> c.getName()).collect(Collectors.toList()));
        Map<String, Object> model = new HashMap<>();
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();

        int chainIndex = 0;
        for (Class pluginClass : chain) {
            log.debug("Executing chained pluginClass {}", pluginClass);
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            applyConfiguration(chainIndex++, plugin, configuration);

            if (plugin instanceof WithProjectClassLoader) {
                ((WithProjectClassLoader) plugin).withProjectClassLoader(configuration.getProjectClassLoader());
            }
            if (plugin instanceof Parser) {
                model.putAll(((Parser) plugin).parse());
            }
            if (plugin instanceof ConfigurationProvider) {
                ((ConfigurationProvider) plugin).updateConfiguration(configuration, model);
            }
            if (plugin instanceof Processor) {
                model = ((Processor) plugin).process(model);
            }
            if (plugin instanceof Generator) {
                generatedProjectFiles.addAll(((Generator) plugin).generate(model));
            }
            if (plugin instanceof GeneratedFilesProcessor) {
                ((GeneratedFilesProcessor) plugin).process(generatedProjectFiles);
            }
            if (plugin instanceof TemplateWriter) {
                ((TemplateWriter) plugin).write(generatedProjectFiles.getAllTemplateOutputs());
            }
        }
    }

    public static void applyConfiguration(int chainIndex, Object plugin, Plugin configuration) throws Exception {
        Map<String, Object> options = configuration.getOptions();
        Object processorFullClassOptions = options.get(plugin.getClass().getName());
        Object processorSimpleClassOptions = options.get(plugin.getClass().getSimpleName());
        Object chainIndexOptions = options.get(String.valueOf(chainIndex));
        var layout = configuration.getProcessedLayout();

        mapper.updateValue(plugin, options);
        if (processorSimpleClassOptions != null) {
            mapper.updateValue(plugin, processorSimpleClassOptions);
        }
        if (processorFullClassOptions != null) {
            mapper.updateValue(plugin, processorFullClassOptions);
        }
        if (chainIndexOptions != null) {
            mapper.updateValue(plugin, chainIndexOptions);
        }
        if (layout != null && FieldUtils.getField(plugin.getClass(), "layout") != null) {
            FieldUtils.writeField(plugin, "layout", layout);
        }

        var templatesField = FieldUtils.getField(plugin.getClass(), "templates");
        if (templatesField != null) {
            var templates = templatesField.get(plugin);
            if(templates instanceof ProjectTemplates projectTemplates) {
                mapper.updateValue(templates, options);
                projectTemplates.setLayout(layout);
            }
        }

        if (plugin instanceof Generator generator) {
            generator.configuration = configuration;
        }

        try {
            plugin.getClass().getMethod("onPropertiesSet").invoke(plugin);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore
        }

        if (templatesField != null && plugin instanceof Generator generator) {
            var templates = templatesField.get(plugin);
            if (templates instanceof ProjectTemplates projectTemplates) {
                projectTemplates.getTemplateHelpers(generator)
                        .forEach(helper -> generator.getTemplateEngine().registerHelpers(helper));
            }
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.addHandler(new ObjectInstantiatorDeserializationHandler());
        mapper.addHandler(new CommaSeparatedCollectionDeserializationHandler());
    }

}
