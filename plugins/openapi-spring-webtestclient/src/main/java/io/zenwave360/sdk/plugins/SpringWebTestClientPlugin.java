package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.processors.OpenApiProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.zenwave360.sdk.plugins.SpringWebTestClientGenerator.GroupByType.businessFlow;
import static io.zenwave360.sdk.plugins.SpringWebTestClientGenerator.GroupByType.partial;

@DocumentedPlugin(value = "Generates test for SpringMVC or Spring WebFlux using WebTestClient based on OpenAPI specification.", shortCode = "spring-webtestclient")
public class SpringWebTestClientPlugin extends Plugin {

    private Logger log = LoggerFactory.getLogger(getClass());
    public SpringWebTestClientPlugin() {
        super();
        withChain(DefaultYamlParser.class, OpenApiProcessor.class, SpringWebTestClientGenerator.class, /* JavaFormatter.class, */ TemplateFileWriter.class);
    }

    @Override
    public <T extends Plugin> T processOptions() {
        if (hasOption("groupBy", partial)) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        if(hasOption("groupBy", businessFlow) && !hasOption("businessFlowTestName")) {
            log.info("Business flow test name option 'businessFlowTestName' not provided. Printing to stdout.");
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
            withOption("businessFlowTestName", "BusinessFlowTest");
        }
        withOption("DefaultYamlParser.apiFile", StringUtils.firstNonBlank((String) getOptions().get("openapiFile"), this.getSpecFile()));
        return (T) this;
    }
}
