package io.zenwave360.generator.plugins;

import io.zenwave360.generator.Plugin;
import io.zenwave360.generator.doc.DocumentedPlugin;
import io.zenwave360.generator.formatters.JavaFormatter;
import io.zenwave360.generator.parsers.DefaultYamlParser;
import io.zenwave360.generator.processors.OpenApiProcessor;
import io.zenwave360.generator.writers.TemplateFileWriter;
import io.zenwave360.generator.writers.TemplateStdoutWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.zenwave360.generator.plugins.SpringWebTestClientGenerator.GroupByType.businessFlow;

@DocumentedPlugin(value = "Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification.", shortCode = "spring-webtestclient")
public class SpringWebTestClientPlugin extends Plugin {

    private Logger log = LoggerFactory.getLogger(getClass());
    public SpringWebTestClientPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, SpringWebTestClientGenerator.class, /* JavaFormatter.class, */ TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (!hasOption("targetFolder")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
            withOption(SpringWebTestClientGenerator.class.getName() + ".groupBy", SpringWebTestClientGenerator.GroupByType.partial.toString());
        }
        if(hasOption("groupBy", businessFlow) && !hasOption("businessFlowTestName")) {
            log.info("Business flow test name option 'businessFlowTestName' not provided. Printing to stdout.");
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
            withOption("businessFlowTestName", "BusinessFlowTest");
        }
        return (T) this;
    }
}
