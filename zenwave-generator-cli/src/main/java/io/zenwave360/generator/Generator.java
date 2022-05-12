package io.zenwave360.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.parsers.Parser;
import io.zenwave360.generator.plugins.GeneratorPlugin;
import io.zenwave360.generator.processors.Processor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.TemplateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Configuration configuration;

    public Generator(Configuration configuration) {
        this.configuration = configuration;
    }

    public void generate() throws Exception {
        log.debug("Executing generate with config options {}", configuration.getOptions());
        log.debug("Processors chain is {}", configuration.getChain().stream().map(c -> c.getName()).collect(Collectors.toList()));
        File file = new File(getClass().getClassLoader().getResource(configuration.getSpecFile()).toURI());
        Map<String, Object> model = null;
        List<TemplateOutput> templateOutputList = new ArrayList<>();

        for (Class processorClass: configuration.getChain()) {
            log.debug("Executing chained processor {}", processorClass);
            Object processor = processorClass.getDeclaredConstructor().newInstance();
            applyConfiguration(processor, configuration);

            if(processor instanceof Parser) {
                model = ((Parser) processor).parse(file);
            }
            if(processor instanceof Processor) {
                model = ((Processor) processor).process(model);
            }
            if(processor instanceof GeneratorPlugin) {
                templateOutputList.addAll(((GeneratorPlugin) processor).generate(model));
            }
            if(processor instanceof TemplateWriter) {
                ((TemplateWriter) processor).write(templateOutputList);
            }
        }
    }

    public void applyConfiguration(Object processor, Configuration configuration) throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.updateValue(processor, configuration.getOptions());
    }
}
