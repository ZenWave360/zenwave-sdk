package io.zenwave360.sdk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import io.zenwave360.sdk.plugins.ConfigurationProvider;
import io.zenwave360.sdk.utils.CommaSeparatedCollectionDeserializationHandler;
import io.zenwave360.sdk.zdl.layout.ProjectLayout;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.sdk.formatters.Formatter;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.writers.TemplateWriter;

public class MainGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public void generate(Plugin configuration) throws Exception {
        log.debug("Executing 'generate' with config Options {}", configuration.getOptions());
        log.debug("Processed Options {}", configuration.processOptions());
        log.debug("Processors chain is {}", configuration.getChain().stream().map(c -> c.getName()).collect(Collectors.toList()));
        Map<String, Object> model = new HashMap<>();
        List<TemplateOutput> templateOutputList = new ArrayList<>();

//        ConfigurationProvider.processLayout(configuration);

        int chainIndex = 0;
        for (Class pluginClass : configuration.getChain()) {
            log.debug("Executing chained pluginClass {}", pluginClass);
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            applyConfiguration(chainIndex++, plugin, configuration);

            if (plugin instanceof Parser) {
                Map parsed = ((Parser) plugin).withProjectClassLoader(configuration.getProjectClassLoader()).parse();
                model.putAll(parsed);
            }
            if (plugin instanceof ConfigurationProvider) {
                ((ConfigurationProvider) plugin).updateConfiguration(configuration, model);
            }
            if (plugin instanceof Processor) {
                model = ((Processor) plugin).process(model);
            }
            if (plugin instanceof Generator) {
                templateOutputList.addAll(((Generator) plugin).generate(model));
            }
            if (plugin instanceof Formatter) {
                templateOutputList = ((Formatter) plugin).format(templateOutputList);
            }
            if (plugin instanceof TemplateWriter) {
                ((TemplateWriter) plugin).write(templateOutputList);
            }
        }
    }

    public static void applyConfiguration(int chainIndex, Object plugin, Plugin configuration) throws Exception {
        Map<String, Object> options = configuration.getOptions();
        Object processorFullClassOptions = options.get(plugin.getClass().getName());
        Object processorSimpleClassOptions = options.get(plugin.getClass().getSimpleName());
        Object chainIndexOptions = options.get(String.valueOf(chainIndex));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addHandler(new CommaSeparatedCollectionDeserializationHandler());

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

        try {
            plugin.getClass().getMethod("onPropertiesSet").invoke(plugin);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore
        }
    }

}
