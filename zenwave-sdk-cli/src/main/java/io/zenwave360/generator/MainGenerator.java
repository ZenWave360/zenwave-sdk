package io.zenwave360.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import io.zenwave360.generator.utils.CommaSeparatedCollectionDeserializationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenwave360.generator.formatters.Formatter;
import io.zenwave360.generator.generators.Generator;
import io.zenwave360.generator.parsers.Parser;
import io.zenwave360.generator.processors.Processor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.TemplateWriter;

public class MainGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public void generate(Plugin configuration) throws Exception {
        log.debug("Executing 'generate' with config Options {}", configuration.getOptions());
        log.debug("Processed Options {}", configuration.processOptions());
        log.debug("Processors chain is {}", configuration.getChain().stream().map(c -> c.getName()).collect(Collectors.toList()));
        Map<String, Object> model = new HashMap<>();
        List<TemplateOutput> templateOutputList = new ArrayList<>();

        int chainIndex = 0;
        for (Class pluginClass : configuration.getChain()) {
            log.debug("Executing chained pluginClass {}", pluginClass);
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            applyConfiguration(chainIndex++, plugin, configuration);

            if (plugin instanceof Parser) {
                Map parsed = ((Parser) plugin).withProjectClassLoader(configuration.getProjectClassLoader()).parse();
                model.putAll(parsed);
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

    public static void applyConfiguration(int chainIndex, Object processor, Plugin plugin) throws JsonMappingException {
        Map<String, Object> options = plugin.getOptions();
        Object processorFullClassOptions = options.get(processor.getClass().getName());
        Object processorSimpleClassOptions = options.get(processor.getClass().getSimpleName());
        Object chainIndexOptions = options.get(String.valueOf(chainIndex));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addHandler(new CommaSeparatedCollectionDeserializationHandler());

        mapper.updateValue(processor, options);
        if (processorSimpleClassOptions != null) {
            mapper.updateValue(processor, processorSimpleClassOptions);
        }
        if (processorFullClassOptions != null) {
            mapper.updateValue(processor, processorFullClassOptions);
        }
        if (chainIndexOptions != null) {
            mapper.updateValue(processor, chainIndexOptions);
        }
    }
}