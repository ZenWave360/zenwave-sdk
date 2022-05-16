package io.zenwave360.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.parsers.Model;
import io.zenwave360.generator.parsers.Parser;
import io.zenwave360.generator.plugins.GeneratorPlugin;
import io.zenwave360.generator.processors.Processor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.TemplateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, ?> model = new HashMap<>();
        List<TemplateOutput> templateOutputList = new ArrayList<>();

        for (Class pluginClass: configuration.getChain()) {
            log.debug("Executing chained pluginClass {}", pluginClass);
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            applyConfiguration(plugin, configuration);

            if(plugin instanceof Parser) {
                model = ((Parser) plugin).parse();
            }
            if(plugin instanceof Processor) {
                model = ((Processor) plugin).process(model);
            }
            if(plugin instanceof GeneratorPlugin) {
                templateOutputList.addAll(((GeneratorPlugin) plugin).generate(model));
            }
            if(plugin instanceof TemplateWriter) {
                ((TemplateWriter) plugin).write(templateOutputList);
            }
        }
    }

    public File loadSpecFile(String specFile) throws URISyntaxException {
        if(specFile.startsWith("classpath:")) {
            return new File(getClass().getClassLoader().getResource(specFile).toURI());
        } else {
            return new File(specFile);
        }
    }

    public void applyConfiguration(Object processor, Configuration configuration) throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.updateValue(processor, configuration.getOptions());
    }

}
