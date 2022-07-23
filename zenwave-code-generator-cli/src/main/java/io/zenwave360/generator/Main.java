package io.zenwave360.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zenwave360.generator.formatters.Formatter;
import io.zenwave360.generator.generators.Generator;
import io.zenwave360.generator.parsers.Parser;
import io.zenwave360.generator.processors.Processor;
import io.zenwave360.generator.templating.TemplateOutput;
import io.zenwave360.generator.writers.TemplateWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author ivangsa
 */
public class Main implements Callable<Integer> {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @Option(names = {"-f", "--help-format"}, arity = "0..1", description = "Help output format", defaultValue = "help")
    Help.Format helpFormat = Help.Format.help;

    @Option(names = {"-p", "--plugin"}, arity = "0..1", description = "Plugin Configuration class")
    String pluginConfigClass;

    @Option(names = {"-c", "--chain"}, split = ",", description = "deprecated use --plugin instead")
    Class[] chain;

    @CommandLine.Parameters
    Map<String, Object> options = new HashMap<>();

    public static void main(String... args) {
        var main = new Main();
        CommandLine cmd = new CommandLine(main);
        CommandLine.ParseResult parsed  = cmd.parseArgs(args);

        if(parsed.hasMatchedOption("h") && parsed.hasMatchedOption("p")) {
            try {
                main.help();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        int returnCode = cmd.execute(args);
        if(returnCode != 0) {
            System.exit(returnCode);
        }
    }

    @Override
    public Integer call() throws Exception {
        Configuration configuration = Configuration.of(this.pluginConfigClass)
                .withSpecFile((String) options.get("specFile"))
                .withTargetFolder((String) options.get("targetFolder"))
                .withOptions(options)
                .withChain(chain);
        generate(configuration);
        return 0;
    }

    public void help() throws Exception {
        Configuration configuration = Configuration.of(this.pluginConfigClass)
                .withSpecFile((String) options.get("specFile"))
                .withTargetFolder((String) options.get("targetFolder"))
                .withOptions(options)
                .withChain(chain);
        String help = new Help().help(configuration, helpFormat);
        System.out.println(help);
    }

    public void generate(Configuration configuration) throws Exception {
        log.debug("Executing 'generate' with config Options {}", configuration.getOptions());
        log.debug("Processed Options {}", configuration.processOptions());
        log.debug("Processors chain is {}", configuration.getChain().stream().map(c -> c.getName()).collect(Collectors.toList()));
        Map<String, Object> model = new HashMap<>();
        List<TemplateOutput> templateOutputList = new ArrayList<>();

        int chainIndex = 0;
        for (Class pluginClass: configuration.getChain()) {
            log.debug("Executing chained pluginClass {}", pluginClass);
            Object plugin = pluginClass.getDeclaredConstructor().newInstance();
            applyConfiguration(chainIndex++, plugin, configuration);

            if(plugin instanceof Parser) {
                Map parsed = ((Parser) plugin).parse();
                model.putAll(parsed);
            }
            if(plugin instanceof Processor) {
                model = ((Processor) plugin).process(model);
            }
            if(plugin instanceof Generator) {
                templateOutputList.addAll(((Generator) plugin).generate(model));
            }
            if(plugin instanceof Formatter) {
                templateOutputList = ((Formatter) plugin).format(templateOutputList);
            }
            if(plugin instanceof TemplateWriter) {
                ((TemplateWriter) plugin).write(templateOutputList);
            }
        }
    }
    public static void applyConfiguration(int chainIndex, Object processor, Configuration configuration) throws JsonMappingException {
        Map<String, Object> options = configuration.getOptions();
        Object processorFullClassOptions = options.get(processor.getClass().getName());
        Object processorSimpleClassOptions = options.get(processor.getClass().getSimpleName());
        Object chainIndexOptions = options.get(String.valueOf(chainIndex));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
